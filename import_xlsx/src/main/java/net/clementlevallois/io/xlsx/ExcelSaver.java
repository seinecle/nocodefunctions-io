/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.io.xlsx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.clementlevallois.functions.model.Occurrence;
import net.clementlevallois.importers.model.CellRecord;
import net.clementlevallois.importers.model.SheetModel;
import net.clementlevallois.umigon.model.Document;
import net.clementlevallois.utils.Multiset;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author LEVALLOIS
 */
public class ExcelSaver {

    public static byte[] exportUmigon(List<Document> results, String lang) {
        Locale locale = Locale.of(lang);

        ResourceBundle localeBundle = ResourceBundle.getBundle("net.clementlevallois.io.xlsx.i18n.text", locale);
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("results");
        int rowNumber = 0;
        // creating the header
        Row rowHeader = sheet.createRow(rowNumber++);
        Cell cell0Header = rowHeader.createCell(0, CellType.STRING);
        cell0Header.setCellValue(localeBundle.getString("general.nouns.line_number"));
        Cell cell1Header = rowHeader.createCell(1, CellType.STRING);
        cell1Header.setCellValue(localeBundle.getString("general.message.text_provided_as_input"));
        Cell cell2Header = rowHeader.createCell(2, CellType.STRING);
        cell2Header.setCellValue(localeBundle.getString("general.nouns.sentiment"));
        Cell cell3Header = rowHeader.createCell(3, CellType.STRING);
        cell3Header.setCellValue("language");
        Cell cell4Header = rowHeader.createCell(4, CellType.STRING);
        cell4Header.setCellValue(localeBundle.getString("general.nouns.explanations"));

        for (Document doc : results) {
            if (doc == null) {
                continue;
            }
            Row row = sheet.createRow(rowNumber++);
            Cell cell0 = row.createCell(0, CellType.STRING);
            cell0.setCellValue(String.valueOf(rowNumber - 1));
            Cell cell1 = row.createCell(1, CellType.STRING);
            if (doc.getText() != null) {
                cell1.setCellValue(doc.getText());
            }
            Cell cell2 = row.createCell(2, CellType.STRING);
            String sentiment;
            if (doc.getCategoryCode() == null) {
                System.out.println("no category code for this doc");
                continue;
            }
            sentiment = switch (doc.getCategoryCode()) {
                case "_12" ->
                    "😔 " + doc.getCategoryLocalizedPlainText();
                case "_11" ->
                    "🤗 " + doc.getCategoryLocalizedPlainText();
                default ->
                    "😐 " + doc.getCategoryLocalizedPlainText();
            };

            cell2.setCellValue(sentiment);
            Cell cell3 = row.createCell(3, CellType.STRING);
            if (doc.getLanguage() != null) {
                cell3.setCellValue(doc.getLanguage());
            }
            Cell cell4 = row.createCell(4, CellType.STRING);
            cell4.setCellValue(doc.getExplanationPlainText());
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            wb.write(bos);
        } catch (IOException ex) {
            Logger.getLogger(ExcelSaver.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte[] barray = bos.toByteArray();
        return barray;
    }

    public static byte[] exportPdfRegionExtractor(Map<String, SheetModel> results) {
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("results");
        int rowNumber = 0;
        // creating the headers
        Row rowHeader = sheet.createRow(rowNumber++);
        Cell cell0Header = rowHeader.createCell(0, CellType.STRING);
        cell0Header.setCellValue("file name");
        Cell cell1Header = rowHeader.createCell(1, CellType.STRING);
        cell1Header.setCellValue("page");
        Cell cell2Header = rowHeader.createCell(2, CellType.STRING);
        cell2Header.setCellValue("text extracted");

        for (Map.Entry<String, SheetModel> entry : results.entrySet()) {
            Row row = sheet.createRow(rowNumber++);
            Cell cell0 = row.createCell(0, CellType.STRING);
            cell0.setCellValue(entry.getKey());
            List<CellRecord> cellRecords = entry.getValue().getCellRecords();
            for (CellRecord cellRecord : cellRecords) {
                row = sheet.createRow(rowNumber++);
                Cell cell1 = row.createCell(1, CellType.STRING);
                cell1.setCellValue(String.valueOf(cellRecord.getRowIndex()));
                Cell cell2 = row.createCell(2, CellType.STRING);
                cell2.setCellValue(cellRecord.getRawValue());
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            wb.write(bos);
        } catch (IOException ex) {
            Logger.getLogger(ExcelSaver.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte[] barray = bos.toByteArray();
        return barray;
    }

    public static byte[] exportOrganic(List<Document> results, String lang) {
        Locale locale = Locale.of(lang);

        ResourceBundle localeBundle = ResourceBundle.getBundle("net.clementlevallois.io.xlsx.i18n.text", locale);
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("results");
        int rowNumber = 0;
        // creating the header
        Row rowHeader = sheet.createRow(rowNumber++);
        Cell cell0Header = rowHeader.createCell(0, CellType.STRING);
        cell0Header.setCellValue(localeBundle.getString("general.nouns.line_number"));
        Cell cell1Header = rowHeader.createCell(1, CellType.STRING);
        cell1Header.setCellValue(localeBundle.getString("general.message.text_provided_as_input"));
        Cell cell2Header = rowHeader.createCell(2, CellType.STRING);
        cell2Header.setCellValue(localeBundle.getString("organic.general.tone_of_voice"));
        Cell cell3Header = rowHeader.createCell(3, CellType.STRING);
        cell3Header.setCellValue("language");
        Cell cell4Header = rowHeader.createCell(4, CellType.STRING);
        cell4Header.setCellValue(localeBundle.getString("general.nouns.explanations"));

        for (Document doc : results) {
            if (doc == null) {
                continue;
            }
            Row row = sheet.createRow(rowNumber++);
            Cell cell0 = row.createCell(0, CellType.STRING);
            cell0.setCellValue(String.valueOf(rowNumber - 1));
            Cell cell1 = row.createCell(1, CellType.STRING);
            if (doc.getText() != null) {
                cell1.setCellValue(doc.getText());
            }
            Cell cell2 = row.createCell(2, CellType.STRING);
            if (doc.getCategoryCode() == null) {
                System.out.println("no category code for this doc");
                continue;
            }
            String organic;
            if (doc.getCategorizationResult().toString().startsWith("_061")) {
                organic = "📢 " + localeBundle.getString("organic.general.soundspromoted");
            } else {
                organic = "🌿 " + localeBundle.getString("organic.general.soundsorganic");
            }

            cell2.setCellValue(organic);
            Cell cell3 = row.createCell(3, CellType.STRING);
            if (doc.getLanguage() != null) {
                cell3.setCellValue(doc.getLanguage());
            }
            Cell cell4 = row.createCell(4, CellType.STRING);
            cell4.setCellValue(doc.getExplanationPlainText());
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            wb.write(bos);
        } catch (IOException ex) {
            Logger.getLogger(ExcelSaver.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte[] barray = bos.toByteArray();
        return barray;
    }

    public static byte[] exportTopics(Map<Integer, Multiset<String>> keywordsPerTopic, Map<Integer, Multiset<Integer>> topicsPerLine, int termsPerCommunity) {
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("keywords per topic");
        int rowNumber = 0;

        // creating the rows
        for (int r = 0; r <= termsPerCommunity; r++) {
            sheet.createRow(r);
        }

        for (Map.Entry<Integer, Multiset<String>> entry : keywordsPerTopic.entrySet()) {
            Integer topicNumber = entry.getKey();
            Multiset<String> termsInOneCommunity = entry.getValue();
            Cell cellHeader = sheet.getRow(0).createCell(topicNumber, CellType.STRING);
            cellHeader.setCellValue("Topic " + topicNumber);
            List<Map.Entry<String, Integer>> sortDesckeepMostfrequent = termsInOneCommunity.sortDesckeepMostfrequent(termsInOneCommunity, termsPerCommunity);
            int rowValue = 1;
            for (Map.Entry<String, Integer> entry2 : sortDesckeepMostfrequent) {
                Cell cellValue = sheet.getRow(rowValue).createCell(topicNumber, CellType.STRING);
                cellValue.setCellValue(entry2.getKey());
                rowValue++;
            }
        }

        /* a sheet where for each document, we see the topics it has been classified into
         the score value for each topic is the sum, for each word of the document, of their normalized betweenness centrality x 1000 000.
        This betweenness is computed on the network made of only the nodes and edges of the topic
        (because a topic is a sub-region of the entire network of words composing the document)
        SEE the Topics function for exact details of how the score is computed
         */
        XSSFSheet sheetDocuments = wb.createSheet("documents");
        sheetDocuments.createRow(0);
        Cell cellHeaderOne = sheetDocuments.getRow(0).createCell(1, CellType.STRING);
        cellHeaderOne.setCellValue("document number");
        Cell cellHeaderTwo = sheetDocuments.getRow(0).createCell(2, CellType.STRING);
        cellHeaderTwo.setCellValue("1st topic");
        Cell cellHeaderThree = sheetDocuments.getRow(0).createCell(3, CellType.STRING);
        cellHeaderThree.setCellValue("score 1st topic");
        Cell cellHeaderFour = sheetDocuments.getRow(0).createCell(4, CellType.STRING);
        cellHeaderFour.setCellValue("2nd topic");
        Cell cellHeaderFive = sheetDocuments.getRow(0).createCell(5, CellType.STRING);
        cellHeaderFive.setCellValue("score 2nd topic");
        Cell cellHeaderSix = sheetDocuments.getRow(0).createCell(6, CellType.STRING);
        cellHeaderSix.setCellValue("3rd topic");
        Cell cellHeaderSeven = sheetDocuments.getRow(0).createCell(7, CellType.STRING);
        cellHeaderSeven.setCellValue("score 3rd topic");

        rowNumber = 1;
        for (Map.Entry<Integer, Multiset<Integer>> entry : topicsPerLine.entrySet()) {
            sheetDocuments.createRow(rowNumber);
            Integer lineNumber = entry.getKey();
            Multiset<Integer> topTopics = entry.getValue();
            Cell cell = sheetDocuments.getRow(rowNumber).createCell(1, CellType.STRING);
            cell.setCellValue("document " + lineNumber);
            List<Map.Entry<Integer, Integer>> sortDesckeepMostfrequent = topTopics.sortDesckeepMostfrequent(topTopics, 3);
            int i = 2;
            for (Map.Entry<Integer, Integer> topTopicsForOneLine : sortDesckeepMostfrequent) {
                Cell cellValue = sheetDocuments.getRow(rowNumber).createCell(i, CellType.STRING);
                cellValue.setCellValue(topTopicsForOneLine.getKey());
                i++;
                Cell cellScore = sheetDocuments.getRow(rowNumber).createCell(i, CellType.STRING);
                cellScore.setCellValue(topTopicsForOneLine.getValue());
                i++;
            }
            rowNumber++;
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            wb.write(bos);
        } catch (IOException ex) {
            Logger.getLogger(ExcelSaver.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte[] barray = bos.toByteArray();
        return barray;
    }

    public static byte[] exportPdfMatcher(Map<String, List<Occurrence>> result) {
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("results");

        Iterator<Map.Entry<String, List<Occurrence>>> iterator = result.entrySet().iterator();
        // creating the rows

        int maxRows = 0;

        while (iterator.hasNext()) {
            Map.Entry<String, List<Occurrence>> next = iterator.next();
            if (next.getValue().size() > maxRows) {
                maxRows = next.getValue().size();
            }
        }

        for (int r = 0; r <= (maxRows + 5); r++) {
            sheet.createRow(r);
        }

        int colNum = 0;
        for (Map.Entry<String, List<Occurrence>> entry : result.entrySet()) {
            colNum++;
            String fileName = entry.getKey();
            List<Occurrence> occInOneFile = entry.getValue();
            Cell cellHeader = sheet.getRow(0).createCell(colNum, CellType.STRING);
            cellHeader.setCellValue("file " + fileName);
            int rowValue = 1;
            for (Occurrence occ : occInOneFile) {
                Cell cellValue = sheet.getRow(rowValue).createCell(colNum, CellType.STRING);
                cellValue.setCellValue("page " + occ.getPage() + ", context: " + occ.getContext());
                rowValue++;
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            wb.write(bos);
        } catch (IOException ex) {
            Logger.getLogger(ExcelSaver.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte[] barray = bos.toByteArray();
        return barray;
    }

    public static byte[] exportHighlighted(List<String[]> results) {
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("results");
        int rowNumber = 0;
        // creating the header
        Row rowHeader = sheet.createRow(rowNumber++);
        Cell cell0Header = rowHeader.createCell(0, CellType.STRING);
        cell0Header.setCellValue(String.valueOf("term"));
        Cell cell1Header = rowHeader.createCell(1, CellType.STRING);
        cell1Header.setCellValue("term highlighted in context");

        for (String[] entry : results) {
            Row row = sheet.createRow(rowNumber++);
            Cell cell0 = row.createCell(0, CellType.STRING);
            cell0.setCellValue(entry[0]);
            Cell cell1 = row.createCell(1, CellType.STRING);
            cell1.setCellValue(entry[1]);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            wb.write(bos);
        } catch (IOException ex) {
            Logger.getLogger(ExcelSaver.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte[] barray = bos.toByteArray();
        return barray;
    }
}
