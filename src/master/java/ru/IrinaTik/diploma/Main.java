package ru.IrinaTik.diploma;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.IrinaTik.diploma.response.SearchResponse;
import ru.IrinaTik.diploma.service.LemmaService;
import ru.IrinaTik.diploma.service.PageService;
import ru.IrinaTik.diploma.service.SearchIndexService;

import java.util.List;

@SpringBootApplication
public class Main implements CommandLineRunner {

    @Autowired
    private PageService pageService;

    @Autowired
    private LemmaService lemmaService;

    @Autowired
    private SearchIndexService indexService;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(String... args) {
        pageService.getSiteMap();

        String searchText = "лаборатория ученого совета";
        List<SearchResponse> result = pageService.getSearchResult(searchText);
        result.forEach(res -> System.out.println(res + "\n ====="));
        System.out.println(result.size());
        System.exit(0);
    }
}
