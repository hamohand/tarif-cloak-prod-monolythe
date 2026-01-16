package com.muhend.backend.codesearch.controller;

import com.muhend.backend.codesearch.z_outils.conversion.SpreadsheetToCSV;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@RestController
//@RequestMapping("/api/conversion") // dev
@RequestMapping("/conversion")// prod
public class ConversionController {

    @PostMapping(
            path = "/convert",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> convertFile(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Aucun fichier n'a été téléversé.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            String originalFilename = file.getOriginalFilename();

            // Nous forçons la conversion en TSV en passant "\t" comme séparateur.
            assert originalFilename != null;
            String convertedData = SpreadsheetToCSV.convert(inputStream, originalFilename, "\t");

            // Le type de contenu est maintenant toujours 'text/tab-separated-values'
            String contentType = "text/tab-separated-values";

            // Construire une réponse 200 OK avec le contenu
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(convertedData);

        } catch (IllegalArgumentException e) {
            // Erreur 400 si le format n'est pas supporté
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Log de l'erreur pour le débogage
            e.printStackTrace();
            // Erreur 500 pour toute autre exception
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Une erreur interne est survenue lors de la conversion : " + e.getMessage());
        }
    }
}

