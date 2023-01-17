package ru.IrinaTik.diploma.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import ru.IrinaTik.diploma.entity.Page;
import ru.IrinaTik.diploma.entity.Site;
import ru.IrinaTik.diploma.service.IndexingService;

import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

@Log4j2
public class SiteParser extends RecursiveAction {

    private static final String CSS_QUERY = "a[href]";
    private static final String SCROLLUP_LINK = "#";

    @Setter(AccessLevel.PRIVATE)
    private static boolean isCancelled;

    private final Site site;
    private final Page page;
    private final IndexingService indexingService;

    @Getter
    private final Set<Page> siteMap;

    public SiteParser(Page page, Site site, IndexingService service) {
        this.page = page;
        this.site = site;
        page.setSite(site);
        this.indexingService = service;
        this.siteMap = Collections.synchronizedSet(new HashSet<>());
        setCancelled(false);
    }

    private SiteParser(Page page, Site site, IndexingService service, Set<Page> siteMap) {
        this.page = page;
        this.site = site;
        page.setSite(site);
        this.indexingService = service;
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
                SiteParser parser = new SiteParser(childPage, site, indexingService, siteMap);
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
            log.info("Parsing page {}, site -> {}", page.getRelPath(), page.getSite().getUrl());
            Thread.sleep((long) (Math.random() * (20000 - 5000) + 1000));
            Connection connection = Jsoup.connect(page.getAbsPath())
                    .userAgent(indexingService.getAppConfig().getUseragent())
                    .referrer(indexingService.getAppConfig().getReferrer())
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
            log.error("Error while parsing page {} -> {}", page.getRelPath(), ex.getMessage());
            indexingService.setPageParsingError("Ошибка при парсинге страницы " + page.getRelPath() + " -> " + ex.getMessage());
        } finally {
            log.info("Parsing complete with code {} for page {}", page.getCode(), page.getAbsPath());
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
