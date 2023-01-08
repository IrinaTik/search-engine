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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class IndexingService {

    private static final String INDEXING_STOPPED_BY_USER_ERROR = "Индексация прервана пользователем";
    private static final String INDEXING_ALREADY_STARTED_ERROR = "Индексация уже запущена";
    private static final String INDEXING_NOT_STARTED_ERROR = "Индексация не запущена";
    private static final String PAGE_NOT_LISTED_ERROR = "Данная страница находится за пределами сайтов, указанных в конфигурационном файле";

    @Setter(AccessLevel.PRIVATE)
    @Getter
    private static volatile boolean isCancelledStopIndexing = false;

    @Setter
    @Getter
    private volatile String pageParsingError;

    private final PageService pageService;
    private final FieldService fieldService;
    private final SearchIndexService indexService;
    private final LemmaService lemmaService;
    private final SiteService siteService;

    private List<Future<IndexingResponse>> responses;

    @Getter
    private List<Field> fieldList;

    public IndexingResponse indexingAllSites() {
        if (!isIndexingDone()) {
            return new IndexingResponse(false, INDEXING_ALREADY_STARTED_ERROR);
        }
        setCancelledStopIndexing(false);
        List<Site> sites = siteService.getSitesFromConfig();
        // каждый сайт должен быть в своем потоке
        ExecutorService pool = Executors.newFixedThreadPool(sites.size());
        responses = new ArrayList<>();
        for (Site site : sites) {
            responses.add(pool.submit(() -> indexingOneSite(site.getUrl(), site.getName())));
        }
        pool.shutdown();
        return new IndexingResponse(true, "");
    }

    public IndexingResponse stopIndexing() {
        if (isIndexingDone()) {
            return new IndexingResponse(false, INDEXING_NOT_STARTED_ERROR);
        }
        setCancelledStopIndexing(true);
        SiteParser.stopParsing();
        return new IndexingResponse(true, "");
    }

    public IndexingResponse indexingAddedSite(String url, String name) {
        return null;
    }

    public IndexingResponse indexingOneSite(String url, String name) {
        initFieldsList();
        pageParsingError = "";
        Site site = siteService.getByUrlOrElseCreateAndSaveNew(url, name);
        if (isCancelledStopIndexing) {
            return setResultAndSiteStatus(site, SiteIndexingStatus.FAILED, INDEXING_STOPPED_BY_USER_ERROR);
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
                return setResultAndSiteStatus(site, SiteIndexingStatus.FAILED, INDEXING_STOPPED_BY_USER_ERROR);
            }
            if (getPageParsingError().isEmpty()) {
                createLemmasAndIndexesForSite(site, parser.getSiteMap());
                if (isCancelledStopIndexing) {
                    result = setResultAndSiteStatus(site, SiteIndexingStatus.FAILED, INDEXING_STOPPED_BY_USER_ERROR);
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
            result = setResultAndSiteStatus(site, SiteIndexingStatus.FAILED, ex.getMessage());
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
            result.setError("Ошибка индексации: сайт - " + site.getUrl() + "\n" + error);
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
            System.out.println("Deleting data for " + site.getUrl());
            siteService.deleteAllInfoRelatedToSite(site);
            System.out.println("Saving data for " + site.getUrl());
            pageService.saveAll(siteMap);
            lemmaService.saveAll(siteLemmas.values());
            indexService.saveAll(siteIndexes);
            System.out.println("Data saved for " + site.getUrl());
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

    private boolean isIndexingDone() {
        return responses == null || responses.stream().allMatch(Future::isDone);
    }

    private void initFieldsList() {
        fieldList = fieldService.getAll();
    }

}
