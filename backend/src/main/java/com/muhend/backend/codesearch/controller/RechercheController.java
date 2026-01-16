package com.muhend.backend.codesearch.controller;

import com.muhend.backend.codesearch.model.*;
import com.muhend.backend.codesearch.service.ChapitreService;
import com.muhend.backend.codesearch.service.Position4Service;
import com.muhend.backend.codesearch.service.Position6DzService;
import com.muhend.backend.codesearch.service.SectionService;
import com.muhend.backend.codesearch.service.ai.AiPrompts;
import com.muhend.backend.codesearch.service.ai.AiService;
import com.muhend.backend.codesearch.service.ai.OpenAiService;
import com.muhend.backend.usage.service.UsageLogService;
import com.muhend.backend.organization.service.OrganizationService;
import com.muhend.backend.organization.dto.OrganizationDto;
import com.muhend.backend.organization.dto.QuotaCheckResult;
import com.muhend.backend.organization.exception.UserNotAssociatedException;
import com.muhend.backend.pricing.dto.PricingPlanDto;
import com.muhend.backend.pricing.service.PricingPlanService;
import java.math.BigDecimal;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
@RestController
@Data
@Slf4j
// --- IMPORTANT --- *******************************************************************
// On supprime "/api" du mapping, car Traefik le gère déjà.
// Spring ne verra que le chemin "/recherche".
// ***********************************************************************************
//@RequestMapping("/api/recherche")
@RequestMapping("/recherche") // pour Traefik

public class RechercheController {
    private final AiService aiService;
    private final AiPrompts aiPrompts;
    private final SectionService sectionService;
    private final ChapitreService chapitreService;
    private final Position4Service position4Service;
    private final Position6DzService position6DzService;
    private final UsageLogService usageLogService;
    private final OrganizationService organizationService;
    private final PricingPlanService pricingPlanService;
    
    // ThreadLocal pour stocker le résultat de la vérification du quota pour la requête courante
    private static final ThreadLocal<QuotaCheckResult> currentQuotaCheck = new ThreadLocal<>();

    @Autowired
    public RechercheController(AiService aiService, AiPrompts aiPrompts, SectionService sectionService, ChapitreService chapitreService,
                               Position4Service position4Service, Position6DzService position6DzService,
                               UsageLogService usageLogService, OrganizationService organizationService,
                               PricingPlanService pricingPlanService) {
        this.aiService = aiService;
        this.aiPrompts = aiPrompts;
        this.sectionService = sectionService;
        this.chapitreService = chapitreService;
        this.position4Service = position4Service;
        this.position6DzService = position6DzService;
        this.usageLogService = usageLogService;
        this.organizationService = organizationService;
        this.pricingPlanService = pricingPlanService;
    }

    // Enumération des différents niveaux de recherche
    private enum SearchLevel {
        SECTIONS, CHAPITRES, POSITIONS4, POSITIONS6
    }

    //****************************************************************************************
    // --------------------------------- ENDPOINTS DE RECHERCHE -----------------------------
    //****************************************************************************************

    // Niveau de recherche 0 : sections
    @GetMapping(value = "/sections", produces = "application/json")
    public List<Position> reponseSections(@RequestParam String termeRecherche) {
        boolean searchExecuted = false;
        try {
            // Vérifier le quota avant de faire la recherche
            checkQuotaBeforeSearch();
            List<Position> result = handleSearchRequest(termeRecherche, SearchLevel.SECTIONS);
            searchExecuted = true;
            return result;
        } catch (com.muhend.backend.organization.exception.QuotaExceededException e) {
            // Ne pas logger si le quota est dépassé (recherche non effectuée)
            throw e;
        } finally {
            // Nettoyer le ThreadLocal et logger seulement si la recherche a été effectuée
            if (searchExecuted) {
                logUsage("/recherche/sections", termeRecherche);
            }
            OpenAiService.clearCurrentUsage(); // Nettoyage de sécurité
            clearCurrentQuotaCheck(); // Nettoyer aussi le quota check
        }
    }

