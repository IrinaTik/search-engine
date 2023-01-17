package ru.IrinaTik.diploma.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.IrinaTik.diploma.entity.Lemma;
import ru.IrinaTik.diploma.entity.Page;
import ru.IrinaTik.diploma.entity.SearchIndex;
import ru.IrinaTik.diploma.entity.Site;
import ru.IrinaTik.diploma.repository.SearchIndexRepository;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SearchIndexService {

    private final SearchIndexRepository indexRepository;

    public List<SearchIndex> getAll() {
        return indexRepository.findAll();
    }

    public SearchIndex getById(int id) {
        return indexRepository.findById(id).orElse(null);
    }

    public SearchIndex save(SearchIndex index) {
        return indexRepository.saveAndFlush(index);
    }

    public List<SearchIndex> saveAll(Collection<SearchIndex> indexes) {
        return indexRepository.saveAllAndFlush(indexes);
    }

    public SearchIndex createNew(Page page, Lemma lemma, Float rank) {
        SearchIndex index = new SearchIndex();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank(rank);
        return index;
    }

    public SearchIndex createAndSave(Page page, Lemma lemma, Float rank) {
        SearchIndex index = createNew(page, lemma, rank);
        return save(index);
    }

    public void deleteBySite(Site site) {
        indexRepository.deleteBySite(site);
    }

    public void deleteAll() {
        indexRepository.deleteAll();
    }

}
