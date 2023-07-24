package net.clementlevallois.importers.import_pdf.controller;

import java.awt.Image;
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
            int width = bi.getWidth();
            int height = bi.getHeight();
            int maxWidth = 800;
            int maxHeight = 800;
            float factorWidthTooBig = width / maxWidth;
            float factorHeightTooBig = height / maxHeight;
            if (factorWidthTooBig > 2 || factorHeightTooBig > 2) {
                float resizeFactor = Math.max(factorWidthTooBig, factorHeightTooBig);
                int targetWidth = (int) ((float) width / resizeFactor);
                int targetHeight = (int) ((float) height / resizeFactor);
                Image resultingImage = bi.getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT);
                BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
                outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
                bi = outputImage;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "PNG", baos);
            results[page] = baos.toByteArray();
        }
        document.close();
        return results;
    }
}
