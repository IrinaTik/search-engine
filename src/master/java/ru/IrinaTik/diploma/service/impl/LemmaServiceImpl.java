package ru.IrinaTik.diploma.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.IrinaTik.diploma.entity.Lemma;
import ru.IrinaTik.diploma.entity.Site;
import ru.IrinaTik.diploma.repository.LemmaRepository;
import ru.IrinaTik.diploma.service.LemmaService;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LemmaServiceImpl implements LemmaService {

    private static final String[] PARTICLES_NAMES = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "МС"};
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

    @Override
    public List<Lemma> getAll() {
        return lemmaRepository.findAll();
    }

    @Override
    public Lemma getById(int id) {
        return lemmaRepository.findById(id).orElse(null);
    }

    @Override
    public Lemma getByLemmaAndSite(String lemma, Site site) {
        return lemmaRepository.findByLemmaAndSite(lemma, site.getId()).orElse(null);
    }

    @Override
    public List<Lemma> getBySite(Site site) {
        List<Lemma> lemmas = lemmaRepository.findBySite(site);
        if (lemmas == null) {
            return new ArrayList<>();
        }
        return lemmas;
    }

    @Override
    public void deleteBySite(Site site) {
        lemmaRepository.deleteBySite(site);
    }

    @Override
    public void deleteAll() {
        lemmaRepository.deleteAll();
    }

    @Override
    public Lemma save(Lemma lemma) {
        return lemmaRepository.saveAndFlush(lemma);
    }

    @Override
    public List<Lemma> saveAll(Collection<Lemma> lemmas) {
        return lemmaRepository.saveAllAndFlush(lemmas);
    }

    @Override
    public Lemma createNew(String lemmaString, Site site) {
        Lemma lemma = new Lemma();
        lemma.setLemma(lemmaString);
        lemma.setFrequency(1);
        lemma.setSite(site);
        return lemma;
    }

    @Override
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

    @Override
    public List<Lemma> getLemmasFromTextSortedByFrequencyPresentInRep(String text, int frequencyLimit, Site site) {
        Set<String> lemmas = getStrLemmasFromText(text);
        return lemmas.stream()
                .map(lemma -> getByLemmaAndSite(lemma, site))
                .filter(lemma -> lemma != null && lemma.getFrequency() <= frequencyLimit)
                .sorted(Comparator.comparingInt(Lemma::getFrequency))
                .collect(Collectors.toList());
    }

    @Override
    public String createSnippet(String text, List<Lemma> lemmas) {
        String[] parsedText = text.replaceAll("[\\s]{2,}", " ").split(" ");
        StringBuilder snippet = new StringBuilder();
        int lastLemmaPos = -1;
        for (int i = 0; i < parsedText.length; i++) {
            for (Lemma lemma : lemmas) {
                if (lemma.getLemma().equals(getLemmaFromWordIfPossible(parsedText[i]))) {
                    snippet.append(createSnippetPart(parsedText, lastLemmaPos, i));
                    lastLemmaPos = i;
                    break;
                }
            }
            if ((i == parsedText.length - 1) && (lastLemmaPos == i - 1)) {
                snippet.append(parsedText[i]).append(" ");
            }
            if ((i - 2 == lastLemmaPos) && (i >= 2)) {
                snippet.append(parsedText[i - 1]).append(" ");
            }
        }
        return snippet.append(SNIPPET_DELIMITER).toString();
    }

    private String createSnippetPart(String[] parsedText, int lastLemmaPos, int currentPos) {
        String snippetPart = "";
        if (currentPos - lastLemmaPos > 2) {
            snippetPart = snippetPart.concat(SNIPPET_DELIMITER);
        }
        if (currentPos - lastLemmaPos >= 2) {
            snippetPart = snippetPart.concat(parsedText[currentPos - 1]).concat(" ");
        }
        return snippetPart.concat("<b>").concat(parsedText[currentPos]).concat("</b>").concat(" ");
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

}
