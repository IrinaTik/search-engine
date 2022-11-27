package ru.IrinaTik.diploma.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.IrinaTik.diploma.entity.SearchIndex;
import ru.IrinaTik.diploma.entity.Site;

@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, Integer> {

    @Modifying
    @Query(value = "DELETE si FROM search_index si " +
            "INNER JOIN pages p ON (si.page_id = p.id) " +
            "WHERE p.site_id = :#{#site.id}",
            nativeQuery = true)
    void deleteBySite(@Param("site") Site site);
}
