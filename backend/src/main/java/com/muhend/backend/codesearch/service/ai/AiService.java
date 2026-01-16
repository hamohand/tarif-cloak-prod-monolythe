package com.muhend.backend.codesearch.service.ai;

import com.muhend.backend.codesearch.model.Position;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static com.muhend.backend.codesearch.z_outils.OutilsJson.*;

@Service
public class AiService {
    private final OpenAiService openAiService;
    private final AnthropicService anthropicService; // ANTHROPIC CLAUDE AI - AI - AI - AI - AI - AI -
    private final OpenAiServiceOllama openAiServiceOllama; // OLLAMA - AI - AI - AI - AI - AI - AI - AI -
    @Autowired
    public AiService(OpenAiService openAiService, AnthropicService anthropicService, OpenAiServiceOllama openAiServiceOllama) {
        this.openAiService = openAiService;
        this.anthropicService = anthropicService;
        this.openAiServiceOllama = openAiServiceOllama;
    }

    //-------------------------------------------------------------------------------
    // ---------------------------- Outils communs ----------------------
    //-------------------------------------------------------------------------------
    // Extract function: construction du prompt

    /**
     * Recherche les Sections pertinents en utilisant l'IA.
     * Cette méthode construit d'abord un RAG à partir de tous les Sections,
     * puis interroge l'IA pour obtenir les Sections correspondants au terme de recherche.
     *
     * @param termeRecherche Le terme de recherche fourni par l'utilisateur.
     * @return Une liste de positions correspondant aux Sections trouvés par l'IA.
     */

    public List<Position> promptEtReponse(String titre, String termeRecherche, List<Position> listePositions) {
        List<Position> ReponseCodesPosition;
        try {
            // méthode de création RAG
            StringBuilder leRAG = creerContexteRAG(titre, listePositions);

            // Étape 2 : IA pour obtenir la réponse brute
            String reponseIaJson = obtenirReponseJsonDeIA(titre, leRAG, termeRecherche); // IA-IA-IA

            // Nettoyer la réponse brute
            String jsonNettoye = cleanJsonString(reponseIaJson);
            //////////////////System.out.println("jsonNettoye : " + jsonNettoye);

            if (!isValidJson(jsonNettoye)) {
                // return "Le terme entré est insuffisant, aucune réponse trouvée. Donnez plus de précisions.";
                return Collections.emptyList();
            }
            // Transformer la réponse nettoyée en liste de codes
            ReponseCodesPosition = conversionReponseIaToList(jsonNettoye);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
        return ReponseCodesPosition;
    }

    private String construirePrompt(StringBuilder ragString, String termeRecherche) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("En utilisant la liste suivante : \n")
                .append(ragString).append("\n")
                .append("Recherchez tous les items qui contiennent la catégorie qui correspond à  : \"")
                .append(termeRecherche).append("\".")
                .append("L'aspect qui nous intéresse est la valeur du code.");

        return prompt.toString();
    }

    // Gestion d'erreur plus sûre: ne retourne plus null
    private String obtenirReponseJsonDeIA(String titre, StringBuilder ragString, String termeRecherche) {
        String prompt = construirePrompt(ragString, termeRecherche);
        try {
            return openAiService.demanderAiAide(titre, prompt); // OPENAI - AI - AI - AI - AI - AI - AI -
            //return anthropicService.demanderAiAide(prompt); // ANTHROPIC CLAUDE AI - AI - AI - AI - AI - AI -
            // return openAiServiceOllama.demanderAiAide(prompt); // ??? OLLAMA - AI - AI - AI - AI - AI - AI - AI -
        } catch (Exception e) {
            return "";
        }
    }
    //
    private StringBuilder formatterPosition(String code, String description, String justification) {
        StringBuilder affichePosition = new StringBuilder();
        affichePosition
                .append(" - Code = ").append(code)
                .append(" -\n\n");
        if (description != null && !description.isEmpty()) {
            affichePosition
                    .append("   _Description : ").append(description)
                    .append("\n\n");
        }
        if (justification != null && !justification.isEmpty()) {
            affichePosition
                    .append("   _Justification: ").append(justification)
                    .append("\n\n");
        }
        return affichePosition;
    }

    private StringBuilder creerContexteRAG(String titre, List<Position> positions) {
        // Etape : RAG
        StringBuilder stringRAG = new StringBuilder("RAG pour la recherche des : " + titre + "\n\n");
        for (Position position : positions) {
            stringRAG
                    .append(formatterPosition(position.getCode(), position.getDescription(), null));
        }
        //System.out.println("Liste  : " + stringRAG + "------------- ");
        return stringRAG;
    }
    //

    public StringBuilder formatterListeReponsesPourAffichage(String titre, List<Position> positions) {
        StringBuilder resultatAffiche = new StringBuilder("** " + titre + " **" + "\n\n");
        String code, justification, description;
        if (!positions.isEmpty()) {
            for (Position position : positions) {
                // Code
                code = position.getCode();
                // Justification
                justification = position.getJustification();
                // Description
                description = position.getDescription();
                //
                resultatAffiche.append("\n\n").append(formatterPosition(code, description, justification));
            }
        } else {
            resultatAffiche.append("Terme insuffisant, aucune réponse trouvée. Donnez plus de précisions.");
        }
        return resultatAffiche;
    }

}