    // Niveau de recherche 1 : chapitres
    @GetMapping(path = "/chapitres", produces = "application/json")
    public List<Position> reponseChapitres(@RequestParam String termeRecherche) {
        boolean searchExecuted = false;
        try {
            // Vérifier le quota avant de faire la recherche
            checkQuotaBeforeSearch();
            List<Position> result = handleSearchRequest(termeRecherche, SearchLevel.CHAPITRES);
            searchExecuted = true;
            return result;
        } catch (com.muhend.backend.organization.exception.QuotaExceededException e) {
            // Ne pas logger si le quota est dépassé (recherche non effectuée)
            throw e;
        } finally {
            // Nettoyer le ThreadLocal et logger seulement si la recherche a été effectuée
            if (searchExecuted) {
                logUsage("/recherche/chapitres", termeRecherche);
            }
            OpenAiService.clearCurrentUsage(); // Nettoyage de sécurité
            clearCurrentQuotaCheck(); // Nettoyer aussi le quota check
        }
    }

    // Niveau de recherche 2 : positions 4
    @GetMapping(path = "/positions4", produces = "application/json")
    public List<Position> reponsePositions4(@RequestParam String termeRecherche) {
        boolean searchExecuted = false;
        try {
            // Vérifier le quota avant de faire la recherche
            checkQuotaBeforeSearch();
            List<Position> result = handleSearchRequest(termeRecherche, SearchLevel.POSITIONS4);
            searchExecuted = true;
            return result;
        } catch (com.muhend.backend.organization.exception.QuotaExceededException e) {
            // Ne pas logger si le quota est dépassé (recherche non effectuée)
            throw e;
        } finally {
            // Nettoyer le ThreadLocal et logger seulement si la recherche a été effectuée
            if (searchExecuted) {
                logUsage("/recherche/positions4", termeRecherche);
            }
            OpenAiService.clearCurrentUsage(); // Nettoyage de sécurité
        }
    }

    // Niveau de recherche 3 : positions 6
    @GetMapping(path = "/positions6", produces = "application/json")
    public List<Position> reponsePositions6(@RequestParam String termeRecherche) {
        System.out.println("=== Requête reçue sur /positions6 ==="); // Log de base
        System.out.println("Terme de recherche: " + termeRecherche);

        boolean searchExecuted = false;
        try {
            // Vérifier le quota avant de faire la recherche (peut lever QuotaExceededException)
            checkQuotaBeforeSearch();
            
            List<Position> result = handleSearchRequest(termeRecherche, SearchLevel.POSITIONS6);
            System.out.println("[CONTROLLER] handleSearchRequest a retourné: " + (result == null ? "null" : result.size() + " éléments"));

            if (result == null) {
                System.out.println("[CONTROLLER] ATTENTION: Résultat null, conversion en liste vide.");
                result = new ArrayList<>();
            }
            searchExecuted = true;
            return result;
        } catch (com.muhend.backend.organization.exception.QuotaExceededException e) {
            // Ne pas logger si le quota est dépassé (recherche non effectuée)
            throw e;
        } catch (Exception e) {
            System.err.println("[CONTROLLER] ERREUR INATTENDUE: " + e.getMessage());
            e.printStackTrace();
            log.error("Erreur lors de la recherche positions6", e);
            // En cas d'erreur, renvoyer une liste vide pour éviter de casser le frontend
            // Note: on ne marque pas searchExecuted = true car la recherche a échoué
            return new ArrayList<>();
        } finally {
            // Nettoyer le ThreadLocal et logger seulement si la recherche a été effectuée avec succès
            if (searchExecuted) {
                logUsage("/recherche/positions6", termeRecherche);
            }
            OpenAiService.clearCurrentUsage(); // Nettoyage de sécurité
            clearCurrentQuotaCheck(); // Nettoyer aussi le quota check
        }
    }
    
