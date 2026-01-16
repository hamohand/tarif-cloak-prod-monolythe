package com.muhend.backend.codesearch.repository;

import com.muhend.backend.codesearch.model.Position6Dz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Position6DzRepository extends JpaRepository<Position6Dz, Long> {
    // Search methods
    List<Position6Dz> findByCodeContaining(String code);
    List<Position6Dz> findByDescriptionContaining(String description);

    // Find by exact code
    Optional<Position6Dz> findByCode(String code);

    /**
     * Check if a position with the given code exists.
     *
     * @param code the position code
     * @return true if a position with the code exists, false otherwise
     */
    boolean existsByCode(String code);

    // Les positions qui commencent par le prefix : regex 'prefix%'
    @Query("SELECT a FROM Position6Dz a WHERE a.code LIKE :x")
    List<Position6Dz> findAllByPrefix(@Param("x") String prefix);
}
