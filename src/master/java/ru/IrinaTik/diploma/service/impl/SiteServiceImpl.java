package ru.IrinaTik.diploma.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.IrinaTik.diploma.entity.Site;
import ru.IrinaTik.diploma.entity.SiteIndexingStatus;
import ru.IrinaTik.diploma.repository.SiteRepository;
import ru.IrinaTik.diploma.service.LemmaService;
import ru.IrinaTik.diploma.service.PageService;
import ru.IrinaTik.diploma.service.SearchIndexService;
import ru.IrinaTik.diploma.service.SiteService;

import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SiteServiceImpl implements SiteService {

    private final SiteRepository siteRepository;

    private final PageService pageService;
    private final SearchIndexService indexService;
    private final LemmaService lemmaService;

    @Override
    public List<Site> getAll() {
        return siteRepository.findAll();
    }

    @Override
    public Site getById(int id) {
        return siteRepository.findById(id).orElse(null);
    }

    @Override
    public Site getByUrl(String url) {
        return siteRepository.findByUrl(url).orElse(null);
    }

    @Override
    public Site save(Site site) {
        return siteRepository.saveAndFlush(site);
    }

    @Override
    public void deleteAll() {
        siteRepository.deleteAll();
    }

    @Override
    public Site getByUrlOrElseCreateAndSaveNew(String url, String name) {
        Site site = getByUrl(url);
        if (site == null) {
            site = new Site();
            site.setUrl(url);
            site.setName(name);
        }
        return setSiteStatusAndSave(site, SiteIndexingStatus.INDEXING, "");
    }

    @Override
    public Site setSiteStatusAndSave(Site site, SiteIndexingStatus status, String error) {
        site.setStatus(status);
        site.setLastError(error);
        site.setStatusTime(System.currentTimeMillis());
        return save(site);
    }

    @Override
    public void deleteAllInfoRelatedToSite(Site site) {
        indexService.deleteBySite(site);
        lemmaService.deleteBySite(site);
        pageService.deleteBySite(site);
    }



}
