/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */
package net.clementlevallois.importers.import_pdf.controller;

//import com.itextpdf.kernel.pdf.PdfDocument;
//import com.itextpdf.kernel.pdf.PdfReader;
//import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
//import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy;
import java.awt.geom.Rectangle2D;
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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 *
 * @author LEVALLOIS
 */
public class PdfExtractorByRegion {

    static {
        // Configure logging levels
        Logger pdfboxLogger = Logger.getLogger("org.apache.pdfbox");
        pdfboxLogger.setLevel(Level.SEVERE);
    }

    public SheetModel extractTextFromRegionInPdf(InputStream is, String fileName, Boolean allPages, Integer selectedPage, Float topLeftX, Float topLeftY, Float width, Float height) {
        Map<Integer, String> textPerPage = new TreeMap();

        SheetModel sheetModel = new SheetModel();
        sheetModel.setName(fileName);

        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(is))) {
            int docPagesCount = doc.getPages().getCount();
            if (!allPages && selectedPage > docPagesCount) {
                CellRecord cellRecord = new CellRecord(0, 0, "selected page is higher than the count of pages in the doc");
                sheetModel.addCellRecord(cellRecord);
                return sheetModel;
            }

            if (allPages) {
                int pageNumber;
                int i = 0;
                for (pageNumber = 0; pageNumber < docPagesCount; pageNumber++) {
                    PDPage docPage = doc.getPage(pageNumber);
                    PDFTextStripperByArea textStripper = new PDFTextStripperByArea();
                    float heightPage = docPage.getMediaBox().getHeight();
                    float widthPage = docPage.getMediaBox().getWidth();
                    Rectangle2D rect = new Rectangle2D.Float(topLeftX * widthPage, topLeftY * heightPage, width * widthPage, height * heightPage);
                    textStripper.addRegion("region", rect);
                    textStripper.extractRegions(docPage);
                    String textForRegion = textStripper.getTextForRegion("region");
                    String linesArray[] = textForRegion.split("\\r?\\n");
                    String text = Utils.fixHyphenatedWordsAndReturnAllLinesInOnePara(linesArray);
                    text = Jsoup.clean(text, Safelist.basicWithImages().addAttributes("span", "style"));
                    textPerPage.put(pageNumber, text);
                }
            } else {
                PDPage docPage = doc.getPage(selectedPage);
                PDFTextStripperByArea textStripper = new PDFTextStripperByArea();
                float heightPage = docPage.getMediaBox().getHeight();
                float widthPage = docPage.getMediaBox().getWidth();
                Rectangle2D rect = new Rectangle2D.Float(topLeftX * widthPage, topLeftY * heightPage, width * widthPage, height * heightPage);
                textStripper.addRegion("region", rect);
                textStripper.extractRegions(docPage);
                String textForRegion = textStripper.getTextForRegion("region");
                textForRegion = Jsoup.clean(textForRegion, Safelist.basicWithImages().addAttributes("span", "style"));
                String linesArray[] = textForRegion.split("\\r?\\n");
                String text = Utils.fixHyphenatedWordsAndReturnAllLinesInOnePara(linesArray);
                text = Jsoup.clean(text, Safelist.basicWithImages().addAttributes("span", "style"));
                textPerPage.put(selectedPage, text);
            }
            doc.close();
            ColumnModel cm;
            cm = new ColumnModel("0", textPerPage.get(0));
            List<ColumnModel> headerNames = new ArrayList();
            headerNames.add(cm);
            sheetModel.setTableHeaderNames(headerNames);
            for (Map.Entry<Integer, String> line : textPerPage.entrySet()) {
                CellRecord cellRecord = new CellRecord(line.getKey(), 0, line.getValue());
                sheetModel.addCellRecord(cellRecord);
            }

        } catch (IOException ex) {
            Logger.getLogger(PdfExtractorByRegion.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sheetModel;
    }
}
