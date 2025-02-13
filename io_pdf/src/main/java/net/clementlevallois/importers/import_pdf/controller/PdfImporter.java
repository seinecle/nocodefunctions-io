/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */
package net.clementlevallois.importers.import_pdf.controller;

//import com.itextpdf.kernel.pdf.PdfDocument;
//import com.itextpdf.kernel.pdf.PdfReader;
//import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
//import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy;
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
        // Configure logging levels
        Logger pdfboxLogger = Logger.getLogger("org.apache.pdfbox");
        pdfboxLogger.setLevel(Level.SEVERE);
        
        // Specifically for the Splitter if needed
        Logger splitterLogger = Logger.getLogger("org.apache.pdfbox.multipdf");
        splitterLogger.setLevel(Level.SEVERE);
    }

    public List<SheetModel> importPdfFile(InputStream is, String fileName, String localizedEmptyLineMessage) {
        Map<Integer, String> lines = new TreeMap();
        List<SheetModel> sheets = new ArrayList();

        SheetModel sheetModel = new SheetModel();
        sheetModel.setName(fileName);
        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(is))) {

            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            PageExtractor pageExtractor;

//            PdfDocument myDocument = new PdfDocument(new PdfReader(is));
//            int numberOfPages = myDocument.getNumberOfPages();
            int numberOfPages = doc.getPages().getCount();
            int pageNumber;
            int i = 0;
            for (pageNumber = 1; pageNumber <= numberOfPages; pageNumber++) {
                sheetModel.getPageAndStartingLine().put(pageNumber, i);
                pageExtractor = new PageExtractor(doc, pageNumber, pageNumber);
                PDDocument pageAsDoc = pageExtractor.extract();
                String textInPage = pdfTextStripper.getText(pageAsDoc);

                String linesArray[] = textInPage.split("\\r?\\n");
                for (String line : linesArray) {
                    if (!line.isBlank()) {
                        lines.put(i++, line);
                    } else {
                        lines.put(i++, localizedEmptyLineMessage);
                    }
                }
                pageAsDoc.close();
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
            Logger.getLogger(PdfImporter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sheets;
    }
}
