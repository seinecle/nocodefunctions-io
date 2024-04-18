package net.clementlevallois.io.io_json;

import com.sigpwned.chardet4j.Chardet;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonParsingException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static java.util.stream.Collectors.toList;

/**
 *
 * @author LEVALLOIS
 */
public class JsonImporter {

    public static void main(String[] args) throws IOException {

        Path testFile = Path.of("C:\\Users\\levallois\\OneDrive - Aescra Emlyon Business School\\Bureau\\tests\\import\\text key.json");
        byte[] readAllBytes = Files.readAllBytes(testFile);
        String importJsonFileToSimpleLines = new JsonImporter().importJsonFileToSimpleLines(readAllBytes, "text");
        System.out.println("values: " + importJsonFileToSimpleLines);
    }

    public String importJsonFileToSimpleLines(byte[] bytes, String jsonKey) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        StringBuilder sb = new StringBuilder();
        try {
            List<String> txtLines;
            String jsonAsOneStringOnOneLine;
            Reader chars = Chardet.decode(byteArrayInputStream, StandardCharsets.UTF_8);
            try (BufferedReader br = new BufferedReader(chars)) {
                txtLines = br.lines().collect(toList());
            }
            // if the json file is made of one json object or array cut in many lines, assemble these lines into one unique big String
            if (txtLines.size() > 1 && !txtLines.get(1).isBlank()) {
                StringBuilder sbTemp = new StringBuilder();
                for (String line : txtLines) {
                    sbTemp.append(line);
                }
                jsonAsOneStringOnOneLine = sbTemp.toString();
            } // otherwise, the file is made of one line and this is what we need
            else {
                jsonAsOneStringOnOneLine = txtLines.get(0);
            }
            try {
                JsonValue value = Json.createReader(new StringReader(jsonAsOneStringOnOneLine)).read();
                traverse(value, "", jsonKey, sb); // Start traversal with an empty prefix for the root
            } catch (JsonParsingException e) {
                return "{}";
            }

        } catch (IOException ex) {
            System.out.println("exception: " + ex.getMessage());
        }
        return sb.toString();
    }

    private static void traverse(JsonValue jsonValue, String key, String TARGET_KEY, StringBuilder foundValues) {
        switch (jsonValue.getValueType()) {
            case OBJECT -> {
                JsonObject obj = jsonValue.asJsonObject();
                obj.keySet().forEach(k -> traverse(obj.get(k), key.isEmpty() ? k : key + "." + k, TARGET_KEY, foundValues));
            }
            case ARRAY -> {
                JsonArray array = jsonValue.asJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    traverse(array.get(i), key + "[" + i + "]", TARGET_KEY, foundValues);
                }
            }
            case STRING -> {
                // the name of the key provided by the user is the right most name of the key.
                // so we should check whether the rightmost part of the key name is "." + the name of the key
                if (key.equals(TARGET_KEY) || key.endsWith("." + TARGET_KEY)) {
                    String textualValue = jsonValue.toString();
                    textualValue = textualValue.replaceAll("\\n", ". ");
                    textualValue = textualValue.replaceAll("\\R", ". ");
                    if (textualValue.startsWith("\"")) {
                        textualValue = textualValue.substring(1);
                    }
                    if (textualValue.endsWith("\"")) {
                        textualValue = textualValue.substring(0, textualValue.length() - 1);
                    }
                    foundValues.append(textualValue).append("\n");
                }
            }
            case NUMBER, TRUE, FALSE, NULL -> {
            }
        }
        // Handle other types if necessary
    }

}
