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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LemmaService {

    private static final String[] PARTICLES_NAMES = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "МС"};
    private static final int SNIPPET_CHARS_NUMBER = 5;
    private static final String SNIPPET_DELIMITER = "... ";

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

    public Map<String, Integer> getStrLemmasFromTextWithCount(String text) {
        String[] words = getRussianWordsFromText(text);
        return Arrays.stream(words)
                .filter(this::isWord)
                .collect(Collectors.toMap(
                        word -> getNormalFormOfWord(word),
                        count -> 1,
                        Integer::sum));
    }

    private Set<String> getStrLemmasFromText(String text) {
        String[] words = getRussianWordsFromText(text);
        return Arrays.stream(words)
                .filter(this::isWord)
                .map(this::getNormalFormOfWord)
                .collect(Collectors.toSet());
    }

    public List<Lemma> getLemmasFromTextSortedByFrequencyPresentInRep(String text) {
        Set<String> lemmas = getStrLemmasFromText(text);
        // TODO отфильтровать по проценту популярности леммы
        return lemmas.stream()
                .map(this::getByLemma)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(Lemma::getFrequency))
                .collect(Collectors.toList());
    }

    public String createSnippet(String text, List<Lemma> lemmas) {
        String[] parsedText = text.replaceAll("[\\s]{2,}", " ").split(" ");
        StringBuilder snippet = new StringBuilder();
        int startPos = -1;
        int endPos = -1;
        boolean isLemma = false;
        for (int i = 0; i < parsedText.length; i++) {
            for (Lemma lemma : lemmas) {
                isLemma = false;
                if (lemma.getLemma().equals(getLemmaFromWordIfPossible(parsedText[i]))) {
                    isLemma = true;
                    // следующая лемма за пределами предыдущего сниппета
                    if (i > endPos) {
                        startPos = calculateSnippetStartPos(i, endPos, snippet);
                        for (int snippetIterator = startPos; snippetIterator < endPos; snippetIterator++) {
                            snippet.append(parsedText[snippetIterator]).append(" ");
                        }
                    }
                    snippet.append("<b>").append(parsedText[i]).append("</b>").append(" ");
                    endPos = calculateSnippetEndPos(i, parsedText.length);
                    break;
                }
            }
            if (startPos != endPos) {
                if ((i <= endPos) && (!isLemma)) {
                    snippet.append(parsedText[i]).append(" ");
                }
            }
        }
        return snippet.append(SNIPPET_DELIMITER).toString();
    }

    private String getLemmaFromWordIfPossible(String word) {
        String wordInLowerCase = word.toLowerCase(Locale.ROOT).replaceAll("(?U)\\W+", "");
        if (isWord(wordInLowerCase)) {
            return getNormalFormOfWord(wordInLowerCase);
        }
        return word;
    }

    private boolean isWord(String word) {
        if (!word.isEmpty() && word.matches("[а-яё]+")) {
            return !anyWordBaseIsParticle(word);
        }
        return false;
    }

    private boolean anyWordBaseIsParticle(String word) {
        return luceneMorph.getMorphInfo(word).stream().anyMatch(this::isParticle);
    }

    private String getNormalFormOfWord(String word) {
        return luceneMorph.getNormalForms(word).get(0);
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

    private int calculateSnippetStartPos(int currentPos, int endPos, StringBuilder snippet) {
        int startPos;
        if (currentPos < SNIPPET_CHARS_NUMBER) {
            startPos = -1;
        } else {
            startPos = currentPos - SNIPPET_CHARS_NUMBER;
        }
        // границы предыдущего и нового сниппетов пересекаются - не надо делать разрыв
        if (startPos <= endPos) {
            startPos = endPos + 1;
        } else {
            snippet.append(SNIPPET_DELIMITER);
        }
        return startPos;
    }

    private int calculateSnippetEndPos(int currentPos, int textLength) {
        if (currentPos > textLength - SNIPPET_CHARS_NUMBER) {
            return textLength - 1;
        }
        return currentPos + SNIPPET_CHARS_NUMBER;
    }

}
