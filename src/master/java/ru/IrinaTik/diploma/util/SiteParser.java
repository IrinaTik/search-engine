package ru.IrinaTik.diploma.util;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import ru.IrinaTik.diploma.dao.Impl.PageDAOImpl;
import ru.IrinaTik.diploma.entity.Page;

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

    private Page page;
    private final PageDAOImpl pageDAO = new PageDAOImpl();

    public SiteParser(Page page) {
        this.page = page;
    }

    @Override
    protected void compute() {
        addPageToVisited(page);
        parse(page);
        pageDAO.save(page);
        if (page.getChildPages() != null) {
            List<SiteParser> parsers = new ArrayList<>();
            for (Page childPage : page.getChildPages()) {
                SiteParser parser = new SiteParser(childPage);
                parser.fork();
                parsers.add(parser);
            }
            for (SiteParser parser : parsers) {
                parser.join();
            }
        }
    }

    private void addPageToVisited(Page page) {
        siteMap.add(page);
    }

    public void parse(Page page) {
        try {
            Thread.sleep((long) (Math.random() * (5000 - 1000) + 1000));
            Connection connection = Jsoup.connect(page.getAbsPath())
                    .userAgent(USER_AGENT)
                    .referrer(REFERRER)
                    .timeout(120000)
                    .maxBodySize(0)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true);
            Connection.Response response = connection.execute();
            page.setCode(response.statusCode());
            page.setContent(response.body());
            Document doc = response.parse();
            if (page.getCode() == 200) {
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
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            System.out.println(page.getCode() + " : " + page.getAbsPath());
        }
    }

    public boolean isVisitedLink(String link) {
        return siteMap.stream().anyMatch(page -> page.getAbsPath().equals(link));
    }

}