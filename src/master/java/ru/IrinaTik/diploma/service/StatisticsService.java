package ru.IrinaTik.diploma.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.IrinaTik.diploma.entity.Site;
import ru.IrinaTik.diploma.entity.SiteIndexingStatus;
import ru.IrinaTik.diploma.response.StatisticsResponse;
import ru.IrinaTik.diploma.response.statistic_data.StatisticsData;
import ru.IrinaTik.diploma.response.statistic_data.StatisticsDetailedInfo;
import ru.IrinaTik.diploma.response.statistic_data.StatisticsTotalInfo;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class StatisticsService {

    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;

    public StatisticsResponse getStatistics() {
        StatisticsResponse response = new StatisticsResponse();
        try {
            response.setStatistics(getStatisticsData());
            response.setResult(true);
        } catch (Exception ex) {
            response.setResult(false);
            response.setError("Ошибка получения статистики: " + ex.getMessage());
        }
        return response;
    }

    private StatisticsData getStatisticsData() {
        StatisticsData statisticsData = new StatisticsData();
        List<Site> sites = siteService.getAll();
        statisticsData.setTotal(getStatisticsTotalInfo(sites));
        statisticsData.setDetailed(getStatisticsDetailedInfo(sites));
        return statisticsData;
    }

    private StatisticsTotalInfo getStatisticsTotalInfo(List<Site> sites) {
        StatisticsTotalInfo totalInfo = new StatisticsTotalInfo();
        totalInfo.setSites(sites.size());
        totalInfo.setPages(pageService.getAll().size());
        totalInfo.setLemmas(lemmaService.getAll().size());
        totalInfo.setIndexing(sites.stream().anyMatch(site -> site.getStatus().equals(SiteIndexingStatus.INDEXING)));
        return totalInfo;
    }

    private List<StatisticsDetailedInfo> getStatisticsDetailedInfo(List<Site> sites) {
        List<StatisticsDetailedInfo> detailedInfoList = new ArrayList<>();
        for (Site site : sites) {
            StatisticsDetailedInfo detailedInfo = new StatisticsDetailedInfo();
            detailedInfo.setUrl(site.getUrl());
            detailedInfo.setName(site.getName());
            detailedInfo.setStatus(site.getStatus());
            detailedInfo.setStatusTime(site.getStatusTime());
            detailedInfo.setError(site.getLastError());
            detailedInfo.setPages(pageService.getBySite(site).size());
            detailedInfo.setLemmas(lemmaService.getBySite(site).size());
            detailedInfoList.add(detailedInfo);
        }
        return detailedInfoList;
    }
}
