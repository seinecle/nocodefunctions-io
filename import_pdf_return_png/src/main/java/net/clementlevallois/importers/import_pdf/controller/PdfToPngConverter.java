package net.clementlevallois.importers.import_pdf.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 *
 * @author LEVALLOIS
 */
public class PdfToPngConverter {

    public byte[][] convertPdfFileToPngs(InputStream is) throws IOException {
        PDDocument document = PDDocument.load(is);
        int numberOfPages = document.getNumberOfPages();
        byte[][] results = new byte[numberOfPages][];
        PDFRenderer pr = new PDFRenderer(document);
        BufferedImage bi;
        for (int page = 0; page < document.getNumberOfPages(); ++page) {
            bi = pr.renderImageWithDPI(page, 100);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "PNG", baos);
            results[page] = baos.toByteArray();
        }
        return results;
    }
}
