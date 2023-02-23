package ru.IrinaTik.diploma.service;

import org.springframework.transaction.annotation.Transactional;
import ru.IrinaTik.diploma.config.AppConfig;
import ru.IrinaTik.diploma.entity.Site;
import ru.IrinaTik.diploma.entity.SiteIndexingStatus;
import ru.IrinaTik.diploma.response.IndexingResponse;

public interface IndexingService {

    IndexingResponse indexingAllSites();

    IndexingResponse stopIndexing();

    IndexingResponse indexingAddedPage(String url);

    IndexingResponse indexingOneSite(String url, String name);

    AppConfig getAppConfig();

    IndexingResponse setResultAndSiteStatus(Site site, SiteIndexingStatus status, String error);
}
