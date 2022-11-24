package ru.IrinaTik.diploma.response.statistic_data;

import lombok.Data;
import ru.IrinaTik.diploma.entity.SiteIndexingStatus;

@Data
public class StatisticsDetailedInfo {

    private String url;
    private String name;
    private SiteIndexingStatus status;
    private long statusTime;
    private String error;
    private int pages;
    private int lemmas;

}
