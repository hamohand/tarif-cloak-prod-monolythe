package com.muhend.backend.codesearch.z_outils.conversion;

import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Cell;
import org.odftoolkit.simple.table.Row;
import org.odftoolkit.simple.table.Table;

import java.io.FileWriter;
import java.io.PrintWriter;

public class OdsToCsv {
    public static void main(String[] args) throws Exception {
        String odsFile = "C:\\Users\\hamoh\\Downloads\\fruits-saison.ods";
        String csvFile = "C:\\Users\\hamoh\\Downloads\\fruits-saison.csv";

        // Charger le document ODS
        SpreadsheetDocument document = SpreadsheetDocument.loadDocument(odsFile);

        // Prendre la première feuille (ou boucler sur toutes si besoin)
        Table sheet = document.getSheetByIndex(0);

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            for (Row row : sheet.getRowList()) {
                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < row.getCellCount(); i++) {
                    Cell cell = row.getCellByIndex(i);
                    String value = cell.getDisplayText(); // récupère le texte affiché

                    sb.append(value);

                    // Séparateur CSV → "," ; pour TSV → "\t"
                    if (i < row.getCellCount() - 1) {
                        sb.append(",");
                    }
                }

                writer.println(sb);
            }
        }

        System.out.println("Conversion ODS terminée : " + csvFile);
    }
}

