package ru.IrinaTik.diploma.service;

import ru.IrinaTik.diploma.entity.Site;
import ru.IrinaTik.diploma.entity.SiteIndexingStatus;

import java.util.List;

public interface SiteService {
    List<Site> getAll();

    Site getById(int id);

    Site getByUrl(String url);

    Site save(Site site);

    void deleteAll();

    Site getByUrlOrElseCreateAndSaveNew(String url, String name);

    Site setSiteStatusAndSave(Site site, SiteIndexingStatus status, String error);

    void deleteAllInfoRelatedToSite(Site site);
}
