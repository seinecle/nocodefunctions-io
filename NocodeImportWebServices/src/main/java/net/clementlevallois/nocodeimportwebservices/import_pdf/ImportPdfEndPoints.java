package net.clementlevallois.nocodeimportwebservices.import_pdf;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.util.NaiveRateLimit;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.clementlevallois.functions.model.FunctionPdfRegionExtract;
import net.clementlevallois.importers.import_pdf.controller.PdfImporter;
import net.clementlevallois.importers.import_pdf.controller.PdfExtractorByRegion;
import net.clementlevallois.importers.import_pdf.controller.PdfToPngConverter;
import net.clementlevallois.importers.model.CellRecord;
import net.clementlevallois.importers.model.ImagesPerFile;
import net.clementlevallois.importers.model.SheetModel;
import net.clementlevallois.nocodeimportwebservices.APIController;
import static net.clementlevallois.nocodeimportwebservices.APIController.increment;
import net.clementlevallois.nocodeimportwebservices.SynchronizedFileWrite;
import org.openide.util.Exceptions;

/**
 *
 * @author LEVALLOIS
 */
public class ImportPdfEndPoints {

    public static void main(String[] args) {
    }

    public static Javalin addAll(Javalin app) throws Exception {

        app.get("/api/import/pdf/simpleLines", ctx -> {
            increment();

            String jobId = ctx.queryParam("jobId");
            String fileUniqueId = ctx.queryParam("fileUniqueId");

            Path tempDataPath = APIController.globals.getInputFileCompletePath(jobId, fileUniqueId);
            if (Files.exists(tempDataPath)) {
                InputStream is = null; // Declare outside try-with-resources for explicit closing if needed
                try {
                    is = new FileInputStream(tempDataPath.toFile());
                    PdfImporter pdfImporter = new PdfImporter();
                    String lines = pdfImporter.importPdfFileToSimpleLines(is);
                    SynchronizedFileWrite.concurrentWriting(APIController.globals.getInputDataPath(jobId), lines);
                    try {
                        is.close();
                    } catch (IOException e) {
                        System.err.println("Error closing InputStream: " + e.getMessage());
                    }
                    ctx.result("OK").status(HttpURLConnection.HTTP_OK);
                } catch (IOException e) {
                    System.err.println("Error processing PDF import: " + e.getMessage());
                    ctx.result("Error processing file: " + e.getMessage()).status(HttpURLConnection.HTTP_INTERNAL_ERROR);
                } finally {
                }
            } else {
                ctx.result("file not found for job " + jobId).status(HttpURLConnection.HTTP_BAD_REQUEST);
            }
        });
        app.get("/api/import/pdf/linesPerPage", ctx -> {
            increment();
            String fileName = ctx.queryParam("fileName");
            String fileUniqueId = ctx.queryParam("fileUniqueId");
            String jobId = ctx.queryParam("jobId");
            String localizedEmptyLineMessage = ctx.queryParam("localizedEmptyLineMessage");

            Path tempDataPath = APIController.globals.getInputFileCompletePath(jobId, fileUniqueId);
            if (Files.exists(tempDataPath)) {
                try (InputStream is = new FileInputStream(tempDataPath.toFile())) {
                    PdfImporter pdfImporter = new PdfImporter();
                    List<SheetModel> sheets = pdfImporter.importPdfFileToLinesPerPage(is, fileName, localizedEmptyLineMessage);
                    var sheetsBytes = APIController.byteArraySerializerForAnyObject(sheets);
//                    Files.write(APIController.globals.getDataSheetPath(jobId, fileUniqueId), sheetsBytes);

                }
                ctx.result("ok").status(HttpURLConnection.HTTP_OK);
            } else {
                ctx.result("file not found for job " + jobId).status(HttpURLConnection.HTTP_BAD_REQUEST);
            }

        });

        app.get("/api/import/pdf/return-png", ctx -> {
            increment();
            String jobId = ctx.queryParam("jobId");
            String fileUniqueId = ctx.queryParam("fileUniqueId");
            String fileName = ctx.queryParam("fileName");

            var inputFilePath = APIController.globals.getInputFileCompletePath(jobId, fileUniqueId);
            byte[] fileBytes = Files.readAllBytes(inputFilePath);
            try (InputStream is = new ByteArrayInputStream(fileBytes)) {
                PdfToPngConverter pdfToPngConverter = new PdfToPngConverter();
                byte[][] images = pdfToPngConverter.convertPdfFileToPngs(is);
                ImagesPerFile ipf = new ImagesPerFile();
                ipf.setImages(images);
                ipf.setFileName(fileName);
                var imageBytes = APIController.byteArraySerializerForAnyObject(ipf);
                Files.write(APIController.globals.getPngPath(jobId, fileUniqueId), imageBytes);
            }
            ctx.result("OK").status(HttpURLConnection.HTTP_OK);
        });

        app.get("/api/import/pdf/extract-region", ctx -> {
            String owner = ctx.queryParam("owner");
            if (owner == null || !owner.equals(APIController.pwdOwner)) {
                NaiveRateLimit.requestPerTimeUnit(ctx, 50, TimeUnit.SECONDS);
                APIController.increment();
            }

            PdfExtractionRequest request = new PdfExtractionRequest();
            JsonObjectBuilder errorBuilder = Json.createObjectBuilder();

            if (!parseBody(ctx, request, errorBuilder)) {
                ctx.result(errorBuilder.build().toString()).status(HttpURLConnection.HTTP_BAD_REQUEST);
                return;
            }

            parseQueryParams(ctx, request);

            if (request.getPdfAsBytes() == null) {
                errorBuilder.add("-99", "no pdf in the payload");
                ctx.result(errorBuilder.build().toString()).status(HttpURLConnection.HTTP_BAD_REQUEST);
                return;
            }

            byte[] byteArray;
            try (InputStream stream = new ByteArrayInputStream(request.getPdfAsBytes())) {
                PdfExtractorByRegion pibr = new PdfExtractorByRegion();
                SheetModel data = pibr.extractTextFromRegionInPdf(
                        stream,
                        request.getFileName(),
                        request.isAllPages(),
                        request.getSelectedPage(),
                        request.getLeftCornerX(),
                        request.getLeftCornerY(),
                        request.getWidth(),
                        request.getHeight()
                );
                byteArray = APIController.byteArraySerializerForAnyObject(data);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
                errorBuilder.add("-99", "Internal Server Error during PDF extraction: " + ex.getMessage());
                ctx.result(errorBuilder.build().toString()).status(HttpURLConnection.HTTP_INTERNAL_ERROR);
                return;
            }

            ctx.result(byteArray).status(HttpURLConnection.HTTP_OK);
        });
        return app;
    }

