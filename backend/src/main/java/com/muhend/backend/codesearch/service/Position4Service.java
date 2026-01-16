package com.muhend.backend.codesearch.service;

import com.muhend.backend.codesearch.model.Position4;
import com.muhend.backend.codesearch.repository.Position4Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing Position4 entities.
 */
@Service
public class Position4Service {

    private final Position4Repository position4Repository;
    @Autowired
    public Position4Service(Position4Repository position4Repository) {
        this.position4Repository = position4Repository;
    }

    /**
     * Get all positions.
     *
     * @return list of all positions
     */
    public List<Position4> getAllPositions() {
        return position4Repository.findAll();
    }

    /**
     * Get a position by its ID.
     *
     * @param id the position ID
     * @return an Optional containing the position if found
     */
    public Optional<Position4> getPositionById(Long id) {
        return position4Repository.findById(id);
    }

    // Get a Position4 description by code
    public String getDescription(String code) {
        return position4Repository.findByCode(code).map(Position4::getDescription).orElse(null);
    }

    /**
     * Get a position by its code.
     *
     * @param code the position code
     * @return an Optional containing the position if found
     */
    public Optional<Position4> getPositionByCode(String code) {
        return position4Repository.findByCode(code);
    }

    /**
     * Save a new position or update an existing one.
     *
     * @param position the position to save
     * @return the saved position
     */
    @Transactional
    public Position4 savePosition(Position4 position) {
        return position4Repository.save(position);
    }

    /**
     * Delete a position by its ID.
     *
     * @param id the position ID
     */
    @Transactional
    public void deletePosition(Long id) {
        position4Repository.deleteById(id);
    }

    /**
     * Check if a position with the given code exists.
     *
     * @param code the position code
     * @return true if a position with the code exists, false otherwise
     */
    public boolean existsByCode(String code) {
        return position4Repository.existsByCode(code);
    }

    public List<Position4> getPosition4sByPrefix(String prefix){
        return position4Repository.findAllByPrefix(prefix);
    }

    public Iterable<Object> findByCodeChapitre(String code) {
        return null;
    }
}