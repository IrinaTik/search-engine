package ru.IrinaTik.diploma.response;

import lombok.Data;
import ru.IrinaTik.diploma.response.statistic_data.StatisticsData;

@Data
public class StatisticsResponse {

    private Boolean result;
    private StatisticsData statistics;
    private String error;

}
