package ru.IrinaTik.diploma.response.statistic_data;

import lombok.Data;

@Data
public class StatisticsTotalInfo {

    private int sites;
    private int pages;
    private int lemmas;
    private boolean isIndexing;

}
