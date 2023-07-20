/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.nocodeimportwebservices.import_pdf;

import io.javalin.Javalin;
import io.javalin.http.util.NaiveRateLimit;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.clementlevallois.importers.import_pdf.controller.PdfImporter;
import net.clementlevallois.importers.import_pdf.controller.PdfExtractorByRegion;
import net.clementlevallois.importers.import_pdf.controller.PdfToPngConverter;
import net.clementlevallois.importers.model.ImagesPerFile;
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

        app.post("/api/import/pdf/return-png", ctx -> {
            increment();
            byte[] bodyAsBytes = ctx.bodyAsBytes();

            String fileName = ctx.queryParam("fileName");
            InputStream is = new ByteArrayInputStream(bodyAsBytes);
            PdfToPngConverter pdfToPngConverter = new PdfToPngConverter();
            byte[][] images = pdfToPngConverter.convertPdfFileToPngs(is);
            ImagesPerFile ipf = new ImagesPerFile();
            ipf.setImages(images);
            ipf.setFileName(fileName);
            byte[] byteArraySerializerForAnyObject = APIController.byteArraySerializerForAnyObject(ipf);
            ctx.result(byteArraySerializerForAnyObject).status(HttpURLConnection.HTTP_OK);
        });

        app.post("/api/import/pdf/extract-region", ctx -> {
            increment();
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
            NaiveRateLimit.requestPerTimeUnit(ctx, 50, TimeUnit.SECONDS);
            byte[] pdfAsBytes = null;
            String fileName = null;
            boolean allPages = false;
            Integer selectedPage = null;
            Float leftCornerX = null;
            Float leftCornerY = null;
            Float width = null;
            Float height = null;
            byte[] bodyAsBytes = ctx.bodyAsBytes();
            String body = new String(bodyAsBytes, StandardCharsets.US_ASCII);
            if (body.isEmpty()) {
                objectBuilder.add("-99", "body of the request should not be empty");
                JsonObject jsonObject = objectBuilder.build();
                ctx.result(jsonObject.toString()).status(HttpURLConnection.HTTP_BAD_REQUEST);
            } else {
                JsonReader jsonReader = Json.createReader(new StringReader(body));
                JsonObject jsonObject = jsonReader.readObject();
                for (String nextKey : jsonObject.keySet()) {
                    if (nextKey.equals("pdfBytes")) {
                        pdfAsBytes = Base64.getDecoder().decode(jsonObject.getString(nextKey));
                    }
                    if (nextKey.equals("fileName")) {
                        fileName = jsonObject.getString(nextKey);
                    }
                    if (nextKey.equals("allPages")) {
                        allPages = jsonObject.getBoolean(nextKey);
                    }
                    if (nextKey.equals("selectedPage")) {
                        selectedPage = jsonObject.getInt(nextKey);
                    }
                    if (nextKey.equals("leftCornerX")) {
                        leftCornerX = jsonObject.getJsonNumber(nextKey).bigDecimalValue().floatValue();
                    }
                    if (nextKey.equals("leftCornerY")) {
                        leftCornerY = jsonObject.getJsonNumber(nextKey).bigDecimalValue().floatValue();
                    }
                    if (nextKey.equals("width")) {
                        width = jsonObject.getJsonNumber(nextKey).bigDecimalValue().floatValue();
                    }
                    if (nextKey.equals("height")) {
                        height = jsonObject.getJsonNumber(nextKey).bigDecimalValue().floatValue();
                    }
                }

                if (pdfAsBytes == null) {
                    objectBuilder.add("-99", "no pdf in the payload");
                    jsonObject = objectBuilder.build();
                    ctx.result(jsonObject.toString()).status(HttpURLConnection.HTTP_BAD_REQUEST);
                }
                InputStream stream = new ByteArrayInputStream(pdfAsBytes);

                PdfExtractorByRegion pibr = new PdfExtractorByRegion();
                SheetModel data = pibr.extractTextFromRegionInPdf(stream, fileName, allPages, selectedPage, leftCornerX, leftCornerY, width, height);

                byte[] byteArray = APIController.byteArraySerializerForAnyObject(data);

                ctx.result(byteArray).status(HttpURLConnection.HTTP_OK);
            }
        });
        return app;
    }
}
