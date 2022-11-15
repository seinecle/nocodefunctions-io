/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */
package net.clementlevallois.importers.import_csv.controller;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.clementlevallois.importers.model.CellRecord;
import net.clementlevallois.importers.model.ColumnModel;
import net.clementlevallois.importers.model.SheetModel;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 *
 * @author LEVALLOIS
 */
public class CsvImporter {

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }

    public List<SheetModel> importCsvFile(InputStream is, String fileName, String functionName, String gazeOption) {
        InputStreamReader reader;
        List<SheetModel> sheets = new ArrayList();
            CsvParserSettings settings = new CsvParserSettings();
            settings.detectFormatAutomatically();
            settings.setMaxCharsPerColumn(-1);
            CsvParser parser = new CsvParser(settings);
            reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            List<String[]> rows = parser.parseAll(reader);
            // if you want to see what delimiter it detected
//            CsvFormat format = parser.getDetectedFormat();
            SheetModel sheetModel = new SheetModel();
            sheetModel.setName(fileName);
            ColumnModel cm;
            List<ColumnModel> headerNames = new ArrayList();
            String[] firstLine = rows.get(0);
            int h = 0;
            for (String header : firstLine) {
                header = Jsoup.clean(header, Safelist.basicWithImages().addAttributes("span", "style"));
                cm = new ColumnModel(String.valueOf(h++), header);
                headerNames.add(cm);
            }

            sheetModel.setTableHeaderNames(headerNames);
            int j = 0;
            for (String[] row : rows) {
                int i = 0;
                for (String field : row) {
                    field = Jsoup.clean(field, Safelist.basicWithImages().addAttributes("span", "style"));
                    CellRecord cellRecord = new CellRecord(j, i++, field);
                    sheetModel.addCellRecord(cellRecord);
                }
                j++;
            }
            sheets.add(sheetModel);
            parser.stopParsing();
        return sheets;
    }
}
