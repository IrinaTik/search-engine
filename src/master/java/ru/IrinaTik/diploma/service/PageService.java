package ru.IrinaTik.diploma.service;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.IrinaTik.diploma.entity.Field;
import ru.IrinaTik.diploma.entity.Lemma;
import ru.IrinaTik.diploma.entity.Page;
import ru.IrinaTik.diploma.entity.SearchIndex;
import ru.IrinaTik.diploma.repository.PageRepository;
import ru.IrinaTik.diploma.util.SiteParser;

import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PageService {

    private static final String ROOT_SITE = "http://www.playback.ru/";

    private final PageRepository pageRepository;
    private final FieldService fieldService;
    private final LemmaService lemmaService;
    private final SearchIndexService indexService;

    private List<Field> fieldList;

    public List<Page> getAll() {
        return pageRepository.findAll();
    }

    public Page getById(int id) {
        return pageRepository.findById(id).orElse(null);
    }

    public Page save(Page page) {
        return pageRepository.saveAndFlush(page);
    }

    public void getSiteMap() {
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(new SiteParser(new Page(ROOT_SITE)));
        SiteParser.siteMap.stream().forEach(this::save);
        SiteParser.siteMap.stream().map(page -> "№" + page.getId() + " - код " + page.getCode() + ": " + page.getRelPath()).forEach(System.out::println);
        System.out.println("Всего: " + SiteParser.siteMap.size());
        System.out.println("Код 200: " + SiteParser.siteMap.stream().filter(Page::isPageResponseOK).count());
        fieldList = fieldService.getAll();
        SiteParser.siteMap.stream().filter(Page::isPageResponseOK).forEach(this::getLemmasFromPage);
    }

    public void getLemmasFromPage(Page page) {
        // работа с одной страницей
        Map<String, Float> uniquePageLemmasWithRank = new HashMap<>();
        Document doc = Jsoup.parse(page.getContent());
        for (Field field : fieldList) {
            Elements elements = doc.select(field.getSelector());
            String text = elements.text();
            Map<String, Integer> fieldLemmas = lemmaService.getLemmasFromText(text);
            fieldLemmas.forEach((fieldLemma, count) -> {
                if (uniquePageLemmasWithRank.containsKey(fieldLemma)) {
                    uniquePageLemmasWithRank.put(fieldLemma, uniquePageLemmasWithRank.get(fieldLemma) + field.getWeight() * count);
                } else {
                    uniquePageLemmasWithRank.put(fieldLemma, field.getWeight() * count);
                }
            });
        }
        saveLemmaAndIndex(uniquePageLemmasWithRank, page);
    }

    private void saveLemmaAndIndex(Map<String, Float> uniquePageLemmasWithRank, Page page) {
        for (Map.Entry<String, Float> entry : uniquePageLemmasWithRank.entrySet()) {
            Lemma lemma = lemmaService.getByLemma(entry.getKey());
            if (lemma == null) {
                lemma = lemmaService.createAndSave(entry.getKey());
            } else {
                lemma.setFrequency(lemma.getFrequency() + 1);
                lemmaService.save(lemma);
            }
            indexService.createAndSave(page, lemma, entry.getValue());
        }
    }

}