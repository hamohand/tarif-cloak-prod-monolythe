package com.muhend.backend.codesearch.z_outils.conversion;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Cell;
import org.odftoolkit.simple.table.Row;
import org.odftoolkit.simple.table.Table;

import java.io.*;

public class SpreadsheetToCSV {

    /**
     * Convertit un fichier tableur (Excel ou ODS) fourni en tant que flux d'entrée en une chaîne de caractères CSV ou TSV.
     *
     * @param inputStream Le flux de données du fichier à convertir.
     * @param originalFilename Le nom original du fichier (pour détecter l'extension).
     * @param outputFormat Le format de sortie souhaité ("csv" ou "tsv").
     * @return Une chaîne contenant les données converties.
     * @throws Exception Si la conversion échoue ou si le format n'est pas supporté.
     */
    public static String convert(InputStream inputStream, String originalFilename, String separator) throws Exception {
        String lowerInput = originalFilename.toLowerCase();
        StringWriter writer = new StringWriter();

        if (lowerInput.endsWith(".ods")) {
            convertOdsToCsv(inputStream, writer, separator);
        } else if (lowerInput.endsWith(".xls") || lowerInput.endsWith(".xlsx")) {
            convertExcelToCsv(inputStream, writer, separator);
        } else {
            throw new IllegalArgumentException("Format de fichier non supporté : " + originalFilename + ". Seuls les formats .ods, .xls, .xlsx sont acceptés.");
        }

        return writer.toString();
    }

    private static void convertOdsToCsv(InputStream inputStream, Writer writer, String separator) throws Exception {
        try (SpreadsheetDocument document = SpreadsheetDocument.loadDocument(inputStream);
             PrintWriter printWriter = new PrintWriter(writer)) {

            Table sheet = document.getSheetByIndex(0);
            for (Row row : sheet.getRowList()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < row.getCellCount(); i++) {
                    Cell cell = row.getCellByIndex(i);
                    sb.append(cell.getDisplayText());
                    if (i < row.getCellCount() - 1) {
                        sb.append(separator);
                    }
                }
                printWriter.println(sb);
            }
        }
    }

    private static void convertExcelToCsv(InputStream inputStream, Writer writer, String separator) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(inputStream);
             PrintWriter printWriter = new PrintWriter(new BufferedWriter(writer))) {

            Sheet sheet = workbook.getSheetAt(0);
            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                StringBuilder sb = new StringBuilder();

                int lastCellNum = row.getLastCellNum();
                if (lastCellNum < 0) { // Gérer les lignes vides
                    lastCellNum = 0;
                }

                for (int i = 0; i < lastCellNum; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(i, org.apache.poi.ss.usermodel.Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

                    String value = switch (cell.getCellType()) {
                        case STRING -> cell.getStringCellValue();
                        case NUMERIC -> String.valueOf(cell.getNumericCellValue());
                        case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                        case FORMULA -> cell.getCellFormula();
                        case BLANK -> "";
                        case ERROR -> String.valueOf(cell.getErrorCellValue());
                        default -> "";
                    };

                    sb.append(value);

                    if (i < row.getLastCellNum() - 1) {
                        sb.append(separator);
                    }
                }
                printWriter.println(sb);
            }
        }
    }
}