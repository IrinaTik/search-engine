package ru.IrinaTik.diploma.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.IrinaTik.diploma.entity.Page;
import ru.IrinaTik.diploma.repository.PageRepository;
import ru.IrinaTik.diploma.util.SiteParser;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PageService {

    private static final String ROOT_SITE = "http://www.playback.ru/";

    private final PageRepository pageRepository;

    public List<Page> getAll() {
        return pageRepository.findAll();
    }

    public Page getById(int id) {
        return pageRepository.findById(id).orElse(null);
    }

    public Page add(Page page) {
        return pageRepository.saveAndFlush(page);
    }

    public void getSiteMap() {
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(new SiteParser(new Page(ROOT_SITE)));
        SiteParser.siteMap.stream().filter(Page::isPageResponseOK).forEach(this::add);
        SiteParser.siteMap.stream().map(page -> "№" + page.getId() + " - код " + page.getCode() + ": " + page.getRelPath()).forEach(System.out::println);
        System.out.println("Всего: " + SiteParser.siteMap.size());
        System.out.println("Код 200: " + SiteParser.siteMap.stream().filter(Page::isPageResponseOK).count());
    }

}