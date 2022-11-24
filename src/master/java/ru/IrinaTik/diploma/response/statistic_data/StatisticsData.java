package ru.IrinaTik.diploma.response.statistic_data;

import lombok.Data;

import java.util.List;

@Data
public class StatisticsData {

    private StatisticsTotalInfo total;
    private List<StatisticsDetailedInfo> detailed;

}
