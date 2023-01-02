package ru.IrinaTik.diploma.service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.IrinaTik.diploma.entity.*;
import ru.IrinaTik.diploma.response.IndexingResponse;
import ru.IrinaTik.diploma.util.SiteParser;

import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class IndexingService {

    private static final String INDEXING_STOPPED_ERROR = "Индексация прервана пользователем";
    private static final String INDEXING_ALREADY_STARTED_ERROR = "Индексация уже запущена";
    private static final String INDEXING_NOT_STARTED_ERROR = "Индексация не запущена";
    private static final String PAGE_NOT_LISTED_ERROR = "Данная страница находится за пределами сайтов, указанных в конфигурационном файле";

    @Setter(AccessLevel.PRIVATE)
    @Getter
    private volatile boolean isCancelledStopIndexing = false;

    @Setter(AccessLevel.PRIVATE)
    @Getter
    private volatile boolean isRunning = false;

    // TODO: isRunning для вывода сообщения индексация идет \ остановлена \ окончена

    @Setter
    @Getter
    private volatile String pageParsingError;

    private final PageService pageService;
    private final FieldService fieldService;
    private final SearchIndexService indexService;
    private final LemmaService lemmaService;
    private final SiteService siteService;

    @Getter
    private List<Field> fieldList;

    public IndexingResponse indexingAllSites() {
        // TODO: индексация всего. НЗ! Каждый сайт в своем потоке
        if (isRunning) {
            return new IndexingResponse(false, INDEXING_ALREADY_STARTED_ERROR);
        }
        setRunning(true);
        setCancelledStopIndexing(false);
        List<Site> sites = siteService.getSitesFromConfig();
        // Сейчас тут заглушка для одного сайта из списка. TODO: for каждый сайт indexingOneSite.
        IndexingResponse result = indexingOneSite(sites.get(2).getUrl(), sites.get(2).getName());
        setRunning(false);
        return result;
    }

    public IndexingResponse stopIndexing() {
        if (isRunning) {
            setCancelledStopIndexing(true);
            setRunning(false);
            SiteParser.stopParsing();
            return new IndexingResponse(true, "");
        }
        return new IndexingResponse(false, INDEXING_NOT_STARTED_ERROR);
    }

    private void initFieldsList() {
        fieldList = fieldService.getAll();
    }

    public IndexingResponse indexingOneSite(String url, String name) {
        initFieldsList();
        pageParsingError = "";
        Site site = siteService.getByUrlOrElseCreateAndSaveNew(url, name);
        if (isCancelledStopIndexing) {
            return setResultAndSiteStatus(site, SiteIndexingStatus.FAILED, INDEXING_STOPPED_ERROR);
        }
        return gatherSiteIndexingInfo(site);
    }

    private IndexingResponse gatherSiteIndexingInfo(Site site) {
        IndexingResponse result;
        try {
            ForkJoinPool pool = new ForkJoinPool();
            SiteParser parser = new SiteParser(new Page(site.getUrl()), site, pageParsingError);
            pool.invoke(parser);
            if (isCancelledStopIndexing) {
                return setResultAndSiteStatus(site, SiteIndexingStatus.FAILED, INDEXING_STOPPED_ERROR);
            }
            if (getPageParsingError().isEmpty()) {
                createLemmasAndIndexesForSite(site, parser.getSiteMap());
                if (isCancelledStopIndexing) {
                    result = setResultAndSiteStatus(site, SiteIndexingStatus.FAILED, INDEXING_STOPPED_ERROR);
                } else {
                    result = setResultAndSiteStatus(site, SiteIndexingStatus.INDEXED, getPageParsingError());
                }
            } else {
                result = setResultAndSiteStatus(site, SiteIndexingStatus.FAILED, getPageParsingError());
            }
            parser.getSiteMap().stream().map(page -> "№" + page.getId() + " - код " + page.getCode() + ": " + page.getRelPath()).forEach(System.out::println);
            System.out.println(parser.getSiteMap().size());
            System.out.println(result);
        } catch (Exception ex) {
            ex.printStackTrace();
            result = setResultAndSiteStatus(site, SiteIndexingStatus.FAILED, "Ошибка индексации: сайт - " + site.getUrl() + System.lineSeparator() + ex.getMessage());
        }
        return result;
    }

    private IndexingResponse setResultAndSiteStatus(Site site, SiteIndexingStatus status, String error) {
        IndexingResponse result = new IndexingResponse();
        siteService.setSiteStatusAndSave(site, status, error);
        if (error == null || error.isEmpty()) {
            result.setResult(true);
        } else {
            result.setResult(false);
            result.setError(error);
        }
        return result;
    }

    private void createLemmasAndIndexesForSite(Site site, Set<Page> siteMap) {
        Map<String, Lemma> siteLemmas = new HashMap<>();
        List<SearchIndex> siteIndexes = new ArrayList<>();
        for (Page page : siteMap) {
            System.out.println("Getting lemmas and indexes for page " + page.getAbsPath());
            Map<String, Float> pageLemmasWithRank = getLemmasFromPage(page);
            for (Map.Entry<String, Float> entry : pageLemmasWithRank.entrySet()) {
                if (isCancelledStopIndexing) {
                    return;
                }
                String strLemma = entry.getKey();
                Lemma lemma;
                if (siteLemmas.containsKey(strLemma)) {
                    lemma = siteLemmas.get(strLemma);
                    lemma.setFrequency(lemma.getFrequency() + 1);
                } else {
                    lemma = lemmaService.createNew(entry.getKey(), page.getSite());
                    siteLemmas.put(lemma.getLemma(), lemma);
                }
                siteIndexes.add(indexService.createNew(page, lemma, entry.getValue()));
            }
        }
        if (!isCancelledStopIndexing) {
            siteService.deleteAllInfoRelatedToSite(site);
            pageService.saveAll(siteMap);
            lemmaService.saveAll(siteLemmas.values());
            indexService.saveAll(siteIndexes);
        }
    }

    public Map<String, Float> getLemmasFromPage(Page page) {
        Map<String, Float> uniquePageLemmasWithRank = new HashMap<>();
        Document doc = Jsoup.parse(page.getContent());
        for (Field field : fieldList) {
            Elements elements = doc.select(field.getSelector());
            String text = elements.text();
            Map<String, Integer> fieldLemmas = lemmaService.getStrLemmasFromTextWithCount(text);
            fieldLemmas.forEach((fieldLemma, count) -> {
                if (uniquePageLemmasWithRank.containsKey(fieldLemma)) {
                    uniquePageLemmasWithRank.put(fieldLemma, uniquePageLemmasWithRank.get(fieldLemma) + field.getWeight() * count);
                } else {
                    uniquePageLemmasWithRank.put(fieldLemma, field.getWeight() * count);
                }
            });
        }
        return uniquePageLemmasWithRank;
    }

}
