package com.muhend.backend.codesearch.controller;


import com.muhend.backend.codesearch.model.Position4;
import com.muhend.backend.codesearch.repository.Position4Repository;
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
//@RequestMapping("api/positions4")
@RequestMapping("/positions4")// prod
public class Position4Controller {

    private final Position4Repository position4Repository;

    public Position4Controller(Position4Repository position4Repository) {
        this.position4Repository = position4Repository;
    }

    // Get all positions
    @GetMapping
    public List<Position4> getAllPositions() {
        return position4Repository.findAll();
    }

    // Get a position by id
    @GetMapping("/{id}")
    public ResponseEntity<Position4> getPositionById(@PathVariable Long id) {
        Optional<Position4> position = position4Repository.findById(id);
        return position.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Get a position by code
    @GetMapping("/code/{code}")
    public ResponseEntity<Position4> getPositionByCode(@PathVariable String code) {
        Optional<Position4> position = position4Repository.findByCode(code);
        return position.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Create a new position
    @PostMapping
    public ResponseEntity<Position4> createPosition(@RequestBody Position4 position) {
        Position4 savedPosition = position4Repository.save(position);
        return new ResponseEntity<>(savedPosition, HttpStatus.CREATED);
    }

    // Update a position
    @PutMapping("/{id}")
    public ResponseEntity<Position4> updatePosition(@PathVariable Long id, @RequestBody Position4 positionDetails) {
        return position4Repository.findById(id)
                .map(existingPosition -> {
                    existingPosition.setCode(positionDetails.getCode());
                    existingPosition.setDescription(positionDetails.getDescription());
                    Position4 updatedPosition = position4Repository.save(existingPosition);
                    return ResponseEntity.ok(updatedPosition);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Delete a position
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePosition(@PathVariable Long id) {
        return position4Repository.findById(id)
                .map(position -> {
                    position4Repository.delete(position);
                    return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
