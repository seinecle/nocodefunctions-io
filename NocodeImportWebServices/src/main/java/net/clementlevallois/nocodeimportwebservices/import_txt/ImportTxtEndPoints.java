package net.clementlevallois.nocodeimportwebservices.import_txt;

import io.javalin.Javalin;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.util.List;
import net.clementlevallois.importers.import_txt.controller.TxtImporter;
import net.clementlevallois.importers.model.SheetModel;
import net.clementlevallois.nocodeimportwebservices.APIController;
import static net.clementlevallois.nocodeimportwebservices.APIController.increment;
import net.clementlevallois.nocodeimportwebservices.SynchronizedFileWrite;

/**
 *
 * @author LEVALLOIS
 */
public class ImportTxtEndPoints {

    public static void main(String[] args) {
    }

    public static Javalin addAll(Javalin app) throws Exception {

        app.post("/api/import/txt_cooc", ctx -> {
            increment();
            String fileUniqueId = ctx.queryParam("fileUniqueId");
            String jobId = ctx.queryParam("jobId");
            String fileName = ctx.queryParam("fileName");

            var inputFilePath = APIController.globals.getInputFileCompletePath(jobId, fileUniqueId);

            byte[] fileBytes = Files.readAllBytes(inputFilePath);
            TxtImporter txtImporter = new TxtImporter();
            List<SheetModel> sheets = txtImporter.importCooccurencesInTextFile(fileBytes, fileName);

            byte[] byteArray = APIController.byteArraySerializerForSheets(sheets);
            Files.write(APIController.globals.getInputDataPath(jobId), byteArray);
            Files.deleteIfExists(inputFilePath);

            ctx.result("OK").status(HttpURLConnection.HTTP_OK);
        });

        app.get("/api/import/txt/simpleLines", ctx -> {
            increment();
            String jobId = ctx.queryParam("jobId");
            String fileUniqueId = ctx.queryParam("fileUniqueId");

            var inputFilePath = APIController.globals.getInputFileCompletePath(jobId, fileUniqueId);

            byte[] fileBytes = Files.readAllBytes(inputFilePath);
            TxtImporter txtImporter = new TxtImporter();
            String lines = txtImporter.importSimpleLinesInTextFile(fileBytes);
            SynchronizedFileWrite.concurrentWriting(APIController.globals.getInputDataPath(jobId), lines);
            Files.deleteIfExists(inputFilePath);

            ctx.result("OK").status(HttpURLConnection.HTTP_OK);

        });

        return app;
    }

}
