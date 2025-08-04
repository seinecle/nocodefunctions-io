package net.clementlevallois.nocodeimportwebservices.import_xlsx;

import io.javalin.Javalin;
import jakarta.json.JsonObject;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import net.clementlevallois.functions.model.WorkflowCoocProps;
import net.clementlevallois.importers.model.SheetModel;
import net.clementlevallois.io.xlsx.ExcelReader;
import net.clementlevallois.nocodeimportwebservices.APIController;
import static net.clementlevallois.nocodeimportwebservices.APIController.increment;

/**
 *
 * @author LEVALLOIS
 */
public class ImportXlsEndPoints {

    public static void main(String[] args) {
    }

    public static Javalin addAll(Javalin app) throws Exception {

        app.get("/api/import/xlsx/sheetModel", ctx -> {
            increment();
            String jobId = ctx.queryParam("jobId");
            String fileUniqueId = ctx.queryParam("fileUniqueId");
            Path tempDataPath = APIController.globals.getInputFileCompletePath(jobId, fileUniqueId);
            if (Files.exists(tempDataPath)) {
                byte[] fileBytes = Files.readAllBytes(tempDataPath);
                List<SheetModel> listsOfSheets = ExcelReader.readExcelFileToSheets(fileBytes);
                byte[] byteArray = APIController.byteArraySerializerForSheets(listsOfSheets);
                Files.write(APIController.globals.getDataSheetPath(jobId), byteArray);
                Files.deleteIfExists(tempDataPath);
                ctx.result("OK").status(HttpURLConnection.HTTP_OK);
            }else{
                System.out.println("uploaded file not found in sheetModel import endpoint");
                ctx.result("uploaded file not found in sheetModel import endpoint").status(HttpURLConnection.HTTP_BAD_REQUEST);
            }
        }
        );

        return app;
    }
}
