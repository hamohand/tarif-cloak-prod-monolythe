package com.muhend.backend.pricing.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * Entité représentant une demande de devis pour un plan tarifaire personnalisé.
 */
@Entity
@Table(name = "quote_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class QuoteRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    
    @Column(name = "contact_name", nullable = false, length = 200)
    private String contactName;
    
    @Column(name = "contact_email", nullable = false, length = 255)
    private String contactEmail;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message; // Description des besoins, volume estimé, etc.
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private QuoteStatus status = QuoteStatus.PENDING;
    
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes; // Notes internes pour l'administration
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = QuoteStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (status == QuoteStatus.RESPONDED && respondedAt == null) {
            respondedAt = LocalDateTime.now();
        }
    }
    
    public enum QuoteStatus {
        PENDING,    // En attente de traitement
        IN_REVIEW,  // En cours d'examen
        RESPONDED,  // Répondu (devis créé ou refusé)
        CLOSED      // Fermé
    }
}

