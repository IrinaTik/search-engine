package ru.IrinaTik.diploma.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.IrinaTik.diploma.entity.Page;
import ru.IrinaTik.diploma.entity.Site;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    List<Page> getBySite(Site site);

    @Modifying
    @Transactional
    void deleteBySite(Site site);

    @Query(value = "SELECT p.* FROM pages p WHERE p.site_id = ?1 AND p.path = ?2",
            nativeQuery = true)
    Page getByRelPathAndSite(Site site, String path);
}
