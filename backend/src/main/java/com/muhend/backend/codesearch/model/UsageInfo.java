package com.muhend.backend.codesearch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Classe pour stocker les informations d'utilisation d'une requête OpenAI.
 * Utilisée pour transmettre les données de coût et de tokens depuis OpenAiService.
 * 
 * Nouvelle politique de tarification :
 * - costUsd : Prix de la requête = BASE_REQUEST_PRICE (dans la devise du marché)
 * - tokenCostUsd : Coût des tokens en USD (affiché uniquement aux administrateurs)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsageInfo {
    private Integer tokens;
    private Double costUsd; // Prix de la requête = BASE_REQUEST_PRICE (dans devise marché)
    private Integer promptTokens;
    private Integer completionTokens;
    private Double tokenCostUsd; // Coût des tokens en USD (pour les admins uniquement)
}

