package com.muhend.backend.codesearch.repository;

import com.muhend.backend.codesearch.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {

    Optional<Section> findByCode(String code);

    //List method
//    Iterable<Section> getAll();
//    // Search methods
//    Section findByCode(String code);
//    Iterable<Section> findByDescriptionContaining(String description);
//    Iterable<Section> findByCodeContaining(String code);


}
