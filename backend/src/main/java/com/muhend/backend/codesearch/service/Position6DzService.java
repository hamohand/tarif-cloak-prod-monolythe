package com.muhend.backend.codesearch.service;

import com.muhend.backend.codesearch.model.Position6Dz;
import com.muhend.backend.codesearch.repository.Position6DzRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing Position6Dz entities.
 */
@Service
public class Position6DzService {

    private final Position6DzRepository position6DzRepository;

    @Autowired
    public Position6DzService(Position6DzRepository position6DzRepository) {
        this.position6DzRepository = position6DzRepository;
    }

    /**
     * Get all positions.
     *
     * @return list of all positions
     */
    public List<Position6Dz> getAllPositions() {
        return position6DzRepository.findAll();
    }

    /**
     * Get a position by its ID.
     *
     * @param id the position ID
     * @return an Optional containing the position if found
     */
    public Optional<Position6Dz> getPositionById(Long id) {
        return position6DzRepository.findById(id);
    }

    // Get a Position6Dz description by code
    public String getDescription(String code) {
        return position6DzRepository.findByCode(code).map(Position6Dz::getDescription).orElse(null);
    }

    /**
     * Get a position by its code.
     *
     * @param code the position code
     * @return an Optional containing the position if found
     */
    public Optional<Position6Dz> getPositionByCode(String code) {
        return position6DzRepository.findByCode(code);
    }

    /**
     * Save a new position or update an existing one.
     *
     * @param position the position to save
     * @return the saved position
     */
    @Transactional
    public Position6Dz savePosition(Position6Dz position) {
        return position6DzRepository.save(position);
    }

    /**
     * Delete a position by its ID.
     *
     * @param id the position ID
     */
    @Transactional
    public void deletePosition(Long id) {
        position6DzRepository.deleteById(id);
    }

    /**
     * Check if a position with the given code exists.
     *
     * @param code the position code
     * @return true if a position with the code exists, false otherwise
     */
    public boolean existsByCode(String code) {
        return position6DzRepository.existsByCode(code);
    }

    public List<Position6Dz> getPosition6DzsByPrefix(String prefix){
        return position6DzRepository.findAllByPrefix(prefix);
    }

    public Iterable<Object> findByCodePosition4(String code) {
        return null;
    }
}