package ru.IrinaTik.diploma.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import ru.IrinaTik.diploma.entity.Page;
import ru.IrinaTik.diploma.entity.Site;

import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

public class SiteParser extends RecursiveAction {

    private static final String CSS_QUERY = "a[href]";
    private static final String SCROLLUP_LINK = "#";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36";
    private static final String REFERRER = "https://www.google.com";

    @Setter(AccessLevel.PRIVATE)
    private static boolean isCancelled;

    private final Site site;
    private final Page page;
    private String error;
    @Getter
    private final Set<Page> siteMap;

    public SiteParser(Page page, Site site, String error) {
        this.page = page;
        this.site = site;
        page.setSite(site);
        this.error = error;
        this.siteMap = Collections.synchronizedSet(new HashSet<>());
        setCancelled(false);
    }

    private SiteParser(Page page, Site site, String error, Set<Page> siteMap) {
        this.page = page;
        this.site = site;
        page.setSite(site);
        this.error = error;
        this.siteMap = siteMap;
    }

    @Override
    protected void compute() {
        if (isCancelled) {
            return;
        }
        addPageToVisited();
        parse();
        if (page.getChildPages() != null) {
            List<SiteParser> parsers = new ArrayList<>();
            for (Page childPage : page.getChildPages()) {
                if (isCancelled) {
                    return;
                }
                SiteParser parser = new SiteParser(childPage, site, error, siteMap);
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
            System.out.println("Page : " + page.getRelPath() + " , site : " + page.getSite().getUrl());
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
            error = "Ошибка при парсинге страницы " + page.getRelPath() + " : " + ex.getMessage();
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

    public static void stopParsing() {
        setCancelled(true);
    }

}
