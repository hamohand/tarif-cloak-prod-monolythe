package com.muhend.backend.codesearch.repository;

import com.muhend.backend.codesearch.model.Chapitre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapitreRepository extends JpaRepository<Chapitre, Long> {
    // Search methods
    List<Chapitre> findByCodeContaining(String code);
    List<Chapitre> findByDescriptionContaining(String description);
    List<Chapitre> findBySectionContaining(String section);

    // Find by exact code
    Optional<Chapitre> findByCode(String code);

    Optional<Chapitre> findByCodeStartingWith(String code);

    /**
     * Check if a chapter with the given code exists.
     *
     * @param code the chapter code
     * @return true if a chapter with the code exists, false otherwise
     */
    boolean existsByCode(String code);

    /**
     * Find chapters by section.
     *
     * @param section the section number
     * @return a list of chapters belonging to the section
     */
    List<Chapitre> findBySection(String section);

    @Query("""
    SELECT c
    FROM Chapitre c
    WHERE TRIM(c.code) = TRIM(:code)
""")
    Optional<Chapitre> findByCodeTrimmed(@Param("code") String code);
}
