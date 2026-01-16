package com.muhend.backend.codesearch.z_outils;

import lombok.Builder;
import lombok.Value;

/**
 * Définit les options d'affichage pour les résultats de recherche de codes douaniers.
 * Cette classe est immuable et utilise le pattern Builder pour sa construction.
 */
@Value
@Builder(toBuilder = true)
public class DefTheme {
    /**
     * Si vrai, inclut le code douanier dans le résultat.
     */
    @Builder.Default
    boolean withCode = true;

    /**
     * // ------------------ Choix du thème de la structure d'affichage ----------------------------
     * Si vrai, inclut une justification pour chaque code douanier suggéré.
     */
    @Builder.Default
    boolean withJustification = false;

    /**
     * Si vrai, affiche la cascade complète des résultats.
     * Sinon, affiche uniquement le résultat de la position demandée.
     */
    @Builder.Default
    boolean withCascade = false;

    /**
     * Si vrai, affiche uniquement le tableau des codes douaniers sans autres informations.
     */
    @Builder.Default
    boolean onlyCodes = false;

    /**
     * Si vrai, inclut les descriptions des codes douaniers dans le résultat.
     */
    @Builder.Default
    boolean withDescription = false;

    /**
     * Crée un thème d'affichage par défaut.
     * <p>
     * Thème configuré pour inclure toutes les informations.
     *
     * @return une nouvelle instance du thème par défaut.
     */
    //description, justification, en cascade
    public static DefTheme getThemeAll() {
        return DefTheme.builder()
                .withDescription(true)
                .withJustification(true)
                .withCascade(true)
                .build();
    }

    //Codes uniquement
    public static DefTheme getCode() {
        return DefTheme.builder()
                .build();
    }

    //description, justification
    public static DefTheme getThemeDescripJustif() {
        return DefTheme.builder()
                .withDescription(true)
                .withJustification(true)
                .build();
    }

    /**
     * Crée un thème d'affichage par défaut.
     * <p>
     * Thème configuré pour inclure les justifications et la cascade des résultats.
     *
     * @return une nouvelle instance du thème par défaut.
     */
    //justification, en cascade
    public static DefTheme getThemeJustifCascade() {
        return DefTheme.builder()
                .withJustification(true)
                .withCascade(true)
                .build();
    }
    //justification
    public static DefTheme getThemeJustif() {
        return DefTheme.builder()
                .withJustification(true)
                .build();
    }

    //description, en cascade
    public static DefTheme getThemeDescripCascade() {
        return DefTheme.builder()
                .withDescription(true)
                .withCascade(true)
                .build();
    }
    //description
    public static DefTheme getThemeDescrip() {
        return DefTheme.builder()
                .withDescription(true)
                .build();
    }


}
