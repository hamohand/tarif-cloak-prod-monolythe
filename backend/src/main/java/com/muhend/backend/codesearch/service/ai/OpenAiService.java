package com.muhend.backend.codesearch.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muhend.backend.codesearch.model.UsageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class OpenAiService {
    private final AiPrompts aiPrompts;
    private final String aiKey;
    private double baseRequestPrice;

    public OpenAiService(AiPrompts aiPrompts, 
                        @Value("${OPENAI_API_KEY}") String aiKey,
                        @Value("${BASE_REQUEST_PRICE:0.01}") String baseRequestPriceStr) {
        this.aiPrompts = aiPrompts;
        this.aiKey = aiKey;
        // Nettoyer la valeur pour éviter les problèmes de concaténation dans le fichier .env
        try {
            log.info("Valeur brute de BASE_REQUEST_PRICE reçue: '{}'", baseRequestPriceStr);
            String cleaned = baseRequestPriceStr != null ? baseRequestPriceStr.trim() : "0.01";
            // Extraire seulement la partie numérique (avant tout caractère non numérique ou espace)
            cleaned = cleaned.split("\\s+")[0]; // Prendre le premier mot
            cleaned = cleaned.replaceAll("[^0-9.]", ""); // Enlever tout sauf chiffres et point
            if (cleaned.isEmpty() || cleaned.equals(".")) {
                cleaned = "0.01";
                log.warn("Valeur nettoyée vide ou invalide, utilisation de la valeur par défaut: {}", cleaned);
            }
            this.baseRequestPrice = Double.parseDouble(cleaned);
            log.info("✅ Tarif de base par requête configuré avec succès: {} (dans la devise du marché, valeur originale: '{}')", this.baseRequestPrice, baseRequestPriceStr);
        } catch (NumberFormatException e) {
            log.error("❌ Erreur lors du parsing de BASE_REQUEST_PRICE: '{}'. Utilisation de la valeur par défaut 0.01", baseRequestPriceStr, e);
            this.baseRequestPrice = 0.01;
        }
    }
    /**
     * Pour utiliser GPT-4o ou toute autre API d'OpenAI, vous devez d'abord intégrer leur SDK ou utiliser un client HTTP pour appeler l'API.
     */
//    @Value("${OPENAI_API_KEY}")
//    private String aiKey;

    private final String aiApiUrl = "https://api.openai.com/v1/chat/completions"; // URL corrigée
    //private static final String OPENAI_API_URL = "http://localhost:11434/completions\n"; // URL

    private final String aiModel = "gpt-4.1";
    //private final String aiModel = "gpt-4.1-mini";
    //   private final String OPENAI_MODEL = "llama3";
    private final int maxTokens = 500;
    private final float temperature = 0.0F;

    double prix_requete = 0.00;
    
    // ThreadLocal pour stocker les informations d'utilisation de la requête courante
    private static final ThreadLocal<UsageInfo> currentUsage = new ThreadLocal<>();

//    /// //////////////// Options pour le prompt et le résultat /////////////////////////////////////
//    // - message système: true avec justification false sans.
//    private final Boolean withJustification = false;
//    // - Afficher toute la cascade des messages (true) ou uniquement le résultat de la position demandée (false)
//    private final Boolean withCascade = false;
//    /// ///////////////////////////////////////////////////////////

    public String demanderAiAide(String titre, String question) {
        log.info("Clé API OpenAI chargée. Longueur2 : {}", aiKey.length());
        log.info("Clé API OpenAI chargée. valeur2 : {}", aiKey);
        //Préparation du client REST
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Bearer " + aiKey);
        httpHeaders.add("Content-Type", "application/json");

        // Construction robuste du corps JSON
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiModel); // Spécifiez le modèle
        requestBody.put("messages", new Object[]{
                Map.of("role", "system", "content", AiPrompts.getSystemMessage(aiPrompts.defTheme.isWithJustification())), // message système: true avec justification false sans
                Map.of("role", "user", "content", question)
        });
        requestBody.put("max_tokens", maxTokens);  // 150 // Limite du nombre de tokens
        requestBody.put("temperature", temperature); // 0.1 // Ajustement de la créativité

        // Sérialisation en JSON avec ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        String body;
        try {
            body = objectMapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la sérialisation JSON.", e);
        }

        // Envoi de la requête POST
        HttpEntity<String> entity = new HttpEntity<>(body, httpHeaders);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    aiApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // Vérifier le code de statut
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Erreur API Openai - Status: {}, Body: {}",
                    response.getStatusCode(), response.getBody());
                return "Erreur lors de l'appel à l'API IA.";
            }

            // Lire le contenu JSON
            String responseBody = response.getBody();
            if (responseBody == null) {
                return "Aucune réponse n'a été trouvée.";
            }

            // Récupérer le message de l'assistant
            // Extraire le champ `choices[0].message.content` de la réponse de l'API
            JsonNode rootNode = objectMapper.readTree(responseBody); //transforme en JSON
            String assistantMessage = rootNode
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();
            //    System.out.println("AI : Réponse STRING assistantMessage ---- : " + assistantMessage);

            // Récupérer les informations du nombre de tokens utilisés
            int promptTokens = rootNode
                    .path("usage")
                    .path("prompt_tokens")
                    .asInt();
            int completionTokens = rootNode
                    .path("usage")
                    .path("completion_tokens")
                    .asInt();
            int totalTokens = rootNode
                    .path("usage")
                    .path("total_tokens")
                    .asInt();

            // 💰 Tarifs GPT-4o mini (au 1er sept 2025) - en USD
            final double PRICE_INPUT_USD = 0.15 / 1_000_000;   // $ par token input
            final double PRICE_OUTPUT_USD = 0.60 / 1_000_000;  // $ par token output
            
            // Calculer le coût des tokens en USD (en utilisant promptTokens et completionTokens séparément)
            double tokenCostUsd = (promptTokens * PRICE_INPUT_USD) + (completionTokens * PRICE_OUTPUT_USD);
            
            // NOUVELLE POLITIQUE : Le prix de la requête = BASE_REQUEST_PRICE (dans la devise du marché)
            // Le coût des tokens est séparé et affiché uniquement aux administrateurs
            prix_requete = baseRequestPrice; // Prix de la requête dans la devise du marché

            // Stocker les informations d'utilisation dans le ThreadLocal pour le tracking
            UsageInfo usageInfo = new UsageInfo(
                totalTokens,
                prix_requete, // Prix de la requête = BASE_REQUEST_PRICE (devise marché)
                promptTokens,
                completionTokens,
                tokenCostUsd // Coût des tokens en USD (pour les admins uniquement)
            );
            currentUsage.set(usageInfo);

            // Log détaillé du calcul du coût
            // Note: Le tarif de base est dans la devise du marché, le coût des tokens est séparé
            log.debug("Calcul du coût - Niveau: {}, Prompt tokens: {}, Completion tokens: {}, Total tokens: {}, Tarif de base (requête): {} (devise marché), Coût tokens USD: {} $", 
                titre, promptTokens, completionTokens, totalTokens, String.format("%.6f", baseRequestPrice), 
                String.format("%.10f", tokenCostUsd));
            System.out.println("Niveau "+ titre +"  -Prompt Tokens = " + promptTokens + ", Completion Tokens = " + completionTokens + ", Total Tokens = " + totalTokens + 
                "   -Tarif de base (requête) = " + String.format("%.6f", baseRequestPrice) + " (devise marché)" +
                "   -Coût tokens USD = " + String.format("%.10f", tokenCostUsd) + " $ (admin uniquement)");

            /// //////////////////////////////////////////////////////////////

            return assistantMessage;

        } catch (Exception e) {
            // Logs pour un meilleur diagnostic
            System.err.println("Erreur lors de la requête à l'API OpenAI : " + e.getMessage());
            // Nettoyer le ThreadLocal en cas d'erreur
            currentUsage.remove();
            return "L'appel à l'API OpenAI a échoué.";
        }

    }
    
    /**
     * Récupère les informations d'utilisation de la requête courante.
     * @return UsageInfo contenant les tokens et le coût, ou null si aucune information disponible
     */
    public static UsageInfo getCurrentUsage() {
        return currentUsage.get();
    }
    
    /**
     * Nettoie les informations d'utilisation de la requête courante.
     * À appeler après avoir enregistré les informations pour éviter les fuites mémoire.
     */
    public static void clearCurrentUsage() {
        currentUsage.remove();
    }
}
