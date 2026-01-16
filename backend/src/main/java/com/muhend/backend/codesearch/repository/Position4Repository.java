package com.muhend.backend.codesearch.repository;

import com.muhend.backend.codesearch.model.Position4;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Position4Repository extends JpaRepository<Position4, Long> {
    // Search methods
    List<Position4> findByCodeContaining(String code);
    List<Position4> findByDescriptionContaining(String description);

    // Find by exact code
    Optional<Position4> findByCode(String code);

    /**
     * Check if a position with the given code exists.
     *
     * @param code the position code
     * @return true if a position with the code exists, false otherwise
     */
    boolean existsByCode(String code);

    // Les positions qui commencent par le prefix : regex 'prefix%'
    @Query("SELECT a FROM Position4 a WHERE a.code LIKE :x")
    List<Position4> findAllByPrefix(@Param("x") String prefix);
}
