package ru.IrinaTik.diploma.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.IrinaTik.diploma.entity.Site;
import ru.IrinaTik.diploma.response.IndexingResponse;
import ru.IrinaTik.diploma.service.IndexingService;
import ru.IrinaTik.diploma.service.SiteService;

import java.util.List;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class IndexingController {

    private final IndexingService indexingService;
    private final SiteService siteService;

    @GetMapping("/api/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        // TODO: убрать заглушку из одного сайта, поставить индексацию всех сайтов из списка
        List<Site> sites = siteService.getSitesFromConfig();
        IndexingResponse response = indexingService.indexingOneSite(sites.get(2).getUrl(), sites.get(2).getName());
        return ResponseEntity.ok(response);
    }
}
