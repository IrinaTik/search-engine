package ru.IrinaTik.diploma.service;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.IrinaTik.diploma.entity.Lemma;
import ru.IrinaTik.diploma.repository.LemmaRepository;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LemmaService {

    private static final String[] PARTICLES_NAMES = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "МС"};

    private static LuceneMorphology luceneMorph;

    private final LemmaRepository lemmaRepository;

    static {
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Lemma> getAll() {
        return lemmaRepository.findAll();
    }

    public Lemma getById(int id) {
        return lemmaRepository.findById(id).orElse(null);
    }

    public Lemma getByLemma(String lemma) {
        return lemmaRepository.findByLemma(lemma);
    }

    public Lemma save(Lemma lemma) {
        return lemmaRepository.saveAndFlush(lemma);
    }

    public Lemma createAndSave(String lemmaString) {
        Lemma lemma = new Lemma();
        lemma.setLemma(lemmaString);
        lemma.setFrequency(1);
        return save(lemma);
    }

    public Map<String, Integer> getLemmasFromText(String text) {
        String[] words = getRussianWordsFromText(text);
        return Arrays.stream(words)
                .filter(word -> !word.isEmpty() && !anyWordBaseIsParticle(word))
                .collect(Collectors.toMap(
                        word -> luceneMorph.getNormalForms(word).get(0),
                        count -> 1,
                        Integer::sum));
    }

    private boolean anyWordBaseIsParticle(String word) {
        return luceneMorph.getMorphInfo(word).stream().anyMatch(this::isParticle);
    }

    private boolean isParticle(String form) {
        for (String particle : PARTICLES_NAMES) {
            if (form.contains(particle)) {
                return true;
            }
        }
        return false;
    }

    private String[] getRussianWordsFromText(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .split("\\s+");
    }

}
