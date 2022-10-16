package ru.IrinaTik.diploma.service;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.IrinaTik.diploma.entity.Field;
import ru.IrinaTik.diploma.entity.Lemma;
import ru.IrinaTik.diploma.entity.Page;
import ru.IrinaTik.diploma.entity.SearchIndex;
import ru.IrinaTik.diploma.repository.PageRepository;
import ru.IrinaTik.diploma.response.SearchResponse;
import ru.IrinaTik.diploma.util.SiteParser;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PageService {

    private static final String ROOT_SITE = "http://www.playback.ru/";

    private final PageRepository pageRepository;
    private final FieldService fieldService;
    private final LemmaService lemmaService;
    private final SearchIndexService indexService;

    private List<Field> fieldList;

    public List<Page> getAll() {
        return pageRepository.findAll();
    }

    public Page getById(int id) {
        return pageRepository.findById(id).orElse(null);
    }

    public Page save(Page page) {
        return pageRepository.saveAndFlush(page);
    }

    public void getSiteMap() {
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(new SiteParser(new Page(ROOT_SITE)));
        SiteParser.siteMap.stream().forEach(this::save);
        SiteParser.siteMap.stream().map(page -> "№" + page.getId() + " - код " + page.getCode() + ": " + page.getRelPath()).forEach(System.out::println);
        System.out.println("Всего: " + SiteParser.siteMap.size());
        System.out.println("Код 200: " + SiteParser.siteMap.stream().filter(Page::isPageResponseOK).count());
        fieldList = fieldService.getAll();
        SiteParser.siteMap.stream().filter(Page::isPageResponseOK).forEach(this::getLemmasFromPage);
    }

    public List<SearchResponse> getSearchResult(String searchText) {
        List<Lemma> lemmas = lemmaService.getLemmasFromTextSortedByFrequencyPresentInRep(searchText);
        if (lemmas.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Page, Float> pages = getPagesRelevantToSearchWithAbsRelevance(lemmas);
        if (pages.isEmpty()) {
            return Collections.emptyList();
        }
        Float maxAbsRelevance = findMaxPageRelevance(pages);
        pages = getPagesWithRelativeRelevance(pages, maxAbsRelevance);
        return getSearchResponseFromRelevantPages(pages, lemmas);
    }

    private List<SearchResponse> getSearchResponseFromRelevantPages(Map<Page, Float> pages, List<Lemma> lemmas) {
        return pages.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .map(entry -> createSearchResponse(entry.getKey(), entry.getValue(), lemmas))
                .collect(Collectors.toList());
    }

    private SearchResponse createSearchResponse(Page page, Float relevance, List<Lemma> lemmas) {
        SearchResponse response = new SearchResponse();
        response.setUri(page.getRelPath());
        Document pageContent = Jsoup.parse(page.getContent());
        response.setTitle(pageContent.title());
        response.setSnippet(lemmaService.createSnippet(pageContent.text(), lemmas));
        response.setRelevance(relevance);
        return response;
    }

    private Map<Page, Float> getPagesRelevantToSearchWithAbsRelevance(List<Lemma> lemmas) {
        // цикл поиска страниц с леммами, начиная от самой редкой
        Map<Page, Float> relevantPages = lemmas.get(0).getIndexSet().stream()
                .collect(Collectors.toMap(SearchIndex::getPage, SearchIndex::getRank));
        int i = 1;
        while (i < lemmas.size() && !relevantPages.isEmpty()) {
            Lemma lemma = lemmas.get(i);
            relevantPages = searchForPagesWithAllLemmasRanks(lemma, relevantPages);
            i++;
        }
        return relevantPages;
    }

    private Map<Page, Float> searchForPagesWithAllLemmasRanks(Lemma lemma, Map<Page, Float> pagesWithAllLemmasRanks) {
        Set<SearchIndex> indexSet = lemma.getIndexSet();
        Map<Integer, Float> lemmasRanksOnPage = indexSet.stream().collect(Collectors.toMap(index -> index.getPage().getId(), SearchIndex::getRank));
        return pagesWithAllLemmasRanks.keySet().stream()
                .filter(page -> lemmasRanksOnPage.containsKey(page.getId()))
                .collect(Collectors.toMap(page -> page, page -> Float.sum(lemmasRanksOnPage.get(page.getId()), pagesWithAllLemmasRanks.get(page))));
    }

    private Float findMaxPageRelevance(Map<Page, Float> pages) {
        return pages.values().stream().max(Float::compare).get();
    }

    private Map<Page, Float> getPagesWithRelativeRelevance(Map<Page, Float> pages, Float maxAbsRelevance) {
        return pages.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue() / maxAbsRelevance));
    }

    private void getLemmasFromPage(Page page) {
        // работа с одной страницей
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
        saveLemmaAndIndex(uniquePageLemmasWithRank, page);
    }

    private void saveLemmaAndIndex(Map<String, Float> uniquePageLemmasWithRank, Page page) {
        for (Map.Entry<String, Float> entry : uniquePageLemmasWithRank.entrySet()) {
            Lemma lemma = lemmaService.getByLemma(entry.getKey());
            if (lemma == null) {
                lemma = lemmaService.createAndSave(entry.getKey());
            } else {
                lemma.setFrequency(lemma.getFrequency() + 1);
                lemmaService.save(lemma);
            }
            indexService.createAndSave(page, lemma, entry.getValue());
        }
    }

}