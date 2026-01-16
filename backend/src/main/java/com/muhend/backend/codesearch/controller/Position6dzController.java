package com.muhend.backend.codesearch.controller;

import com.muhend.backend.codesearch.model.Position6Dz;
import com.muhend.backend.codesearch.repository.Position6DzRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
// --- IMPORTANT --- *******************************************************************
// Pour le déploiement, on supprime "/api" du mapping, car Traefik le gère déjà.
// Spring ne verra que le chemin "/positions6dz".
// Modifier @RequestMapping("/api/positions6dz")
// ***********************************************************************************
//@RequestMapping("api/positions6dz")
@RequestMapping("/positions6dz")// prod
public class Position6dzController {

    private final Position6DzRepository position6dzRepository;

    public Position6dzController(Position6DzRepository position6dzRepository) {
        this.position6dzRepository = position6dzRepository;
    }

    // Get all positions
    @GetMapping
    public List<Position6Dz> getAllPositions() {
        return position6dzRepository.findAll();
    }

    // Get a position by id
    @GetMapping("/{id}")
    public ResponseEntity<Position6Dz> getPositionById(@PathVariable Long id) {
        Optional<Position6Dz> position = position6dzRepository.findById(id);
        return position.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Get a position by code
    @GetMapping("/code/{code}")
    public ResponseEntity<Position6Dz> getPositionByCode(@PathVariable String code) {
        Optional<Position6Dz> position = position6dzRepository.findByCode(code);
        return position.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Create a new position
    @PostMapping
    public ResponseEntity<Position6Dz> createPosition(@RequestBody Position6Dz position) {
        Position6Dz savedPosition = position6dzRepository.save(position);
        return new ResponseEntity<>(savedPosition, HttpStatus.CREATED);
    }

    // Update a position
    @PutMapping("/{id}")
    public ResponseEntity<Position6Dz> updatePosition(@PathVariable Long id, @RequestBody Position6Dz positionDetails) {
        return position6dzRepository.findById(id)
                .map(existingPosition -> {
                    existingPosition.setCode(positionDetails.getCode());
                    existingPosition.setDescription(positionDetails.getDescription());
                    Position6Dz updatedPosition = position6dzRepository.save(existingPosition);
                    return ResponseEntity.ok(updatedPosition);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Delete a position
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePosition(@PathVariable Long id) {
        return position6dzRepository.findById(id)
                .map(position -> {
                    position6dzRepository.delete(position);
                    return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
