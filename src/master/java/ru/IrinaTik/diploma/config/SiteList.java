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
@ConfigurationProperties(prefix = "sites")
public class SiteList {

    private List<Site> siteList;

}