    /**
     * Log l'utilisation d'une recherche.
     * Récupère les informations de coût depuis OpenAiService et enregistre le log.
     * Cette méthode est complètement non-bloquante et ne doit jamais faire échouer la requête principale.
     */
    private void logUsage(String endpoint, String searchTerm) {
        try {
            // Récupérer l'utilisateur depuis le contexte de sécurité
            String userId = getCurrentUserId();
            if (userId == null) {
                log.debug("Impossible de récupérer l'utilisateur pour le logging. Utilisation non enregistrée.");
                return;
            }
            
            // Récupérer l'organisation de l'utilisateur (obligatoire)
            // Si l'utilisateur n'a pas d'organisation, on ne peut pas logger l'utilisation
            Long organizationId;
            try {
                organizationId = organizationService.getOrganizationIdByUserId(userId);
            } catch (UserNotAssociatedException e) {
                log.warn("Utilisateur {} non associé à une organisation. Logging non effectué.", userId);
                return;
            } catch (Exception e) {
                // En cas d'erreur inattendue, on ne bloque pas mais on ne log pas
                log.warn("Erreur lors de la récupération de l'organisation pour l'utilisateur {}: {}", userId, e.getMessage());
                return;
            }
            
            // Récupérer les informations d'utilisation depuis OpenAiService
            UsageInfo usageInfo = OpenAiService.getCurrentUsage();
            if (usageInfo != null && usageInfo.getTokens() != null && usageInfo.getTokens() > 0) {
                // Récupérer l'organisation pour déterminer le type de plan
                OrganizationDto organization = organizationService.getOrganizationById(organizationId);
                QuotaCheckResult quotaResult = getCurrentQuotaCheck();
                
                // Déterminer le coût selon la politique de facturation
                Double costToUse = null; // Par défaut : pas de facturation
                String billingReason = "plan mensuel (facturation mensuelle fixe)";
                
                if (organization != null && organization.getPricingPlanId() != null) {
                    try {
                        PricingPlanDto plan = pricingPlanService.getPricingPlanById(organization.getPricingPlanId());
                        boolean hasPricePerRequest = plan.getPricePerRequest() != null && plan.getPricePerRequest().compareTo(BigDecimal.ZERO) > 0;
                        boolean hasPricePerMonth = plan.getPricePerMonth() != null && plan.getPricePerMonth().compareTo(BigDecimal.ZERO) > 0;
                        boolean isPayPerRequest = hasPricePerRequest && !hasPricePerMonth;
                        boolean isMonthlyPlan = hasPricePerMonth && !hasPricePerRequest;
                        
                        if (isPayPerRequest) {
                            // Plan pay-per-request : facturer chaque requête avec le prix du plan dans sa monnaie
                            costToUse = plan.getPricePerRequest().doubleValue();
                            billingReason = String.format("plan pay-per-request (%s %s)", 
                                    plan.getPricePerRequest(), plan.getCurrency() != null ? plan.getCurrency() : "EUR");
                            log.debug("💰 Facturation par requête pour plan pay-per-request: {} {}", 
                                    costToUse, plan.getCurrency() != null ? plan.getCurrency() : "EUR");
                        } else if (isMonthlyPlan) {
                            // Plan mensuel : pas de facturation par requête SAUF si quota dépassé
                            if (quotaResult != null && !quotaResult.isQuotaOk() && quotaResult.isCanUsePayPerRequest()) {
                                // Quota dépassé : facturer au prix Pay-per-Request du plan correspondant au marché
                                if (quotaResult.getPayPerRequestPrice() != null) {
                                    costToUse = quotaResult.getPayPerRequestPrice().doubleValue();
                                    billingReason = "quota mensuel dépassé (facturation pay-per-request)";
                                    log.info("💰 Requête facturée au prix Pay-per-Request (quota dépassé): {} au lieu de {}", 
                                            costToUse, usageInfo.getCostUsd());
                                } else {
                                    log.warn("⚠️ Quota dépassé mais prix Pay-per-Request non disponible, pas de facturation");
                                }
                            } else {
                                // Plan mensuel normal : pas de facturation par requête
                                costToUse = null;
                                log.debug("✅ Plan mensuel : pas de facturation par requête (facturation mensuelle fixe)");
                            }
                        } else {
                            // Plan gratuit ou mixte : pas de facturation
                            costToUse = null;
                            billingReason = "plan gratuit ou mixte";
                            log.debug("Plan gratuit ou mixte : pas de facturation");
                        }
                    } catch (Exception e) {
                        log.warn("Erreur lors de la récupération du plan {} pour déterminer la facturation: {}", 
                                organization.getPricingPlanId(), e.getMessage());
                        // En cas d'erreur, ne pas facturer pour éviter les erreurs
                        costToUse = null;
                    }
                } else {
                    // Pas de plan : pas de facturation
                    costToUse = null;
                    billingReason = "pas de plan tarifaire";
                    log.debug("Organisation sans plan tarifaire : pas de facturation");
                }
                
                // Le service logUsage est déjà non-bloquant, on peut l'appeler sans try-catch
                usageLogService.logUsage(
                    userId,
                    organizationId,
                    endpoint,
                    searchTerm,
                    usageInfo.getTokens(),
                    costToUse
                );
                log.debug("Enregistrement du log: userId={}, organizationId={}, endpoint={}, tokens={}, cost={} ({})", 
                         userId, organizationId, endpoint, usageInfo.getTokens(), 
                         costToUse != null ? costToUse : "0 (non facturé)", billingReason);
            } else {
                log.debug("Aucune information d'utilisation disponible pour l'endpoint: {} (usageInfo={})", 
                         endpoint, usageInfo != null ? "présent mais tokens=0 ou null" : "null");
            }
            
            // Nettoyer le ThreadLocal après utilisation
            clearCurrentQuotaCheck();
        } catch (Exception e) {
            // Double sécurité : ne jamais faire échouer la requête si le logging échoue
            log.warn("Erreur lors du logging de l'utilisation (non bloquant): {}", e.getMessage());
        }
    }
    
