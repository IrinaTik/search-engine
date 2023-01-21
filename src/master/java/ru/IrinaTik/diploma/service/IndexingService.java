package ru.IrinaTik.diploma.service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.IrinaTik.diploma.config.AppConfig;
import ru.IrinaTik.diploma.entity.*;
import ru.IrinaTik.diploma.response.IndexingResponse;
import ru.IrinaTik.diploma.util.SiteParser;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

@Log4j2
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

    private ExecutorService executorThreadPool;

    @Getter
    private final AppConfig appConfig;

    @Getter
    private List<Field> fieldList;

    public IndexingResponse indexingAllSites() {
        if (!isIndexingDone()) {
            return new IndexingResponse(false, INDEXING_ALREADY_STARTED_ERROR);
        }
        setCancelledStopIndexing(false);
        List<Site> sites = appConfig.getSiteList();
        SiteParser.initSiteMap(sites);
        // каждый сайт должен быть в своем потоке
        executorThreadPool = Executors.newFixedThreadPool(sites.size());
        for (Site site : sites) {
            executorThreadPool.submit(() -> indexingOneSite(site.getUrl(), site.getName()));
        }
        executorThreadPool.shutdown();
        return new IndexingResponse(true, "");
    }

    public IndexingResponse stopIndexing() {
        if (isIndexingDone()) {
            return new IndexingResponse(false, INDEXING_NOT_STARTED_ERROR);
        }
        executorThreadPool.shutdownNow();
        setCancelledStopIndexing(true);
        SiteParser.stopParsing();
        log.warn("Indexing was CANCELLED by user");
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

    @Transactional
    private IndexingResponse gatherSiteIndexingInfo(Site site) {
        IndexingResponse result;
        try {
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            SiteParser parser = new SiteParser(new Page(site.getUrl()), site, this);
            forkJoinPool.invoke(parser);
            if (isCancelledStopIndexing) {
                forkJoinPool.shutdownNow();
                return setResultAndSiteStatus(site, SiteIndexingStatus.FAILED, INDEXING_STOPPED_BY_USER_ERROR);
            }
            Set<Page> sitePages = SiteParser.getSitePages(site);
            if (!sitePages.isEmpty() && sitePages.stream().anyMatch(Page::isPageResponseOK)) {
                createLemmasAndIndexesForSite(site, sitePages);
                if (isCancelledStopIndexing) {
                    result = setResultAndSiteStatus(site, SiteIndexingStatus.FAILED, INDEXING_STOPPED_BY_USER_ERROR);
                } else {
                    result = setResultAndSiteStatus(site, SiteIndexingStatus.INDEXED, getPageParsingError());
                }
            } else {
                result = setResultAndSiteStatus(site, SiteIndexingStatus.FAILED, getPageParsingError());
            }
            sitePages.stream().map(page -> "№" + page.getId() + " - код " + page.getCode() + ": " + page.getRelPath()).forEach(System.out::println);
            System.out.println(sitePages.size());
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
            if (!page.isPageResponseOK()) {
                continue;
            }
            log.info("Getting lemmas and indexes for page {}", page.getAbsPath());
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
            log.info("Deleting data for {}", site.getUrl());
            siteService.deleteAllInfoRelatedToSite(site);
            log.info("Saving data for {}", site.getUrl());
            pageService.saveAll(siteMap);
            lemmaService.saveAll(siteLemmas.values());
            indexService.saveAll(siteIndexes);
            log.info("Data saved for {} \n\t Pages -> {} \n\t Lemmas -> {} \n\t Indexes -> {}",
                    site.getUrl(), siteMap.size(), siteLemmas.values().size(), siteIndexes.size());
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
        return executorThreadPool == null || executorThreadPool.isTerminated();
    }

    private void initFieldsList() {
        fieldList = fieldService.getAll();
    }

}
