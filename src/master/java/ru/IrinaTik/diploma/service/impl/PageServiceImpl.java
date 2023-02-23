package ru.IrinaTik.diploma.service.impl;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.IrinaTik.diploma.entity.Lemma;
import ru.IrinaTik.diploma.entity.Page;
import ru.IrinaTik.diploma.entity.SearchIndex;
import ru.IrinaTik.diploma.entity.Site;
import ru.IrinaTik.diploma.repository.PageRepository;
import ru.IrinaTik.diploma.response.SearchResponse;
import ru.IrinaTik.diploma.service.LemmaService;
import ru.IrinaTik.diploma.service.PageService;
import ru.IrinaTik.diploma.service.SearchIndexService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PageServiceImpl implements PageService {

    private static final Double LEMMA_FREQUENCY_PERCENT = 0.9;

    private final PageRepository pageRepository;
    private final LemmaService lemmaService;

    @Override
    public List<Page> getAll() {
        return pageRepository.findAll();
    }

    @Override
    public Page getById(int id) {
        return pageRepository.findById(id).orElse(null);
    }

    @Override
    public Page getByRelPathAndSite(Site site, String path) {
        return pageRepository.getByRelPathAndSite(site, path);
    }

    @Override
    public Page save(Page page) {
        return pageRepository.saveAndFlush(page);
    }

    @Override
    public List<Page> saveAll(Collection<Page> pages) {
        return pageRepository.saveAllAndFlush(pages);
    }

    @Override
    public List<Page> getBySite(Site site) {
        List<Page> sitePages = pageRepository.getBySite(site);
        if (sitePages == null) {
            return new ArrayList<>();
        }
        return sitePages;
    }

    @Override
    public void deleteBySite(Site site) {
        pageRepository.deleteBySite(site);
    }

    @Override
    public void deleteAll() {
        pageRepository.deleteAll();
    }

    @Override
    public int getLemmaFrequencyLimit() {
        long responsivePagesCount = getAll().stream().filter(Page::isPageResponseOK).count();
        return (int) (responsivePagesCount * LEMMA_FREQUENCY_PERCENT);
    }

    @Override
    public List<SearchResponse> getSearchResult(String searchText, Site site) {
        List<Lemma> lemmas = lemmaService.getLemmasFromTextSortedByFrequencyPresentInRep(searchText, getLemmaFrequencyLimit(), site);
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

    @Override
    public String createRelPathFromAbsPath(String absPath, String siteUrl) {
        return absPath.replace(siteUrl, "");
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

}