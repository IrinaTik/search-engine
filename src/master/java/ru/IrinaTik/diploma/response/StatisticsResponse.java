package ru.IrinaTik.diploma.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import ru.IrinaTik.diploma.response.statistic_data.StatisticsData;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatisticsResponse {

    private Boolean result;
    private StatisticsData statistics;
    private String error;

}
