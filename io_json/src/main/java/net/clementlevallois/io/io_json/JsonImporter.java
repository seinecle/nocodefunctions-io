package net.clementlevallois.io.io_json;

import com.sigpwned.chardet4j.Chardet;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParsingException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

/**
     * Imports a JSON object or array from a byte array into a JSON array stored in a file.
     *
     * - If the target file does not exist, it creates a new JSON array and adds the provided JSON structure as the first element.
     * - If the target file already exists, it reads the existing JSON array, appends the new JSON structure, and overwrites the file.
     * - The method is synchronized to ensure thread-safe file access.
     *
     * @param jsonObjectOrArrayAsBytes A byte array representing either a JsonObject or a JsonArray.
     * @param jsonArrayPersistedAsFile The path to the file where the JSON array is or will be stored.
     * persists the appended JsonArray to the provided path
     * @throws IOException If an I/O error occurs during file reading or writing.
     */
    public synchronized void appendToPersistedJsonArray(byte[] jsonObjectOrArrayAsBytes, Path jsonArrayPersistedAsFile) throws IOException {
        JsonReaderFactory readerFactory = Json.createReaderFactory(null);
        JsonArray existingArray;

        // Step 1: Check if the file exists and read its content.
        if (Files.exists(jsonArrayPersistedAsFile) && Files.size(jsonArrayPersistedAsFile) > 0) {
            try (InputStream fis = Files.newInputStream(jsonArrayPersistedAsFile);
                 JsonReader jsonReader = readerFactory.createReader(fis)) {
                // Read the root structure. It should be a JsonArray.
                JsonValue readValue = jsonReader.readValue();
                if (readValue.getValueType() == JsonValue.ValueType.ARRAY) {
                    existingArray = (JsonArray) readValue;
                } else {
                    // If the file contains something other than an array, we handle it as an error
                    // or decide on a recovery strategy. Here, we'll start a new array.
                    System.err.println("Warning: File does not contain a valid JSON array. A new array will be created.");
                    existingArray = Json.createArrayBuilder().build();
                }
            } catch (JsonException e) {
                // Handle cases where the file is corrupt or not valid JSON.
                System.err.println("Warning: Could not parse existing file content. A new array will be created. Error: " + e.getMessage());
                existingArray = Json.createArrayBuilder().build();
            }
        } else {
            // If the file doesn't exist or is empty, start with an empty array.
            existingArray = Json.createArrayBuilder().build();
        }

        // Step 2: Parse the input byte array to get the new JSON value.
        JsonValue newJsonValue;
        try (JsonReader jsonReader = readerFactory.createReader(new ByteArrayInputStream(jsonObjectOrArrayAsBytes))) {
            newJsonValue = jsonReader.readValue();
        }

        // Step 3: Create a new JsonArrayBuilder, add all existing elements, then the new one.
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (JsonValue value : existingArray) {
            arrayBuilder.add(value);
        }
        arrayBuilder.add(newJsonValue);
        JsonArray finalArray = arrayBuilder.build();

        // Step 4: Write the final array back to the file, overwriting it.
        // We use pretty printing for readability.
        Map<String, Object> properties = new HashMap<>(1);
        properties.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory writerFactory = Json.createWriterFactory(properties);

        try (OutputStream fos = Files.newOutputStream(jsonArrayPersistedAsFile);
             JsonWriter jsonWriter = writerFactory.createWriter(fos, StandardCharsets.UTF_8)) {
            jsonWriter.writeArray(finalArray);
        }
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
