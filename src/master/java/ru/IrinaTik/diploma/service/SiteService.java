package ru.IrinaTik.diploma.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.IrinaTik.diploma.config.SiteList;
import ru.IrinaTik.diploma.entity.Field;
import ru.IrinaTik.diploma.entity.Page;
import ru.IrinaTik.diploma.entity.Site;
import ru.IrinaTik.diploma.entity.SiteIndexingStatus;
import ru.IrinaTik.diploma.repository.SiteRepository;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SiteService {

    private final SiteRepository siteRepository;

    private final PageService pageService;
    private final SearchIndexService indexService;
    private final LemmaService lemmaService;

    private final SiteList siteList;

    public List<Site> getSitesFromConfig() {
        return siteList.getSiteList();
    }

    public List<Site> getAll() {
        return siteRepository.findAll();
    }

    public Site getById(int id) {
        return siteRepository.findById(id).orElse(null);
    }

    public Site getByUrl(String url) {
        return siteRepository.findByUrl(url).orElse(null);
    }

    public Site save(Site site) {
        return siteRepository.saveAndFlush(site);
    }

    public void deleteAll() {
        siteRepository.deleteAll();
    }

    public Site getByUrlOrElseCreateAndSaveNew(String url, String name) {
        Site site = getByUrl(url);
        if (site == null) {
            site = new Site();
            site.setUrl(url);
            site.setName(name);
        }
        return setSiteStatusAndSave(site, SiteIndexingStatus.INDEXING, "");
    }

    public Site setSiteStatusAndSave(Site site, SiteIndexingStatus status, String error) {
        site.setStatus(status);
        site.setLastError(error);
        site.setStatusTime(System.currentTimeMillis());
        return save(site);
    }

    public void deleteAllInfoRelatedToSite(Site site) {
        indexService.deleteBySite(site);
        lemmaService.deleteBySite(site);
        pageService.deleteBySite(site);
    }



}
