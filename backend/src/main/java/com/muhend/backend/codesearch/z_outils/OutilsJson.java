package com.muhend.backend.codesearch.z_outils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muhend.backend.codesearch.model.Position;

import java.util.List;

public class OutilsJson {

    //Nettoyage du Json ----------------------------------------------------------------
    private static final String ERROR_EMPTY_JSON = "Réponse JSON vide ou nulle !";
    private static final String ERROR_INVALID_JSON = "Veuillez donner plus de précisions dans votre question. La réponse n'est pas un JSON valide : ";
    private static final String JSON_PREFIX = "json";
    private static final char BACKTICK = '`';

    //Création d'un tableau Java à partir de la réponse JSON de l'IA
    public static List<Position> conversionReponseIaToList(String cleanedJson) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonPourListe = cleanedJson.trim();
        if (jsonPourListe.startsWith("{")) {
            jsonPourListe = "[" + jsonPourListe + "]";
        }
        List<Position> positions = objectMapper.readValue(jsonPourListe, new TypeReference<List<Position>>() {});
        //System.out.println("Positions extraites de la réponse IA : " + positions);
        return positions;
    }

    public static String cleanJsonString(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isBlank()) {
            throw new RuntimeException(ERROR_EMPTY_JSON);
        }

        String cleanedJson = jsonResponse.trim();
        cleanedJson = removeEnclosingBackticks(cleanedJson);

        if (cleanedJson.startsWith(JSON_PREFIX)) {
            cleanedJson = cleanedJson.substring(JSON_PREFIX.length()).trim();
        }

        if (!isValidJson(cleanedJson)) {
            throw new RuntimeException(ERROR_INVALID_JSON + cleanedJson);
        }

        return cleanedJson;
    }

    public static String removeEnclosingBackticks(String json) {
        while (json.startsWith(String.valueOf(BACKTICK))) {
            json = json.substring(1).trim();
        }
        while (json.endsWith(String.valueOf(BACKTICK))) {
            json = json.substring(0, json.length() - 1).trim();
        }
        return json;
    }

    //Vérifie si JSON
    public static boolean isValidJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.readTree(json); // Si le parsing réussit, c'est un JSON valide
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
