package ru.IrinaTik.diploma.service;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LemmaService {

    private static final String[] PARTICLES_NAMES = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "МС"};

    private static LuceneMorphology luceneMorph;

    static {
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Integer> getLemmas(String text) {
        String[] words = getRussianWordsFromText(text);
        return Arrays.stream(words)
                .filter(word -> !anyWordBaseIsParticle(word))
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
