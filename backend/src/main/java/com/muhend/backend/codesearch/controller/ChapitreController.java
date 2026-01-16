package com.muhend.backend.codesearch.controller;

import com.muhend.backend.codesearch.model.Chapitre;
import com.muhend.backend.codesearch.repository.ChapitreRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
// --- IMPORTANT --- *******************************************************************
// Pour le déploiement, on supprime "/api" du mapping, car Traefik le gère déjà.
// Spring ne verra que le chemin "/positions6dz".
// Modifier @RequestMapping("/api/positions6dz")
// ***********************************************************************************
// @RequestMapping("/api/chapitres") // dev
@RequestMapping("/chapitres")  // prod
public class ChapitreController {

    private final ChapitreRepository chapitreRepository;

    public ChapitreController(ChapitreRepository chapitreRepository) {
        this.chapitreRepository = chapitreRepository;
    }

    // Get all chapitres
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")  // ← Seulement cette méthode nécessite ADMIN
    public List<Chapitre> getAllChapitres() {
        return chapitreRepository.findAll();
    }

    // Get a chapitre by id
    @GetMapping("/{id}")
    public ResponseEntity<Chapitre> getChapitreById(@PathVariable Long id) {
        Optional<Chapitre> chapitre = chapitreRepository.findById(id);
        return chapitre.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Get a chapitre by code
    @Operation(
            summary = "Rechercher un chapitre par son code",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Chapitre trouvé", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "Chapitre non trouvé", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content(mediaType = "application/json"))
            }
    )

    @GetMapping("/code/{code}")
    public ResponseEntity<Chapitre> rechercherChapitreParCode(@PathVariable String code) {
        // return convertirEnResponseEntity(chapitreRepository.findByCodeTrimmed(code));
        // Validation de l'entrée
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        // Recherche du chapitre
        try {
            code = code.trim(); // Nettoyage côté utilisateur
            Optional<Chapitre> chapitre = chapitreRepository.findByCode(code);
            //Optional<Chapitre> chapitre = chapitreRepository.findByCodeTrimmed(code);
            // Transformation en réponse appropriée
            return chapitre.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception ex) {
            // Gestion des erreurs inattendues
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }

    }

    private ResponseEntity<Chapitre> convertirEnResponseEntity(Optional<Chapitre> chapitre) {
        return chapitre.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    // Create a new chapitre
    @PostMapping
    public ResponseEntity<Chapitre> createChapitre(@RequestBody Chapitre chapitre) {
        Chapitre savedChapitre = chapitreRepository.save(chapitre);
        return new ResponseEntity<>(savedChapitre, HttpStatus.CREATED);
    }

    // Update a chapitre
    @PutMapping("/{id}")
    public ResponseEntity<Chapitre> updateChapitre(@PathVariable Long id, @RequestBody Chapitre chapitreDetails) {
        return chapitreRepository.findById(id)
                .map(existingChapitre -> {
                    existingChapitre.setCode(chapitreDetails.getCode());
                    existingChapitre.setDescription(chapitreDetails.getDescription());
                    existingChapitre.setSection(chapitreDetails.getSection());
                    Chapitre updatedChapitre = chapitreRepository.save(existingChapitre);
                    return ResponseEntity.ok(updatedChapitre);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Delete a chapitre
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChapitre(@PathVariable Long id) {
        return chapitreRepository.findById(id)
                .map(chapitre -> {
                    chapitreRepository.delete(chapitre);
                    return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}

