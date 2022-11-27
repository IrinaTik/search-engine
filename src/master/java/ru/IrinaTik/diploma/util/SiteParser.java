package ru.IrinaTik.diploma.util;

import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import ru.IrinaTik.diploma.entity.Field;
import ru.IrinaTik.diploma.entity.Page;
import ru.IrinaTik.diploma.entity.Site;
import ru.IrinaTik.diploma.service.SiteService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

public class SiteParser extends RecursiveAction {

    private static final String CSS_QUERY = "a[href]";
    private static final String SCROLLUP_LINK = "#";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36";
    private static final String REFERRER = "https://www.google.com";

    public static List<Page> siteMap = Collections.synchronizedList(new ArrayList<>());
    @Setter
    private static List<Field> fieldList;
    private final SiteService siteService;

    private final Site site;
    private final Page page;

    public SiteParser(Page page, Site site, SiteService siteService) {
        this.page = page;
        this.site = site;
        page.setSite(site);
        this.siteService = siteService;
    }

    @Override
    protected void compute() {
        // TODO: сюда проверка на isCancelled
        addPageToVisited();
        parse();
//        System.out.println("Going to siteService for page : " + page.getRelPath() + " , site : " + page.getSite().getUrl());
        siteService.createPageWithLemmasAndIndexes(fieldList, page);
        if (page.getChildPages() != null) {
            // TODO: сюда проверка на isCancelled
            List<SiteParser> parsers = new ArrayList<>();
            for (Page childPage : page.getChildPages()) {
                SiteParser parser = new SiteParser(childPage, site, siteService);
                parser.fork();
                parsers.add(parser);
            }
            for (SiteParser parser : parsers) {
                parser.join();
            }
        }
    }

    private void addPageToVisited() {
        siteMap.add(page);
    }

    public void parse() {
        try {
            Thread.sleep((long) (Math.random() * (20000 - 5000) + 1000));
            Connection connection = Jsoup.connect(page.getAbsPath())
                    .userAgent(USER_AGENT)
                    .referrer(REFERRER)
                    .timeout(120000)
                    .maxBodySize(0)
                    .ignoreContentType(true);
            Connection.Response response = connection.execute();
            page.setCode(response.statusCode());
            page.setContent(response.body());
            Document doc = response.parse();
            if (page.isPageResponseOK()) {
                parsePageLinks(doc);
            }
        } catch (HttpStatusException ex) {
            page.setCode(ex.getStatusCode());
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Ошибка при парсинге страницы " + page.getRelPath() + " : " + ex.getMessage());
            siteService.setPageParsingError("Ошибка при парсинге страницы " + page.getRelPath() + " : " + ex.getMessage());
        } finally {
            System.out.println(page.getCode() + " : " + page.getAbsPath());
        }
    }

    private void parsePageLinks(Document doc) {
        Elements links = doc.select(CSS_QUERY);
        List<String> actualLinks = links.stream()
                .map(linkCode -> linkCode.absUrl("href"))
                .filter(link -> link.startsWith(page.getAbsPath()) && !link.equals(page.getAbsPath() + SCROLLUP_LINK))
                .distinct()
                .filter(link -> !isVisitedLink(link))
                .collect(Collectors.toList());
        if (!actualLinks.isEmpty()) {
            page.setChildPages(actualLinks);
        }
    }

    public boolean isVisitedLink(String link) {
        return siteMap.stream().anyMatch(page -> page.getAbsPath().equals(link));
    }

    public static void clearSiteMap() {
        if (!siteMap.isEmpty()) {
            siteMap.clear();
        }
    }

}
