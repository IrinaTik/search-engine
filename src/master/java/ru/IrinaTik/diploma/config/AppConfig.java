package ru.IrinaTik.diploma.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import ru.IrinaTik.diploma.entity.Site;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "config")
public class AppConfig {

    private List<Site> siteList;
    private String useragent;
    private String referrer;

}