    /**
     * Récupère l'ID de l'utilisateur Keycloak depuis le contexte de sécurité.
     * @return L'ID de l'utilisateur (sub du JWT) ou null si non disponible
     */
    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                return jwt.getClaimAsString("sub");
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'ID utilisateur", e);
        }
        return null;
    }
    
    /**
     * Vérifie le quota de l'organisation de l'utilisateur avant d'effectuer une recherche.
     * Phase 4 MVP : Quotas Basiques
     * Un utilisateur DOIT être associé à une organisation pour effectuer des recherches.
     * Vérifie aussi si l'essai gratuit est expiré.
     */
    private void checkQuotaBeforeSearch() {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                throw new IllegalStateException("Impossible de récupérer l'utilisateur pour la vérification du quota. Recherche non autorisée.");
            }
            
            // EXIGER une organisation (lève une exception si pas d'organisation)
            Long organizationId = organizationService.getOrganizationIdByUserId(userId);
            
            // Vérifier si l'essai est expiré (quota atteint pour un plan d'essai)
            // Si le quota de l'essai gratuit est atteint, l'essai est définitivement terminé
            // et aucune requête n'est autorisée pour tous les collaborateurs de l'organisation
            if (!organizationService.canOrganizationMakeRequests(organizationId)) {
                // Vérifier si c'est parce que l'organisation est désactivée ou le quota atteint
                try {
                    OrganizationDto organization = organizationService.getOrganizationById(organizationId);
                    if (organization != null) {
                        // Vérifier si l'organisation est désactivée par un administrateur
                        if (!Boolean.TRUE.equals(organization.getEnabled())) {
                            throw new IllegalStateException(
                                "Votre organisation a été désactivée par un administrateur. " +
                                "Aucune requête HS-code n'est autorisée pour tous les collaborateurs. " +
                                "Veuillez contacter l'administrateur pour plus d'informations."
                            );
                        }
                        // Vérifier si c'est parce que le quota est atteint et définitivement terminé
                        if (Boolean.TRUE.equals(organization.getTrialPermanentlyExpired())) {
                            throw new IllegalStateException(
                                "Le quota de votre essai gratuit a été atteint et est maintenant définitivement désactivé pour votre organisation. " +
                                "Aucune requête HS-code n'est autorisée pour tous les collaborateurs. " +
                                "Veuillez choisir un plan tarifaire ou faire une demande de devis pour continuer à utiliser le service."
                            );
                        }
                    }
                } catch (IllegalStateException e) {
                    // Relancer l'exception si c'est déjà notre message personnalisé
                    throw e;
                } catch (Exception e) {
                    // Si l'organisation n'est pas trouvée ou autre erreur, utiliser le message générique
                    log.debug("Erreur lors de la vérification du statut de l'organisation: {}", e.getMessage());
                }
                throw new IllegalStateException(
                    "Votre période d'essai gratuit est terminée. Veuillez choisir un plan tarifaire ou faire une demande de devis pour continuer à utiliser le service."
                );
            }
            
            // Vérifier le quota avec résultat détaillé (ne lève plus d'exception si dépassé)
            QuotaCheckResult quotaResult = organizationService.checkQuotaWithResult(organizationId);
            
            // Stocker le résultat dans ThreadLocal pour utilisation dans logUsage()
            currentQuotaCheck.set(quotaResult);
            
            // Si le quota est dépassé mais qu'on peut utiliser Pay-per-Request, permettre la requête
            if (!quotaResult.isQuotaOk() && quotaResult.isCanUsePayPerRequest()) {
                log.info("⚠️ Quota dépassé pour l'organisation {} (ID: {}): {}/{} requêtes. " +
                        "La requête sera facturée au prix Pay-per-Request: {}",
                        organizationId, quotaResult.getCurrentUsage(), quotaResult.getMonthlyQuota(),
                        quotaResult.getPayPerRequestPrice() != null ? quotaResult.getPayPerRequestPrice() : "tarif de base");
                // Permettre la requête, elle sera facturée au prix Pay-per-Request
            } else if (!quotaResult.isQuotaOk() && !quotaResult.isCanUsePayPerRequest()) {
                // Quota dépassé et pas de plan Pay-per-Request disponible - bloquer la requête
                String message = String.format(
                        "Quota mensuel dépassé pour votre organisation. Utilisation: %d/%d requêtes. " +
                        "Aucun plan Pay-per-Request disponible pour votre marché.",
                        quotaResult.getCurrentUsage(), quotaResult.getMonthlyQuota());
                log.warn("❌ {}", message);
                currentQuotaCheck.remove();
                throw new com.muhend.backend.organization.exception.QuotaExceededException(message);
            } else {
                // Quota OK
                log.debug("✅ Quota OK pour l'organisation {}: {}/{} requêtes", 
                        organizationId, quotaResult.getCurrentUsage(), quotaResult.getMonthlyQuota());
            }
            
        } catch (UserNotAssociatedException e) {
            // Un utilisateur doit être associé à une organisation
            currentQuotaCheck.remove();
            throw new IllegalStateException("Vous devez être associé à une organisation pour effectuer des recherches.", e);
        } catch (com.muhend.backend.organization.exception.QuotaExceededException e) {
            // Relancer l'exception pour qu'elle soit gérée par le gestionnaire d'exceptions global
            currentQuotaCheck.remove();
            throw e;
        } catch (IllegalArgumentException e) {
            // Erreur lors de la vérification du quota (organisation introuvable, etc.)
            currentQuotaCheck.remove();
            throw new IllegalStateException("Impossible de vérifier le quota. Recherche non autorisée.", e);
        } catch (Exception e) {
            // En cas d'erreur inattendue, on bloque la recherche pour la sécurité
            currentQuotaCheck.remove();
            log.error("Erreur inattendue lors de la vérification du quota: {}", e.getMessage(), e);
            throw new IllegalStateException("Erreur lors de la vérification du quota. Recherche non autorisée.", e);
        }
    }
    
    /**
     * Récupère le résultat de la vérification du quota pour la requête courante.
     * @return QuotaCheckResult ou null si non disponible
     */
    public static QuotaCheckResult getCurrentQuotaCheck() {
        return currentQuotaCheck.get();
    }
    
    /**
     * Nettoie le ThreadLocal du quota check.
     */
    public static void clearCurrentQuotaCheck() {
        currentQuotaCheck.remove();
    }


    //****************************************************************************************
    // --------------------------------- LOGIQUE DE RECHERCHE EN CASCADE --------------------
    //****************************************************************************************

    private List<Position> handleSearchRequest(String termeRecherche, SearchLevel maxLevel) {
        System.out.println("[HANDLER] --- Début de la recherche en cascade pour '" + termeRecherche + "' (maxLevel: " + maxLevel + ") ---");
        List<Position> reponseList = new ArrayList<>();
        List<Position> positions = new ArrayList<>();
        List<Position> reponseListLevel = new ArrayList<>();
        List<Position> ragNiveau;
        int tentativesMax = 2;

        // --------------------------- Level 0 : Sections ---------------------------------------
        ragNiveau = ragSections();
        System.out.println("[HANDLER] Level 0 (Sections) - Taille du RAG: " + ragNiveau.size());

        int nbTentatives = 0;
        do {
            nbTentatives++;
            System.out.println("[HANDLER] Level 0 -> Tentative " + nbTentatives + "/" + tentativesMax);
            positions = aiService.promptEtReponse(SearchLevel.SECTIONS.toString(), termeRecherche, ragNiveau);
        } while (nbTentatives < tentativesMax && positions.isEmpty());

        System.out.println("[HANDLER] Level 0 -> Résultat de l'IA: " + (positions != null ? positions.size() : "null") + " élément(s)");
        if (positions == null || positions.isEmpty()) {
            System.out.println("[HANDLER] Level 0 -> Aucun résultat. Arrêt de la cascade et retour liste vide.");
            return new ArrayList<>();
        }

        // Description
        if (aiPrompts.defTheme.isWithDescription()) { // affichage avec les descriptions
            for (Position position : positions) {
                String code = position.getCode();
                String description = sectionService.getDescription(code.trim());
                position.setDescription(description);
            }
        }
        // Résultat du niveau
        reponseListLevel.addAll(positions);
        // Cascade
        if (aiPrompts.defTheme.isWithCascade()) { // ajout du niveau au résultat général
            reponseList.addAll(reponseListLevel);
        }
        // si niveau demandé
        if (maxLevel == SearchLevel.SECTIONS) {
            if (!aiPrompts.defTheme.isWithCascade()) { // reponseList contiendra le résultat du niveau courant uniquement
                return reponseListLevel;
            } else {
                return reponseList;
            }
        }

        // ----------------------------- Level 1: Chapitres ----------------------------------------
        reponseListLevel.clear();
        ragNiveau = ragChapitres(positions);
        System.out.println("[HANDLER] Level 1 (Chapitres) - Taille du RAG: " + ragNiveau.size());

        nbTentatives = 0;
        do {
            nbTentatives++;
            System.out.println("[HANDLER] Level 1 -> Tentative " + nbTentatives + "/" + tentativesMax);

            positions = aiService.promptEtReponse(SearchLevel.CHAPITRES.toString(), termeRecherche, ragNiveau);

        } while (nbTentatives < tentativesMax && positions.isEmpty());

        System.out.println("[HANDLER] Level 1 -> Résultat de l'IA: " + (positions != null ? positions.size() : "null") + " élément(s)");
        if (positions == null || positions.isEmpty()) {
            System.out.println("[HANDLER] Level 1 -> Aucun résultat. Arrêt de la cascade et retour liste vide.");
            return new ArrayList<>();
        }

        // Description
        if (aiPrompts.defTheme.isWithDescription()) { // affichage avec les descriptions
            for (Position position : positions) {
                String code = position.getCode();
                String description = chapitreService.getDescription(code);
                position.setDescription(description);
            }
        }
        // Résultat du niveau
        reponseListLevel.addAll(positions);
        // Cascade
        if (aiPrompts.defTheme.isWithCascade()) { // ajout du niveau au résultat général
            reponseList.addAll(reponseListLevel);
        }
        // Si niveau demandé
        if (maxLevel == SearchLevel.CHAPITRES) {
            if (!aiPrompts.defTheme.isWithCascade()) { // reponseList contiendra le résultat du niveau courant uniquement
                return reponseListLevel;
            } else {
                return reponseList;
            }
        }

        // ------------------------------- Level 2 : Positions 4 -------------------------------------------------
        reponseListLevel.clear();
        ragNiveau = ragPositions4(positions);
        System.out.println("[HANDLER] Level 2 (Positions4) - Taille du RAG: " + ragNiveau.size());

        nbTentatives = 0;
        do {
            nbTentatives++;
            System.out.println("[HANDLER] Level 2 -> Tentative " + nbTentatives + "/" + tentativesMax);

                positions = aiService.promptEtReponse(SearchLevel.POSITIONS4.toString(), termeRecherche, ragNiveau);

        } while (nbTentatives < tentativesMax && positions.isEmpty());

        List<Position> positionsPositions4 = positions;
        System.out.println("[HANDLER] Level 2 -> Résultat de l'IA: " + (positions != null ? positions.size() : "null") + " élément(s)");

        if (positions == null || positions.isEmpty()) {
            System.out.println("[HANDLER] Level 2 -> Aucun résultat. Arrêt de la cascade et retour liste vide.");
            return new ArrayList<>();
        }

        // Description
        if (aiPrompts.defTheme.isWithDescription()) { // ajout des descriptions
            for (Position position : positions) {
                String code = position.getCode();
                String description = position4Service.getDescription(code);
                position.setDescription(description);
            }
        }
        // Résultat du niveau
        reponseListLevel.addAll(positions);
        // Cascade
        if (aiPrompts.defTheme.isWithCascade()) { // ajout du niveau au résultat général
            reponseList.addAll(reponseListLevel);
        }
        // si niveau demandé
        if (maxLevel == SearchLevel.POSITIONS4) {
            if (!aiPrompts.defTheme.isWithCascade()) { // reponseList contiendra affichage du niveau courant uniquement
                return reponseListLevel;
            }
            return reponseList;
        }

        // ------------------------------- Level 3 : Positions 6 - le plus haut pour le moment-------------------------------------------------
        reponseListLevel.clear();
        ragNiveau = ragPositions6(positions);
        System.out.println("[HANDLER] Level 3 (Positions6) - Taille du RAG: " + ragNiveau.size());

        nbTentatives = 0;
        do {
            nbTentatives++;
            System.out.println("[HANDLER] Level 3 -> Tentative " + nbTentatives + "/" + tentativesMax);

                positions = aiService.promptEtReponse(SearchLevel.POSITIONS6.toString(), termeRecherche, ragNiveau);

        } while (nbTentatives < tentativesMax && positions.isEmpty());

        //List<Position> positionsPositions6Dz = positions;
        System.out.println("[HANDLER] Level 3 -> Résultat de l'IA: " + (positions != null ? positions.size() : "null") + " élément(s)");

        if (positions == null || positions.isEmpty()) {
            System.out.println("[HANDLER] Level 3 -> Aucun résultat au niveau 6.");
            if (!positionsPositions4.isEmpty()) {
                System.out.println("[HANDLER] Level 3 -> Utilisation des résultats de Level 2 (Positions4): " + positionsPositions4.size() + " élément(s)");
                positions = positionsPositions4;
            } else {
                System.out.println("[HANDLER] Level 3 -> Aucun résultat aux niveaux 2 et 3. Retour null.");
                return new ArrayList<>();
            }
        }
        // Description
        if (aiPrompts.defTheme.isWithDescription()) { // ajout des descriptions
            for (Position position : positions) {
                String code = position.getCode();
                String description = position6DzService.getDescription(code);
                position.setDescription(description);
            }
        }
        // Résultat du niveau
        reponseListLevel.addAll(positions);
        // Cascade
        if (aiPrompts.defTheme.isWithCascade()) { // ajout du niveau au résultat général
            reponseList.addAll(reponseListLevel);
        }
        // si niveau demandé
        if (maxLevel == SearchLevel.POSITIONS6) {
            if (!aiPrompts.defTheme.isWithCascade()) {
                System.out.println("[HANDLER] --- Fin recherche (sans cascade). Retour: " + reponseListLevel.size() + " élément(s) ---");
                return reponseListLevel;
            }
            System.out.println("[HANDLER] --- Fin recherche (avec cascade). Retour: " + reponseList.size() + " élément(s) ---");
            return reponseList;
        }

        // Réponse genérale
        System.out.println("[HANDLER] --- Fin recherche générale. Retour: " + reponseList.size() + " élément(s) ---");
        return reponseList;
    }


    //****************************************************************************************
    // --------------------------------- GÉNÉRATION DU CONTEXTE (RAG) -----------------------
    //****************************************************************************************

    /**
     * Crée le contexte (RAG) pour la recherche de CHAPITRES en listant toutes les sections disponibles.
     *
     * @return Une liste de Positions contenant les sections.
     */
    private List<Position> ragSections() {
        List<Section> results = sectionService.getAllSections();
        return results.stream()
                .map(section -> new Position(section.getCode(), section.getDescription()))
                .collect(Collectors.toList());
    }

    private List<Position> ragChapitres(List<Position> listePositions) {
        if (listePositions != null && !listePositions.isEmpty()) {
            return listePositions.stream()
                    .flatMap(position -> chapitreService.getChapitresBySection(position.getCode()).stream())
                    .map(chapitre -> new Position(chapitre.getCode(), chapitre.getDescription()))
                    .collect(Collectors.toList());
        } else { // si la liste des sections condidates est vide, RAG = liste de tous les chapitres
            List<Chapitre> results = chapitreService.getAllChapitres();
            return results.stream()
                    .map(chapitre -> new Position(chapitre.getCode(), chapitre.getDescription()))
                    .collect(Collectors.toList());
        }
    }

    private List<Position> ragPositions4(List<Position> listePositions) {
        return listePositions.stream()
                .flatMap(position -> {
                    String chapterCodePrefix = position.getCode() + "%";
                    return position4Service.getPosition4sByPrefix(chapterCodePrefix).stream();
                })
                .map(pos4 -> new Position(pos4.getCode(), pos4.getDescription()))
                .collect(Collectors.toList());
    }

    private List<Position> ragPositions6(List<Position> listePositions) {
        return listePositions.stream()
                .flatMap(position -> {
                    String position4CodePrefix = position.getCode() + "%";
                    return position6DzService.getPosition6DzsByPrefix(position4CodePrefix).stream();
                })
                .map(pos6 -> new Position(pos6.getCode(), pos6.getDescription()))
                .collect(Collectors.toList());
    }
}
