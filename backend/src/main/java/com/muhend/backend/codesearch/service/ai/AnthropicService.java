package com.muhend.backend.codesearch.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class AnthropicService {

    /**
     *
     */

    @Value("${ANTHROPIC_API_KEY}") // *** Anthropic Claude ***
    private String aiKey;

    //*** Anthropic Claude ***
    private final String aiApiUrl = "https://api.anthropic.com/v1/messages"; // URL corrigée
    //private final String aiModel = "claude-opus-4-20250514";
    //private final String aiModel = "claude-sonnet-4-20250514";
    private final String aiModel = "claude-3-7-sonnet-20250219";

    //   private final String OPENAI_MODEL = "llama3";
    private final int maxTokens = 1000;
    private final float temperature = 0.1F;

    //--- Message système pour l'IA ---
    private static final String SYSTEM_MESSAGE_TEMPLATE = """
            Extraction intelligente de codes douaniers
              Vous êtes un assistant multilingue spécialisé dans le domaine de la recherche des codes douaniers du commerce international.
               Tâche :
               À partir d’une liste complète ou partielle de codes douaniers (codes SH/HS/Harmonized System), identifie tous les codes et leurs descriptions qui pourraient raisonnablement s’appliquer au produit suivant :
               Produit cible : {décrire précisément le produit, ses caractéristiques, son usage, ses matériaux, sa nature, etc.}
               
               Instructions :
               - Analyse sémantiquement la description du produit.
               - Scanne la liste des codes douaniers fournie.
               - Sélectionne tous les codes dont les descriptions sont susceptibles de s'appliquer au produit, que ce soit de manière :
                    * directe (correspondance explicite),\\s
                    * ou indirecte (correspondance potentielle selon l’usage ou le matériau).
               - Pour chaque code sélectionné, fournis :{instruction_details}
               
               Format de sortie attendu un tableau JSON :
               Exemples de sortie :
           {format_example}
               
               Remarques :
                   Si plusieurs codes sont pertinents selon des contextes ou des interprétations différentes (ex : selon matière, usage, ou destination du produit), indique-les tous.
                   Ne sélectionne aucun code hors sujet, même si une partie du libellé semble correspondre.
               
               Veuillez répondre uniquement au format JSON avec les clés {json_keys}.\\s
               Toujours retourner un tableau JSON, même s'il ne contient qu'un seul élément ou est vide.
                   
               Voir exemples ci-dessous :
           {examples}
               """;

    // --- Contenus pour le prompt AVEC justification ---
    private static final String INSTRUCTION_DETAILS_JUSTIFIED = """
            
                    -- Le code douanier (à 2, 4 ou 6 chiffres selon la liste).
                    -- Une brève justification expliquant pourquoi ce code est pertinent.""";
    private static final String FORMAT_EXAMPLE_JUSTIFIED = """
                   [
                       {
                            "code": "...",
                            "justification": "Correspond au produit car..."
                       }
                   ]
                   ou
                   [
                       {
                            "code": "...",
                            "justification": "Correspond au produit car..."
                       },
                       {
                            "code": "...",
                            "justification": "Correspond au produit car..."
                       }
                   ]""";
    private static final String JSON_KEYS_JUSTIFIED = "`code` et `justification`";
    private static final String EXAMPLES_JUSTIFIED = """
               Exemple 1 :
                    USER : Pommes.
                    ASSISTANT :\\s
                    [   
                       {
                                "code": "08",
                                "justification": "C'est un code précis"
                       }
                   ]
                   
               Exemple 2 :
                    USER : T-shirt à manches courtes, en coton 100%, destiné aux hommes.
                    ASSISTANT :\\s
                    [
                       {
                                "code": "6109 10",
                                "justification": "C'est le code le plus précis pour les t-shirts en coton fabriqués en maille (bonneterie), ce qui est le cas ici."
                       },
                       {
                                "code": "6109 90",
                                "justification": "Le code pourrait être pertinent si le t-shirt était fait d’un mélange de coton et d’autres fibres, ce qui n’est pas le cas ici mais reste une alternative potentielle."
                       }
                    ]
           
               Exemple 3 :
                    USER : Drone quadrirotor destiné à un usage civil non militaire, pesant moins de 2 kg, équipé d'une caméra haute définition pour la photographie et la vidéographie aérienne. Fonctionne à batterie, piloté par télécommande ou application mobile.
                    ASSISTANT :\\s
                        [
                           {
                                "code": "8806 10",
                                "justification": "Ce code est directement destiné aux drones civils non militaires, ce qui correspond précisément au produit décrit."
                           },
                           {
                                "code": "8525 80",
                                "justification": "Le drone contient une caméra HD intégrée pour la capture d'images, cette partie du produit peut être concernée si la caméra est importée séparément ou mise en avant."
                           },
                           {
                                "code": "...",
                                "justification": "..."
                           }
                       ]""";

    // --- Contenus pour le prompt SANS justification ---
    private static final String INSTRUCTION_DETAILS_SIMPLE = """
            
                    le code douanier (à 2, 4 ou 6 chiffres selon la liste).""";
    private static final String FORMAT_EXAMPLE_SIMPLE = """
                   [
                       {
                            "code": "..."
                       }
                   ]
                   ou
                   [
                       {
                            "code": "..."
                       },
                       {
                            "code": "..."
                       }
                   ]""";
    private static final String JSON_KEYS_SIMPLE = "`code`";
    private static final String EXAMPLES_SIMPLE = """
               Exemple 1 :
                    USER : Pommes.
                    ASSISTANT :\\s
                    [   
                       {
                                "code": "08"
                       }
                   ]
                   
               Exemple 2 :
                    USER : T-shirt à manches courtes, en coton 100%, destiné aux hommes.
                    ASSISTANT :\\s
                    [
                       {
                                "code": "6109 10"
                       },
                       {
                                "code": "6109 90"
                       }
                    ]
           
               Exemple 3 :
                    USER : Drone quadrirotor destiné à un usage civil non militaire, pesant moins de 2 kg, équipé d'une caméra haute définition pour la photographie et la vidéographie aérienne. Fonctionne à batterie, piloté par télécommande ou application mobile.
                    ASSISTANT :\\s
                        [
                           {
                                "code": "8806 10"
                           },
                           {
                                "code": "8525 80"
                           },
                           {
                                "code": "..."
                           }
                       ]""";

    /**
     * Construit le message système pour l'IA, avec ou sans demande de justification.
     *
     * @param withJustification Si true, le prompt demandera une justification pour chaque code.
     * @return Le message système complet.
     */
    private String getSystemMessage(boolean withJustification) {
        if (withJustification) {
            return SYSTEM_MESSAGE_TEMPLATE
                    .replace("{instruction_details}", INSTRUCTION_DETAILS_JUSTIFIED)
                    .replace("{format_example}", FORMAT_EXAMPLE_JUSTIFIED)
                    .replace("{json_keys}", JSON_KEYS_JUSTIFIED)
                    .replace("{examples}", EXAMPLES_JUSTIFIED);
        } else {
            return SYSTEM_MESSAGE_TEMPLATE
                    .replace("{instruction_details}", INSTRUCTION_DETAILS_SIMPLE)
                    .replace("{format_example}", FORMAT_EXAMPLE_SIMPLE)
                    .replace("{json_keys}", JSON_KEYS_SIMPLE)
                    .replace("{examples}", EXAMPLES_SIMPLE);
        }
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Envoie une requête à l'API Anthropic Claude et traite la réponse.
     *
     * @param question La question à poser à l'assistant
     * @return La réponse formatée de l'assistant ou un message d'erreur
     */
    public String demanderAiAide(String question) {
        // Préparation du client REST
        // Correction du format d'authentification pour Anthropic
        // Utiliser x-api-key et non Authorization

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("x-api-key", aiKey);
        httpHeaders.add("Content-Type", "application/json");
        httpHeaders.add("anthropic-version", "2023-06-01"); // Version de l'API requise par Anthropic

        // Construction robuste du corps JSON
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiModel);
        requestBody.put("system", getSystemMessage(true)); // message système: true avec justification false sans

        // Les messages ne contiennent que la question de l'utilisateur
        requestBody.put("messages", new Object[]{
                Map.of("role", "user", "content", question)
        });

        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", temperature);

        // Afficher le corps de la requête pour le débogage
        log.debug("Requête à Anthropic: {}", requestBody);

        // Sérialisation en JSON avec ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        String body;
        try {
            body = objectMapper.writeValueAsString(requestBody);
            log.debug("Corps de la requête: {}", body);
        } catch (Exception e) {
            log.error("Erreur lors de la sérialisation JSON: {}", e.getMessage());
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
                log.error("Erreur API Anthropic - Status: {}, Body: {}",
                    response.getStatusCode(), response.getBody());
                return "Erreur lors de l'appel à l'API IA.";
            }

            // Lire le contenu JSON
            String responseBody = response.getBody();
            if (responseBody == null) {
                return "Aucune réponse n'a été trouvée.";
            }

            log.debug("Réponse brute d'Anthropic: {}", responseBody);

            // Extraction selon la documentation Anthropic
            JsonNode rootNode = objectMapper.readTree(responseBody);

            /// Structure de réponse Anthropic: content[0].text
            if (rootNode.has("content")) {
                JsonNode contentNode = rootNode.path("content");
                if (contentNode.isArray() && !contentNode.isEmpty()) {
                    String assistantMessage = contentNode.get(0).path("text").asText();
                    log.debug("Message extrait: {}", assistantMessage);
                    return assistantMessage;
                }
            }

            // Si la structure n'est pas celle attendue, essayer une autre structure possible
            if (rootNode.has("message") && rootNode.path("message").has("content")) {
                String assistantMessage = rootNode.path("message").path("content").asText();
                log.debug("Message extrait (format alternatif): {}", assistantMessage);
                return assistantMessage;
            }

            log.warn("Structure de réponse inattendue: {}", responseBody);
            return "Format de réponse non reconnu.";

        } catch (Exception e) {
            log.error("Erreur lors de la requête à l'API Anthropic: {}", e.getMessage(), e);
            return "L'appel à l'API Anthropic a échoué." + e.getMessage();
        }
    }
}

