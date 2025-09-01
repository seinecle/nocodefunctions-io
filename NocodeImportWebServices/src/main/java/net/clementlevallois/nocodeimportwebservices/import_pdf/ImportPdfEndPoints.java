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
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.clementlevallois.functions.model.FunctionRegionExtract;
import net.clementlevallois.functions.model.Globals;
import static net.clementlevallois.functions.model.Globals.GlobalQueryParams.CALLBACK_URL;
import static net.clementlevallois.functions.model.Globals.GlobalQueryParams.JOB_ID;
import net.clementlevallois.importers.import_pdf.controller.PdfImporter;
import net.clementlevallois.importers.import_pdf.controller.PdfExtractorByRegion;
import net.clementlevallois.importers.import_pdf.controller.PdfToPngConverter;
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

        app.get("/api/import/pdf/pages-to-png", ctx -> {
            increment();
            String jobId = ctx.queryParam("jobId");
            String fileName = ctx.queryParam("fileName");

            var inputFilePath = APIController.globals.getJobDirectory(jobId).resolve(fileName);
            byte[] fileBytes = Files.readAllBytes(inputFilePath);
            try (InputStream is = new ByteArrayInputStream(fileBytes)) {
                PdfToPngConverter pdfToPngConverter = new PdfToPngConverter();
                byte[][] images = pdfToPngConverter.convertPdfFileToPngs(is);
                ImagesPerFile ipf = new ImagesPerFile();
                ipf.setImages(images);
                ipf.setFileName(fileName);
                var imageBytes = APIController.byteArraySerializerForAnyObject(ipf);
                Files.write(APIController.globals.getAllPngPath(jobId), imageBytes);
            }
            ctx.result("OK").status(HttpURLConnection.HTTP_OK);
        });

        app.get("/api/import/" + FunctionRegionExtract.ENDPOINT, ctx -> {
            try {
                String owner = ctx.queryParam("owner");
                if (owner == null || !owner.equals(APIController.pwdOwner)) {
                    NaiveRateLimit.requestPerTimeUnit(ctx, 50, TimeUnit.SECONDS);
                    APIController.increment();
                }

                PdfExtractionRequest request = new PdfExtractionRequest();
                JsonObjectBuilder errorBuilder = Json.createObjectBuilder();

                parseQueryParams(ctx, request);

                Path jobDir = APIController.globals.getJobDirectory(request.getJobId());
                @SuppressWarnings("unchecked")
                List<Path> uploadedFiles = new ArrayList();
                try (var stream = Files.list(jobDir)) {
                    uploadedFiles = stream
                            .filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().startsWith(Globals.UPLOADED_FILE_PREFIX))
                            .toList();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
                ConcurrentHashMap<String, SheetModel> results = new ConcurrentHashMap();
                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                    for (Path path : uploadedFiles) {
                        scope.fork(() -> {
                            String fileNameWithoutPrefix = path.getFileName().toString().replace(Globals.UPLOADED_FILE_PREFIX, "");
                            try (InputStream is = Files.newInputStream(path)) {
                                PdfExtractorByRegion pibr = new PdfExtractorByRegion();
                                SheetModel data = pibr.extractTextFromRegionInPdf(
                                        is,
                                        fileNameWithoutPrefix,
                                        request.isAllPages(),
                                        request.getSelectedPage(),
                                        request.getLeftCornerX(),
                                        request.getLeftCornerY(),
                                        request.getWidth(),
                                        request.getHeight()
                                );
                                results.put(fileNameWithoutPrefix, data);
                                return null;
                            } catch (IOException ex) {
                                throw ex;
                            }
                        });
                    }

                    scope.join();
                    scope.throwIfFailed();  // rethrow first failure (cancels siblings)
                } catch (Exception e) {
                    Exceptions.printStackTrace(e);
                    errorBuilder.add("-99", "Internal Server Error during PDF extraction: " + e.getMessage());
                    ctx.result(errorBuilder.build().toString()).status(HttpURLConnection.HTTP_INTERNAL_ERROR);
                    return;
                }

                byte[] bytes = APIController.byteArraySerializerForAnyObject(results);
                Path out = jobDir.resolve(request.getJobId() + Globals.GLOBAL_RESULTS_BYTE_FILE_EXTENSION);
                try {
                    Files.write(out, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                } catch (IOException e) {
                    Exceptions.printStackTrace(e);
                    errorBuilder.add("-99", "Internal Server Error writing result.bytes: " + e.getMessage());
                    ctx.result(errorBuilder.build().toString()).status(HttpURLConnection.HTTP_INTERNAL_ERROR);
                }

            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        });
        return app;
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

            Optional<FunctionRegionExtract.QueryParams> qp = APIController.enumValueOf(FunctionRegionExtract.QueryParams.class, key);
            Optional<Globals.GlobalQueryParams> gqp = APIController.enumValueOf(Globals.GlobalQueryParams.class, key);

            if (qp.isPresent()) {
                Consumer<String> queryParamHandler = switch (qp.get()) {
                    case FILE_NAME_PREFIX ->
                        request::setFileNamePrefix;
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
            } else if (gqp.isPresent()) {
                Consumer<String> gqpHandler = switch (gqp.get()) {
                    case CALLBACK_URL ->
                        request::setCallbackURL;
                    case JOB_ID ->
                        request::setJobId;
                };
                gqpHandler.accept(decodedParamValue);
            } else {
                System.out.println("PdfExtractEndPoint: Unknown query parameter key: " + key);
            }
        }
    }

}
