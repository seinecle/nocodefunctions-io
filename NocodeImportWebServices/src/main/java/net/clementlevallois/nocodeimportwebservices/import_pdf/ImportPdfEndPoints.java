/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.nocodeimportwebservices.import_pdf;

import io.javalin.Javalin;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import net.clementlevallois.importers.import_pdf.controller.PdfImporter;
import net.clementlevallois.importers.model.SheetModel;
import net.clementlevallois.nocodeimportwebservices.APIController;
import static net.clementlevallois.nocodeimportwebservices.APIController.increment;

/**
 *
 * @author LEVALLOIS
 */
public class ImportPdfEndPoints {

    public static void main(String[] args) {
    }

    public static Javalin addAll(Javalin app) throws Exception {

        app.post("/api/import/pdf", ctx -> {
            increment();
            byte[] bodyAsBytes = ctx.bodyAsBytes();

            String fileName = ctx.queryParam("fileName");
            String localizedEmptyLineMessage = ctx.queryParam("localizedEmptyLineMessage");

            InputStream is = new ByteArrayInputStream(bodyAsBytes);
            PdfImporter pdfImporter = new PdfImporter();
            List<SheetModel> sheets = pdfImporter.importPdfFile(is, fileName, localizedEmptyLineMessage);

            byte[] byteArray = APIController.byteArraySerializerForSheets(sheets);

            ctx.result(byteArray).status(HttpURLConnection.HTTP_OK);
        });

        return app;
    }

}
