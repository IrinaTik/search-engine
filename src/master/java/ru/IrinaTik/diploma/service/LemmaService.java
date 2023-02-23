package ru.IrinaTik.diploma.service;

import ru.IrinaTik.diploma.entity.Lemma;
import ru.IrinaTik.diploma.entity.Site;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface LemmaService {
    List<Lemma> getAll();

    Lemma getById(int id);

    Lemma getByLemmaAndSite(String lemma, Site site);

    List<Lemma> getBySite(Site site);

    void deleteBySite(Site site);

    void deleteAll();

    Lemma save(Lemma lemma);

    List<Lemma> saveAll(Collection<Lemma> lemmas);

    Lemma createNew(String lemmaString, Site site);

    Map<String, Integer> getStrLemmasFromTextWithCount(String text);

    List<Lemma> getLemmasFromTextSortedByFrequencyPresentInRep(String text, int frequencyLimit, Site site);

    String createSnippet(String text, List<Lemma> lemmas);
}
