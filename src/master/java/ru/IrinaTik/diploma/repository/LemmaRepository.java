package ru.IrinaTik.diploma.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.IrinaTik.diploma.entity.Lemma;
import ru.IrinaTik.diploma.entity.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Query(value = "SELECT l.* FROM lemma l WHERE l.lemma = ?1 AND l.site_id = ?2",
            nativeQuery = true)
    Optional<Lemma> findByLemmaAndSite(String lemma, int siteID);

    List<Lemma> findBySite(Site site);

    @Modifying
    @Transactional
    void deleteBySite(Site site);

}
