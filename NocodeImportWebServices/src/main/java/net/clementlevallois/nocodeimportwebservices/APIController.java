/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.clementlevallois.importers.model.SheetModel;
import net.clementlevallois.nocodeimportwebservices.export_xlsx.ExportXlsEndPoints;
import net.clementlevallois.nocodeimportwebservices.import_csv.ImportCsvEndPoints;
import net.clementlevallois.nocodeimportwebservices.import_pdf.ImportPdfEndPoints;
import net.clementlevallois.nocodeimportwebservices.import_txt.ImportTxtEndPoints;
import net.clementlevallois.nocodeimportwebservices.import_xlsx.ImportXlsEndPoints;

/**
 *
 * @author LEVALLOIS
 */
public class APIController {

    /**
     * @param args the command line arguments
     */
    private static Javalin app;
    public static String pwdOwner;

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.load(new FileInputStream("private/props.properties"));

        String port = props.getProperty("port");
        app = Javalin.create(config -> {
            config.http.maxRequestSize = 1000000000;
        }).start(Integer.parseInt(port));

        pwdOwner = props.getProperty("pwdOwner");
        String twitterClientId = props.getProperty("twitter_client_id");
        String twitterClientSecret = props.getProperty("twitter_client_secret");

//        TwitterApi twitterApiInstance;
//        TwitterCredentialsOAuth2 twitterApiCredentials;
//        twitterApiCredentials = new TwitterCredentialsOAuth2(twitterClientId,
//                twitterClientSecret, "", "");
//        twitterApiInstance = new TwitterApi();
//
//        TwitterOAuth20Service twitterOAuthService = new TwitterOAuth20Service(
//                twitterApiCredentials.getTwitterOauth2ClientId(),
//                twitterApiCredentials.getTwitterOAuth2ClientSecret(),
//                "https://test.nocodefunctions.com/twitter_auth.html",
//                "offline.access tweet.read users.read");
//
//        app = TweetRetrieverEndPoints.addAll(app, twitterApiInstance, twitterApiCredentials, twitterOAuthService);
        app = ImportCsvEndPoints.addAll(app);
        app = ImportTxtEndPoints.addAll(app);
        app = ImportPdfEndPoints.addAll(app);
        app = ImportXlsEndPoints.addAll(app);
        app = ExportXlsEndPoints.addAll(app);
        System.out.println("running the api");

    }

    public static void increment() {
        Long epochdays = LocalDate.now().toEpochDay();
        String message = epochdays.toString() + "\n";
        try {
            Files.write(Paths.get("api_calls.txt"), message.getBytes(), StandardOpenOption.APPEND);
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

}
