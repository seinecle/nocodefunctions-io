package net.clementlevallois.importers.import_csv.controller;

import com.sigpwned.chardet4j.Chardet;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.clementlevallois.importers.model.CellRecord;
import net.clementlevallois.importers.model.ColumnModel;
import net.clementlevallois.importers.model.SheetModel;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class CsvImporter {

    public List<SheetModel> importCsvFile(byte[] bytes, String fileName) {
        List<SheetModel> sheets = new ArrayList<>();

        CsvParserSettings settings = new CsvParserSettings();
        settings.detectFormatAutomatically();
        settings.setMaxCharsPerColumn(-1);
        CsvParser parser = new CsvParser(settings);

        try (Reader reader = Chardet.decode(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            List<String[]> rows = parser.parseAll(reader);

            if (rows.isEmpty()) {
                return sheets;
            }

            SheetModel sheetModel = new SheetModel();
            sheetModel.setName(fileName);

            List<ColumnModel> headerNames = new ArrayList<>();
            String[] firstLine = rows.getFirst();
            String substituteHeaderChars = "ABCDEFGHIJKLMNOPQRSTUVWYZ";
            int indexEmptyHeader = 0;

            for (int h = 0; h < firstLine.length; h++) {
                String header = firstLine[h];
                if (header == null || header.isBlank()) {
                    header = "header was empty! " + substituteHeaderChars.charAt(indexEmptyHeader);
                    indexEmptyHeader = (indexEmptyHeader + 1) % substituteHeaderChars.length();
                }
                header = Jsoup.clean(header, Safelist.basicWithImages().addAttributes("span", "style"));
                headerNames.add(new ColumnModel(String.valueOf(h), header));
            }
            sheetModel.setTableHeaderNames(headerNames);

            for (int j = 0; j < rows.size(); j++) {
                String[] row = rows.get(j);
                for (int i = 0; i < row.length; i++) {
                    String field = row[i];
                    if (field == null) {
                        field = "";
                    }
                    field = Jsoup.clean(field, Safelist.basicWithImages().addAttributes("span", "style"));
                    sheetModel.addCellRecordToVariousDataStructures(new CellRecord(j, i, field));
                }
            }
            sheets.add(sheetModel);
            parser.stopParsing();
        } catch (IOException ex) {
            System.err.println("Error when reading CSV file: " + ex.getMessage());
        }
        return sheets;
    }

    public String importCsvFileToSimpleLines(byte[] bytes, int colIndex, boolean hasHeaders) {
        StringBuilder result = new StringBuilder();
        CsvParserSettings settings = new CsvParserSettings();
        settings.detectFormatAutomatically();
        settings.setMaxCharsPerColumn(-1);
        CsvParser parser = new CsvParser(settings);

        try (Reader reader = Chardet.decode(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            List<String[]> rows = parser.parseAll(reader);

            if (rows.isEmpty()) {
                return "";
            }

            int startRow = 0;
            if (hasHeaders) {
                // Add the header from the selected column if it exists and hasHeaders is true
                String[] firstLine = rows.getFirst();
                if (colIndex >= 0 && colIndex < firstLine.length) {
                    String header = firstLine[colIndex];
                    result.append(cleanAndSanitize(header)).append(System.lineSeparator());
                }
                startRow = 1; // Start processing from the second row (data rows)
            }

            // Iterate through the rows, starting from 'startRow'
            for (int j = startRow; j < rows.size(); j++) {
                String[] row = rows.get(j);
                if (colIndex >= 0 && colIndex < row.length) {
                    String field = row[colIndex];
                    result.append(cleanAndSanitize(field)).append(System.lineSeparator());
                }
            }
            parser.stopParsing();
        } catch (IOException ex) {
            System.err.println("Error when reading CSV file: " + ex.getMessage());
        }
        return result.toString();
    }

    private String cleanAndSanitize(String text) {
        if (text == null) {
            return "";
        }
        // Sanitize HTML content
        return Jsoup.clean(text, Safelist.basicWithImages().addAttributes("span", "style"));
    }
}
