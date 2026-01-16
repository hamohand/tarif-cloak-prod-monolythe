package com.muhend.backend.codesearch.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Chapitre {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String code; // numéro de chapitre
    
    @Column(nullable = false, length = 1024) // Limitation définie ici
    private String description;

    private String section; // Champ pour stocker le numéro de la section
}