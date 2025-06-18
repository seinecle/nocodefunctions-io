package net.clementlevallois.nocodeimportwebservices;

import io.javalin.Javalin;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.clementlevallois.functions.model.Globals;
import net.clementlevallois.importers.model.SheetModel;
import net.clementlevallois.nocodeimportwebservices.export_xlsx.ExportXlsEndPoints;
import net.clementlevallois.nocodeimportwebservices.import_csv.ImportCsvEndPoints;
import net.clementlevallois.nocodeimportwebservices.import_gexf.ImportGexfEndPoints;
import net.clementlevallois.nocodeimportwebservices.import_html.ImportHtmlEndPoints;
import net.clementlevallois.nocodeimportwebservices.import_json.ImportJsonEndPoints;
import net.clementlevallois.nocodeimportwebservices.import_pdf.ImportPdfEndPoints;
import net.clementlevallois.nocodeimportwebservices.import_txt.ImportTxtEndPoints;
import net.clementlevallois.nocodeimportwebservices.import_xlsx.ImportXlsEndPoints;

/**
 *
 * @author LEVALLOIS
 */
public class APIController {
    private static Javalin app;
    public static String pwdOwner;
    public static Path tempFilesFolder;
    public static Globals globals;

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.load(new FileInputStream("private/props.properties"));

        boolean isLocal = System.getProperty("os.name").toLowerCase().contains("win");
        if (isLocal) {
            tempFilesFolder = Path.of(props.getProperty("pathToTempFilesWindows"));
        } else {
            tempFilesFolder = Path.of(props.getProperty("pathToTempFilesLinux"));
        }
        

        if (!Files.isDirectory(tempFilesFolder)) {
            System.out.println("temp folder in private.properties file does not exist:");
            System.out.println(tempFilesFolder);
            System.out.println("exiting now");
            System.exit(-1);
        }

        globals = new Globals(tempFilesFolder);
        
        
        String port = props.getProperty("port");
        app = Javalin.create(config -> {
            config.http.maxRequestSize = 1000000000;
        }).start(Integer.parseInt(port));

        pwdOwner = props.getProperty("pwdOwner");
        app = ImportCsvEndPoints.addAll(app);
        app = ImportTxtEndPoints.addAll(app);
        app = ImportJsonEndPoints.addAll(app);
        app = ImportPdfEndPoints.addAll(app);
        app = ImportXlsEndPoints.addAll(app);
        app = ImportHtmlEndPoints.addAll(app);
        app = ImportGexfEndPoints.addAll(app);
        app = ExportXlsEndPoints.addAll(app);
        System.out.println("running the api");

    }

    public static void increment() {
        Long epochdays = LocalDate.now().toEpochDay();
        String message = epochdays.toString() + "\n";
        try {
            Files.write(Paths.get("api_calls.txt"), message.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("issue with the api call counter");
            System.out.println(e.getMessage());
        }
    }

    public static byte[] byteArraySerializerForSheets(List<SheetModel> o) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(o);
        oos.flush();
        byte[] data = bos.toByteArray();
        return data;
    }

    public static String turnJsonObjectToString(JsonObject jsonObject) {
        String output = "{}";
        try (java.io.StringWriter stringWriter = new StringWriter()) {
            var jsonWriter = Json.createWriter(stringWriter);
            jsonWriter.writeObject(jsonObject);
            output = stringWriter.toString();
        } catch (IOException ex) {
            Logger.getLogger(APIController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return output;

    }

    public static byte[] byteArraySerializerForAnyObject(Object o) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(o);
        oos.flush();
        byte[] data = bos.toByteArray();
        return data;
    }

    public static <E extends Enum<E>> Optional<E> enumValueOf(Class<E> enumClass, String value) {
        try {
            return Optional.of(Enum.valueOf(enumClass, value.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

}
