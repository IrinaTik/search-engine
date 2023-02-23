package ru.IrinaTik.diploma.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.IrinaTik.diploma.response.IndexingResponse;
import ru.IrinaTik.diploma.service.IndexingService;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class IndexingController {

    private final IndexingService indexingService;

    @GetMapping("/api/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        IndexingResponse response = indexingService.indexingAllSites();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/indexPage")
    public ResponseEntity<IndexingResponse> startIndexingOnePage(@RequestParam String url) {
        IndexingResponse response = indexingService.indexingAddedPage(url);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        IndexingResponse response = indexingService.stopIndexing();
        return ResponseEntity.ok(response);
    }

}
