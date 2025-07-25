package net.clementlevallois.importers.import_pdf.controller;

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
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PageExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 *
 * @author LEVALLOIS
 */
public class PdfImporter {

    static {
        Logger pdfboxLogger = Logger.getLogger("org.apache.pdfbox");
        pdfboxLogger.setLevel(Level.SEVERE);
    }

    public String importPdfFileToSimpleLines(InputStream is) {
        Map<Integer, String> lines = new TreeMap();

        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(is))) {

            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            PageExtractor pageExtractor;
            int numberOfPages = doc.getPages().getCount();
            int pageNumber;
            int i = 0;
            for (pageNumber = 1; pageNumber <= numberOfPages; pageNumber++) {
                pageExtractor = new PageExtractor(doc, pageNumber, pageNumber);
                try (PDDocument pageAsDoc = pageExtractor.extract()) {
                    String textInPage = pdfTextStripper.getText(pageAsDoc);

                    String linesArray[] = textInPage.split("\\r?\\n");
                    for (String line : linesArray) {
                        lines.put(i++, line);
                    }
                }
            }
            doc.close();

            /* this step addresses the case of text imported from a PDF source.
            
            PDF imports can have their last words truncated at the end, like so:
            
            "Inbound call centers tend to focus on assistance for customers who need to solve their problems, ques-
            tions bout a product or service, schedule appointments, dispatch technicians, or need instructions"

            In this case, the last word of the first sentence should be removed,
            and so should be the first word of the following line.
            
            Thx to https://twitter.com/Verukita1 for reporting the issue with a test case.
            
             */
            lines = Utils.fixHyphenatedWordsAndReturnOriginalEntries(lines);

            StringBuilder sb = new StringBuilder();
            lines.entrySet().forEach(e -> sb.append(e.getValue()).append("\n"));

            return sb.toString();

        } catch (IOException ex) {
            System.out.println("file in pdf import io to simple lines function caused an IO exception : empty or corrupted file, or not pdf");
            return "";
        }
    }

    public List<SheetModel> importPdfFileToLinesPerPage(InputStream is, String fileName, String localizedEmptyLineMessage) {
        Map<Integer, String> lines = new TreeMap();
        List<SheetModel> sheets = new ArrayList();

        SheetModel sheetModel = new SheetModel();
        sheetModel.setName(fileName);
        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(is))) {

            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            PageExtractor pageExtractor;

            int numberOfPages = doc.getPages().getCount();
            int pageNumber;
            int i = 0;
            for (pageNumber = 1; pageNumber <= numberOfPages; pageNumber++) {
                sheetModel.getPageAndStartingLine().put(pageNumber, i);
                pageExtractor = new PageExtractor(doc, pageNumber, pageNumber);
                try (PDDocument pageAsDoc = pageExtractor.extract()) {
                    String textInPage = pdfTextStripper.getText(pageAsDoc);
                    
                    String linesArray[] = textInPage.split("\\r?\\n");
                    for (String line : linesArray) {
                        if (!line.isBlank()) {
                            lines.put(i++, line);
                        } else {
                            lines.put(i++, localizedEmptyLineMessage);
                        }
                    }
                }
            }
            doc.close();
//            myDocument.close();

            /* this step addresses the case of text imported from a PDF source.
            
            PDF imports can have their last words truncated at the end, like so:
            
            "Inbound call centers tend to focus on assistance for customers who need to solve their problems, ques-
            tions bout a product or service, schedule appointments, dispatch technicians, or need instructions"

            In this case, the last word of the first sentence should be removed,
            and so should be the first word of the following line.
            
            Thx to https://twitter.com/Verukita1 for reporting the issue with a test case.
            
             */
            lines = Utils.fixHyphenatedWordsAndReturnOriginalEntries(lines);

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
            System.out.println("file in pdf import io function caused an IO exception : empty or corrupted file, or not pdf");
        }
        return sheets;
    }

}
