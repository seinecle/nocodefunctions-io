package net.clementlevallois.nocodeimportwebservices.import_csv;

import io.javalin.Javalin;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.clementlevallois.importers.import_csv.controller.CsvImporter;
import net.clementlevallois.importers.model.SheetModel;
import net.clementlevallois.nocodeimportwebservices.APIController;
import static net.clementlevallois.nocodeimportwebservices.APIController.increment;
import net.clementlevallois.nocodeimportwebservices.SynchronizedFileWrite;

/**
 *
 * @author LEVALLOIS
 */
public class ImportCsvEndPoints {

    public static void main(String[] args) {
    }

    public static Javalin addAll(Javalin app) throws Exception {

        app.post("/api/import/csv", ctx -> {
            increment();
            String jobId = ctx.queryParam("jobId");
            String fileName = ctx.queryParam("fileName");
            Path tempDataPath = APIController.globals.getInputDataPath(jobId);
            if (Files.exists(tempDataPath)) {
                try {
                    byte[] fileBytes = Files.readAllBytes(tempDataPath);

                    CsvImporter csvImporter = new CsvImporter();
                    List<SheetModel> sheets = csvImporter.importCsvFile(fileBytes, fileName);

                    byte[] byteArray = APIController.byteArraySerializerForSheets(sheets);
                    Files.write(APIController.globals.getDataSheetPath(jobId, jobId), byteArray);
                    Files.deleteIfExists(tempDataPath);

                    ctx.result(byteArray).status(HttpURLConnection.HTTP_OK);
                } catch (IOException e) {
                    ctx.status(HttpURLConnection.HTTP_INTERNAL_ERROR).result("Error processing file.");
                }
            }
        }
        );

        app.post("/api/import/csv/simpleLines", ctx -> {
            increment();
            String jobId = ctx.queryParam("jobId");
            String fileUniqueId = ctx.queryParam("fileUniqueId");
            boolean hasHeaders = Boolean.parseBoolean(ctx.queryParam("hasHeaders"));
            int colIndex = Integer.parseInt(ctx.queryParam("colIndex"));
            Path tempDataPath = APIController.globals.getInputFileCompletePath(jobId, fileUniqueId);
            if (Files.exists(tempDataPath)) {
                    byte[] fileBytes = Files.readAllBytes(tempDataPath);
                    CsvImporter csvImporter = new CsvImporter();
                    String lines = csvImporter.importCsvFileToSimpleLines(fileBytes, colIndex, hasHeaders);
                    SynchronizedFileWrite.concurrentWriting(APIController.globals.getInputDataPath(jobId), lines);
                    Files.deleteIfExists(APIController.globals.getInputFileCompletePath(jobId, fileUniqueId));
                    ctx.result("OK").status(HttpURLConnection.HTTP_OK);
            }
        }
        );

        return app;
    }

}
