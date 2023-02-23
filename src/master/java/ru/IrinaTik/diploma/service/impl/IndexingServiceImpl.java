package ru.IrinaTik.diploma.service.impl;

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
import ru.IrinaTik.diploma.service.*;
import ru.IrinaTik.diploma.util.SiteParser;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class IndexingServiceImpl implements IndexingService {

    private static final String INDEXING_STOPPED_BY_USER_ERROR = "Индексация прервана пользователем";
    private static final String INDEXING_ALREADY_STARTED_ERROR = "Индексация уже запущена";
    private static final String INDEXING_NOT_STARTED_ERROR = "Индексация не запущена";
    private static final String PAGE_NOT_LISTED_ERROR = "Данная страница находится за пределами сайтов, указанных в конфигурационном файле";

    @Setter(AccessLevel.PRIVATE)
    @Getter
    private static volatile boolean isCancelledStopIndexing = false;

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

    @Override
    public IndexingResponse indexingAllSites() {
        if (!isIndexingDone()) {
            return new IndexingResponse(false, INDEXING_ALREADY_STARTED_ERROR);
        }
        log.info("Indexing was initiated by user");
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

    @Override
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


    @Override
    @Transactional
    public IndexingResponse indexingAddedPage(String url) {
        // TODO: 18.02.2023 отладочное
        log.info("Indexing page {} was initiated by user", url);
        if (!isIndexingDone()) {
            log.warn(INDEXING_ALREADY_STARTED_ERROR);
            return new IndexingResponse(false, INDEXING_ALREADY_STARTED_ERROR);
        }
        setCancelledStopIndexing(false);
        List<Site> sites = appConfig.getSiteList();
        Site pageSite = sites.stream().filter(site -> url.contains(site.getUrl())).findAny().orElse(null);
        if (pageSite == null) {
            log.warn(PAGE_NOT_LISTED_ERROR + " -> {}", url);
            return new IndexingResponse(false, PAGE_NOT_LISTED_ERROR);
        }
        executorThreadPool = Executors.newFixedThreadPool(1);
        // TODO: 28.01.2023 удаление должно быть совсем другое - всю инфу по сайту не удалять. через флаг?
        executorThreadPool.submit(() -> indexingOnePage(url, pageSite));
        executorThreadPool.shutdown();
        return new IndexingResponse(true, "");
    }

    @Override
    @Transactional
    public IndexingResponse indexingOneSite(String url, String name) {
        initFieldsList();
        Site site = siteService.getByUrlOrElseCreateAndSaveNew(url, name);
        if (isCancelledStopIndexing) {
            return setResultAndSiteStatus(site, SiteIndexingStatus.FAILED, INDEXING_STOPPED_BY_USER_ERROR);
        }
        return gatherSiteIndexingInfo(site);
    }

    private IndexingResponse indexingOnePage(String pageURL, Site site) {
        initFieldsList();
        // TODO: 18.02.2023 в getByUrlOrElseCreateAndSaveNew устанавливается статус INDEXING у сайта. Подумать, что делать с этим
        site = siteService.getByUrlOrElseCreateAndSaveNew(site.getUrl(), site.getName());
        String pageRelPath = pageService.createRelPathFromAbsPath(pageURL, site.getUrl());
        Page page = pageService.getByRelPathAndSite(site, pageRelPath);
        if (page == null) {
            page = new Page(pageURL);
            page.setSite(site);
        } else {
            page.setAbsPath(pageURL);
        }
        return gatherPageIndexingInfo(site, page);
    }

    private IndexingResponse gatherPageIndexingInfo(Site site, Page page) {
        SiteParser parser = new SiteParser(page, site, this);
        parser.parse();
        pageService.save(page);
        if (!page.isPageResponseOK()) {
            log.warn("Indexing failed for page {} - response code {}", page.getAbsPath(), page.getCode());
            return new IndexingResponse(false, "Page response code - " + page.getCode());
        }
        log.info("Page absPath {}, relPath {}, code {}, of site {}", page.getAbsPath(), page.getRelPath(), page.getCode(), page.getSite().getUrl());
        createLemmasAndIndexesForPage(site, page);
        return new IndexingResponse(true, "");
    }

    private void createLemmasAndIndexesForPage(Site site, Page page) {
        // TODO: 18.02.2023
        List<SearchIndex> pageIndexes = indexService.getByPage(page);
        System.out.println(pageIndexes.size());

        System.out.println("------------------- Old lemmas -------------------");
        pageIndexes.stream().map(SearchIndex::getLemma)
                .forEach(lemma -> System.out.println(lemma.getId() + " -> " + lemma.getLemma() + " -> " + lemma.getFrequency()));
        if (!pageIndexes.isEmpty()) {
            List<Lemma> oldPageLemmas = decreaseLemmasFrequencyByOne(pageIndexes);
            lemmaService.saveAll(oldPageLemmas);
            System.out.println("------------------- Old lemmas with decreased frequency -------------------");
            for (Lemma lemma : oldPageLemmas) {
                Lemma newLemma = lemmaService.getByLemmaAndSite(lemma.getLemma(), lemma.getSite());
                System.out.println(newLemma.getId() + " -> " + newLemma.getLemma() + " -> " + newLemma.getFrequency());
            }
            indexService.deleteByPage(page);
            pageIndexes.clear();
            List<SearchIndex> deletedIndexes = indexService.getByPage(page);
            System.out.println(deletedIndexes.size());
        }
        System.out.println("Getting site lemmas");
        Map<String, Lemma> siteLemmas = getSiteLemmas(site);
        getLemmasAndIndexesForPage(page, siteLemmas, pageIndexes);
        lemmaService.saveAll(siteLemmas.values());
        List<Lemma> oldPageLemmas = pageIndexes.stream().map(SearchIndex::getLemma).collect(Collectors.toList());
        System.out.println("------------------- New lemmas -------------------");
        for (Lemma lemma : oldPageLemmas) {
            Lemma newLemma = lemmaService.getByLemmaAndSite(lemma.getLemma(), lemma.getSite());
            System.out.println(newLemma.getId() + " -> " + newLemma.getLemma() + " -> " + newLemma.getFrequency());
        }
        indexService.saveAll(pageIndexes);
    }

    private List<Lemma> decreaseLemmasFrequencyByOne(List<SearchIndex> pageIndexes) {
        List<Lemma> lemmas = pageIndexes.stream().map(SearchIndex::getLemma).collect(Collectors.toList());
        for (Lemma lemma : lemmas) {
            lemma.setFrequency(lemma.getFrequency() - 1);
        }
        return lemmas;
    }

    private Map<String, Lemma> getSiteLemmas(Site site) {
        List<Lemma> lemmas = lemmaService.getBySite(site);
        return lemmas.stream().collect(Collectors.toMap(Lemma::getLemma, lemma -> lemma));
    }


    private IndexingResponse gatherSiteIndexingInfo(Site site) {
        IndexingResponse result;
        try {
//            String pageParsingError = "";
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
                    result = setResultAndSiteStatus(site, SiteIndexingStatus.INDEXED, site.getLastError());
                }
            } else {
                result = setResultAndSiteStatus(site, SiteIndexingStatus.FAILED, site.getLastError());
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

    @Transactional
    public IndexingResponse setResultAndSiteStatus(Site site, SiteIndexingStatus status, String error) {
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
            getLemmasAndIndexesForPage(page, siteLemmas, siteIndexes);
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

    private void getLemmasAndIndexesForPage(Page page, Map<String, Lemma> lemmas, Collection<SearchIndex> indexes) {
        log.info("Getting lemmas and indexes for page {}", page.getAbsPath());
        Map<String, Float> pageLemmasWithRank = getLemmasFromPage(page);
        for (Map.Entry<String, Float> entry : pageLemmasWithRank.entrySet()) {
            if (isCancelledStopIndexing) {
                return;
            }
            String strLemma = entry.getKey();
            Lemma lemma;
            if (lemmas.containsKey(strLemma)) {
                lemma = lemmas.get(strLemma);
                lemma.setFrequency(lemma.getFrequency() + 1);
            } else {
                lemma = lemmaService.createNew(entry.getKey(), page.getSite());
                lemmas.put(lemma.getLemma(), lemma);
            }
            indexes.add(indexService.createNew(page, lemma, entry.getValue()));
        }
    }

    private Map<String, Float> getLemmasFromPage(Page page) {
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
