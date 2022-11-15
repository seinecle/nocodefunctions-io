/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */
package net.clementlevallois.importers.import_pdf.controller;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.clementlevallois.importers.model.CellRecord;
import net.clementlevallois.importers.model.ColumnModel;
import net.clementlevallois.importers.model.SheetModel;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 *
 * @author LEVALLOIS
 */
public class PdfImporter {

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }

    public List<SheetModel> importPdfFile(InputStream is, String fileName, String localizedEmptyLineMessage) {
        Map<Integer, String> lines = new TreeMap();
        List<SheetModel> sheets = new ArrayList();

        SheetModel sheetModel = new SheetModel();
        sheetModel.setName(fileName);

        try {
            PdfDocument myDocument = new PdfDocument(new PdfReader(is));
            int numberOfPages = myDocument.getNumberOfPages();
            int pageNumber;
            int i = 0;
            for (pageNumber = 1; pageNumber <= numberOfPages; pageNumber++) {
                sheetModel.getPageAndStartingLine().put(pageNumber, i);
                String textInDoc = PdfTextExtractor.getTextFromPage(myDocument.getPage(pageNumber), new SimpleTextExtractionStrategy());
                String linesArray[] = textInDoc.split("\\r?\\n");
                for (String line : linesArray) {
                    line = Jsoup.clean(line, Safelist.basicWithImages().addAttributes("span", "style"));
                    if (!line.isBlank()) {
                        lines.put(i++, line);
                    } else {
                        lines.put(i++, localizedEmptyLineMessage);
                    }
                }
            }
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

        } catch (IOException ex) {
            Logger.getLogger(PdfImporter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sheets;
    }
}
