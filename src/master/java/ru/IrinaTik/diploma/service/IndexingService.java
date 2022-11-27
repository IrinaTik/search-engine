package ru.IrinaTik.diploma.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.IrinaTik.diploma.entity.Field;
import ru.IrinaTik.diploma.entity.Page;
import ru.IrinaTik.diploma.entity.Site;
import ru.IrinaTik.diploma.entity.SiteIndexingStatus;
import ru.IrinaTik.diploma.response.IndexingResponse;
import ru.IrinaTik.diploma.util.SiteParser;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class IndexingService {

    private static final String INDEXING_STOPPED_ERROR = "Индексация прервана пользователем";
    private static final String INDEXING_ALREADY_STARTED_ERROR = "Индексация уже запущена";
    private static final String INDEXING_NOT_STARTED_ERROR = "Индексация не запущена";
    private static final String PAGE_NOT_LISTED_ERROR = "Данная страница находится за пределами сайтов, указанных в конфигурационном файле";

    private final boolean isCancelled = false;

    private final PageService pageService;
    private final FieldService fieldService;
    private final SearchIndexService indexService;
    private final LemmaService lemmaService;
    private final SiteService siteService;

    @Getter
    private List<Field> fieldList;

    public IndexingResponse indexingAllSites() {
        // TODO: индексация всего. НЗ! Каждый сайт в своем потоке
        IndexingResponse result = new IndexingResponse();
        return result;
    }

    public IndexingResponse indexingOneSite(String url, String name) {
        // TODO: разбить на мелкие части
        fieldList = fieldService.getAll();
        SiteParser.clearSiteMap();
        SiteParser.setFieldList(fieldList);
        IndexingResponse result = new IndexingResponse();
        siteService.setPageParsingError("");
        Site site = siteService.getByUrlOrElseCreateAndSaveNew(url, name);
        if (isCancelled) {
            siteService.setSiteStatusAndSave(site, SiteIndexingStatus.FAILED, INDEXING_STOPPED_ERROR);
            result.setResult(false);
            result.setError(INDEXING_STOPPED_ERROR);
            return result;
        }
        siteService.deleteAllInfoRelatedToSite(site);
        try {
            ForkJoinPool pool = new ForkJoinPool();
            pool.invoke(new SiteParser(new Page(site.getUrl()), site, siteService));
            if (siteService.getPageParsingError().isEmpty()) {
                siteService.setSiteStatusAndSave(site, SiteIndexingStatus.INDEXED, siteService.getPageParsingError());
                result.setResult(true);
            } else {
                siteService.setSiteStatusAndSave(site, SiteIndexingStatus.FAILED, siteService.getPageParsingError());
                result.setResult(false);
                result.setError(siteService.getPageParsingError());
            }
            SiteParser.siteMap.stream().map(page -> "№" + page.getId() + " - код " + page.getCode() + ": " + page.getRelPath()).forEach(System.out::println);
            System.out.println(SiteParser.siteMap.size());
            System.out.println(result);
        } catch (Exception ex) {
            ex.printStackTrace();
            result.setResult(false);
            result.setError("Ошибка индексации: сайт - " + site.getUrl() + System.lineSeparator() + ex.getMessage());
            siteService.setSiteStatusAndSave(site, SiteIndexingStatus.FAILED, "Ошибка индексации: сайт - " + site.getUrl() + System.lineSeparator() + ex.getMessage());
        }
        return result;
    }

    //TODO: метод, берущий список сайтов из конфига д.б. здесь

    //    public void getSiteMap(Site site) {
//        System.out.println("Indexing site: " + site.getName());
//        ForkJoinPool pool = new ForkJoinPool();
//        pool.invoke(new SiteParser(new Page(site.getUrl())));
//        SiteParser.siteMap.forEach(pageService::save);
//        SiteParser.siteMap.stream().map(page -> "№" + page.getId() + " - код " + page.getCode() + ": " + page.getRelPath()).forEach(System.out::println);
//        System.out.println("Всего: " + SiteParser.siteMap.size());
//        System.out.println("Код 200: " + SiteParser.siteMap.stream().filter(Page::isPageResponseOK).count());
//        fieldList = fieldService.getAll();
//        SiteParser.siteMap.stream().filter(Page::isPageResponseOK).forEach(page -> pageService.getLemmasFromPage(page, fieldList));
//    }

}
