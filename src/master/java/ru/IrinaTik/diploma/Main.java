package ru.IrinaTik.diploma;

import ru.IrinaTik.diploma.entity.Page;
import ru.IrinaTik.diploma.util.SiteParser;

import java.util.concurrent.ForkJoinPool;

public class Main {

    private static final String ROOT_SITE = "http://radiomv.ru/";

    public static void main(String[] args) {
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(new SiteParser(new Page(ROOT_SITE)));
        SiteParser.siteMap.stream().map(page -> page.getCode() + " : " + page.getRelPath()).forEach(System.out::println);
        System.out.println(SiteParser.siteMap.size());
    }

}
