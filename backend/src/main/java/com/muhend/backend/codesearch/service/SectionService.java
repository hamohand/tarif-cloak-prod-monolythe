package com.muhend.backend.codesearch.service;

import com.muhend.backend.codesearch.model.Section;
import com.muhend.backend.codesearch.repository.SectionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SectionService {
    private final SectionRepository sectionRepository;
    public SectionService(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    /**
     * Get all sections.
     *
     * @return list of all sections
     */
    public List<Section> getAllSections() {
        return sectionRepository.findAll();
    }

    public Optional<Section> getSectionByCode(String code) {
        return sectionRepository.findByCode(code);
    }

    public Iterable<Object> findAll() {
        return null;
    }

    public String getDescription(String code) {
        return sectionRepository.findByCode(code).map(Section::getDescription).orElse(null);
    }
    public String getDescriptionByCode(String code) {
        return sectionRepository.findByCode(code).map(Section::getDescription).orElse(null);
    }
}
