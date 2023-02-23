package ru.IrinaTik.diploma.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.IrinaTik.diploma.entity.Lemma;
import ru.IrinaTik.diploma.entity.Page;
import ru.IrinaTik.diploma.entity.SearchIndex;
import ru.IrinaTik.diploma.entity.Site;
import ru.IrinaTik.diploma.repository.SearchIndexRepository;
import ru.IrinaTik.diploma.service.SearchIndexService;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SearchIndexServiceImpl implements SearchIndexService {

    private final SearchIndexRepository indexRepository;

    @Override
    public List<SearchIndex> getAll() {
        return indexRepository.findAll();
    }

    @Override
    public SearchIndex getById(int id) {
        return indexRepository.findById(id).orElse(null);
    }

    @Override
    public List<SearchIndex> getByPage(Page page) {
        return indexRepository.getByPage(page);
    }

    @Override
    public SearchIndex save(SearchIndex index) {
        return indexRepository.saveAndFlush(index);
    }

    @Override
    public List<SearchIndex> saveAll(Collection<SearchIndex> indexes) {
        return indexRepository.saveAllAndFlush(indexes);
    }

    @Override
    public SearchIndex createNew(Page page, Lemma lemma, Float rank) {
        SearchIndex index = new SearchIndex();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank(rank);
        return index;
    }

    @Override
    public SearchIndex createAndSave(Page page, Lemma lemma, Float rank) {
        SearchIndex index = createNew(page, lemma, rank);
        return save(index);
    }

    @Override
    public void deleteBySite(Site site) {
        indexRepository.deleteBySite(site);
    }

    @Override
    public void deleteByPage(Page page) {
        indexRepository.deleteByPage(page);
    }

    @Override
    public void deleteAll() {
        indexRepository.deleteAll();
    }

}
