/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */
package net.clementlevallois.importers.import_txt.controller;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import static java.util.stream.Collectors.toList;
import net.clementlevallois.importers.model.CellRecord;
import net.clementlevallois.importers.model.ColumnModel;
import net.clementlevallois.importers.model.SheetModel;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 *
 * @author LEVALLOIS
 */
public class TxtImporter {

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }

    public List<SheetModel> importTextFile(InputStream is, String fileName, String functionName, String gazeOption) {
        Map<Integer, String> lines = new TreeMap();
        List<SheetModel> sheets = new ArrayList();
        try {

            // if we are computing cooccurrences, we need the lines of text to be decomposed as a csv file.
            if (functionName.equals("gaze") && gazeOption.equals("1")) {
                CsvParserSettings settings = new CsvParserSettings();
                settings.detectFormatAutomatically();
                settings.setMaxCharsPerColumn(-1);
                CsvParser parser = new CsvParser(settings);
                InputStreamReader reader;
                reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                List<String[]> rows = parser.parseAll(reader);
                SheetModel sheetModel = new SheetModel();
                sheetModel.setName(fileName);
                ColumnModel cm;
                List<ColumnModel> headerNames = new ArrayList();
                String[] firstLine = rows.get(0);
                int h = 0;
                for (String header : firstLine) {
                    header = Jsoup.clean(header, Safelist.basicWithImages().addAttributes("span", "style"));
                    cm = new ColumnModel(String.valueOf(h++), header.trim());
                    headerNames.add(cm);
                }

                sheetModel.setTableHeaderNames(headerNames);
                int j = 0;
                for (String[] row : rows) {
                    int i = 0;
                    for (String field : row) {
                        field = Jsoup.clean(field, Safelist.basicWithImages().addAttributes("span", "style"));
                        CellRecord cellRecord = new CellRecord(j, i++, field.trim());
                        sheetModel.addCellRecord(cellRecord);
                    }
                    j++;
                }
                sheets.add(sheetModel);


            } // normal case of importing text as flat lines without caring for cooccurrences
            else {
                List<String> txtLines;
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                txtLines = br.lines().collect(toList());
                br.close();
                int i = 0;
                for (String line : txtLines) {
                    line = Jsoup.clean(line, Safelist.basicWithImages().addAttributes("span", "style"));
                    lines.put(i++, line);
                }
                SheetModel sheetModel = new SheetModel();
                sheetModel.setName(fileName);
                ColumnModel cm;
                cm = new ColumnModel("0", lines.get(0));
                List<ColumnModel> headerNames = new ArrayList();
                headerNames.add(cm);
                sheetModel.setTableHeaderNames(headerNames);
                for (Map.Entry<Integer, String> line : lines.entrySet()) {
                    CellRecord cellRecord = new CellRecord(line.getKey(), 0, line.getValue());
                    sheetModel.addCellRecord(cellRecord);
                }
                sheets.add(sheetModel);
            }
        } catch (IOException ex) {
            System.out.println("exception: "+ ex.getMessage());
        }
        return sheets;
    }
}
