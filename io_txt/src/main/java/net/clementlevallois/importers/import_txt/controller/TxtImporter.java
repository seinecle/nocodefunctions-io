package net.clementlevallois.importers.import_txt.controller;

import com.sigpwned.chardet4j.Chardet;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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

    public List<SheetModel> importCooccurencesInTextFile(byte[] bytes, String fileName) {
        List<SheetModel> sheets = new ArrayList();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        try {
            CsvParserSettings settings = new CsvParserSettings();
            settings.detectFormatAutomatically();
            settings.setMaxCharsPerColumn(-1);
            CsvParser parser = new CsvParser(settings);
            Reader reader = Chardet.decode(byteArrayInputStream, StandardCharsets.UTF_8);
            List<String[]> rows = parser.parseAll(reader);
            SheetModel sheetModel = new SheetModel();
            sheetModel.setName(fileName);
            ColumnModel cm;
            List<ColumnModel> headerNames = new ArrayList();
            String[] firstLine = rows.get(0);
            int h = 0;
            for (String header : firstLine) {
                if (header == null) {
                    header = "";
                }
                header = Jsoup.clean(header, Safelist.basicWithImages().addAttributes("span", "style"));
                cm = new ColumnModel(String.valueOf(h++), header.trim());
                headerNames.add(cm);
            }

            sheetModel.setTableHeaderNames(headerNames);
            int j = 0;
            for (String[] row : rows) {
                int i = 0;
                for (String field : row) {
                    if (field == null) {
                        field = "";
                    }
                    field = Jsoup.clean(field, Safelist.basicWithImages().addAttributes("span", "style"));
                    CellRecord cellRecord = new CellRecord(j, i++, field.trim());
                    sheetModel.addCellRecord(cellRecord);
                }
                j++;
            }
            sheets.add(sheetModel);

        } catch (IOException ex) {
            System.out.println("exception: " + ex.getMessage());
        }
        return sheets;
    }

    public String importSimpleLinesInTextFile(byte[] bytes) {
        try (Reader reader = Chardet.decode(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8); BufferedReader br = new BufferedReader(reader)) {

            return br.lines()
                    .map(line -> line == null ? "" : line)
                    .map(line -> Jsoup.clean(line, Safelist.basicWithImages().addAttributes("span", "style")))
                    .collect(Collectors.joining("\n"));

        } catch (IOException ex) {
            System.getLogger(TxtImporter.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            return "";
        }
    }
}
