package ru.IrinaTik.diploma.service;

import ru.IrinaTik.diploma.entity.Page;
import ru.IrinaTik.diploma.entity.Site;
import ru.IrinaTik.diploma.response.SearchResponse;

import java.util.Collection;
import java.util.List;

public interface PageService {
    List<Page> getAll();

    Page getById(int id);

    Page getByRelPathAndSite(Site site, String path);

    Page save(Page page);

    List<Page> saveAll(Collection<Page> pages);

    List<Page> getBySite(Site site);

    void deleteBySite(Site site);

    void deleteAll();

    int getLemmaFrequencyLimit();

    List<SearchResponse> getSearchResult(String searchText, Site site);

    String createRelPathFromAbsPath(String absPath, String siteUrl);
}