    /**
     * Parses the JSON request body and populates the PdfExtractionRequest
     * object. Uses a switch expression on the BodyParams enum for
     * exhaustiveness.
     *
     * @param ctx The Javalin context.
     * @param request The PdfExtractionRequest object to populate.
     * @param errorBuilder The JsonObjectBuilder to add error messages to.
     * @return true if parsing was successful and required fields are present,
     * false otherwise.
     */
    private static boolean parseBody(Context ctx, PdfExtractionRequest request, JsonObjectBuilder errorBuilder) {
        try {
            String body = new String(ctx.bodyAsBytes(), StandardCharsets.US_ASCII);
            if (body.isEmpty()) {
                errorBuilder.add("-99", "body of the request should not be empty");
                return false;
            }

            JsonReader jsonReader = Json.createReader(new StringReader(body));
            JsonObject jsonObject = jsonReader.readObject();

            for (var entry : jsonObject.entrySet()) {
                String key = entry.getKey();
                Optional<FunctionPdfRegionExtract.BodyParams> bp = APIController.enumValueOf(FunctionPdfRegionExtract.BodyParams.class, key);

                if (bp.isPresent()) {
                    Consumer<String> bodyParamHandler = switch (bp.get()) {
                        case PDF_BYTES ->
                            s -> request.setPdfAsBytes(Base64.getDecoder().decode(jsonObject.getString(key)));
                        case FILE_NAME ->
                            s -> request.setFileName(jsonObject.getString(key));
                    };
                    bodyParamHandler.accept(key);
                } else {
                    System.out.println("PdfExtractEndPoint: Unknown body parameter key: " + key);
                }
            }
        } catch (Exception e) {
            errorBuilder.add("-99", "Failed to parse request body: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Parses the query parameters and populates the PdfExtractionRequest
     * object. Uses a switch expression on the QueryParams enum for
     * exhaustiveness.
     *
     * @param ctx The Javalin context.
     * @param request The PdfExtractionRequest object to populate.
     */
    private static void parseQueryParams(Context ctx, PdfExtractionRequest request) {
        for (var entry : ctx.queryParamMap().entrySet()) {
            String key = entry.getKey();
            String decodedParamValue = URLDecoder.decode(entry.getValue().getFirst(), StandardCharsets.UTF_8);

            Optional<FunctionPdfRegionExtract.QueryParams> qp = APIController.enumValueOf(FunctionPdfRegionExtract.QueryParams.class, key);

            if (qp.isPresent()) {
                Consumer<String> queryParamHandler = switch (qp.get()) {
                    case OWNER ->
                        request::setOwner;
                    case ALL_PAGES ->
                        s -> request.setAllPages(Boolean.parseBoolean(s));
                    case SELECTED_PAGES ->
                        s -> request.setSelectedPage(Integer.valueOf(s));
                    case LEFT_CORNER_X ->
                        s -> request.setLeftCornerX(Float.valueOf(s));
                    case LEFT_CORNER_Y ->
                        s -> request.setLeftCornerY(Float.valueOf(s));
                    case WIDTH ->
                        s -> request.setWidth(Float.valueOf(s));
                    case HEIGHT ->
                        s -> request.setHeight(Float.valueOf(s));
                };
                queryParamHandler.accept(decodedParamValue);
            } else {
                System.out.println("PdfExtractEndPoint: Unknown query parameter key: " + key);
            }
        }
    }

}
