package ru.IrinaTik.diploma.service;

import ru.IrinaTik.diploma.entity.Lemma;
import ru.IrinaTik.diploma.entity.Page;
import ru.IrinaTik.diploma.entity.SearchIndex;
import ru.IrinaTik.diploma.entity.Site;

import java.util.Collection;
import java.util.List;

public interface SearchIndexService {
    List<SearchIndex> getAll();

    SearchIndex getById(int id);

    List<SearchIndex> getByPage(Page page);

    SearchIndex save(SearchIndex index);

    List<SearchIndex> saveAll(Collection<SearchIndex> indexes);

    SearchIndex createNew(Page page, Lemma lemma, Float rank);

    SearchIndex createAndSave(Page page, Lemma lemma, Float rank);

    void deleteBySite(Site site);

    void deleteByPage(Page page);

    void deleteAll();
}
