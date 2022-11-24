package ru.IrinaTik.diploma.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.IrinaTik.diploma.response.StatisticsResponse;
import ru.IrinaTik.diploma.service.StatisticsService;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/api/statistics")
    public ResponseEntity<StatisticsResponse> getStatistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

}
