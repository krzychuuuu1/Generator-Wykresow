package com.mycompany.pkprojectjava.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.pkprojectjava.io.DelimitedFileLoader.Result;
import com.mycompany.pkprojectjava.model.ColumnDefinition;
import com.mycompany.pkprojectjava.model.DataRow;
import javafx.beans.property.*;

import java.io.*;
import java.util.*;

/**
 * Utility class for reading and writing table data in JSON format.
 */
public class JsonTableIO {

    /**
     * Gson instance for JSON serialization/deserialization.
     */
    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    /**
     * Saves the provided schema and data to a JSON file.
     *
     * @param file   the target file to save to
     * @param schema the list of column definitions
     * @param data   the list of data rows to save
     * @throws IOException if an I/O error occurs while writing the file
     */
    public static void save(
            File file,
            List<ColumnDefinition> schema,
            List<DataRow> data) throws IOException {

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", schema);
        root.put("rows", extractRows(data));

        try (Writer w = new FileWriter(file)) {
            GSON.toJson(root, w);
        }
    }

    /**
     * Loads table data from a JSON file.
     *
     * @param file the source file to load from
     * @return a {@link Result} object containing the loaded schema and data rows
     * @throws IOException if an I/O error occurs while reading the file
     */
    public static Result load(File file) throws IOException {

        try (Reader r = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();

            ColumnDefinition[] schemaArr =
                    GSON.fromJson(root.get("schema"),
                            ColumnDefinition[].class);

            List<ColumnDefinition> schema =
                    List.of(schemaArr);

            List<DataRow> rows = new ArrayList<>();
            for (JsonElement e : root.getAsJsonArray("rows")) {
                DataRow row = new DataRow();
                JsonObject obj = e.getAsJsonObject();

                for (ColumnDefinition col : schema) {
                    row.put(
                            col.getName(),
                            createProperty(
                                    col.getType(),
                                    obj.get(col.getName())));
                }
                rows.add(row);
            }

            return new Result(schema, rows);
        }
    }

    /**
     * Extracts raw values from a list of {@link DataRow} objects for JSON serialization.
     *
     * @param data the list of data rows
     * @return a list of maps containing raw column-to-value mappings
     */
    private static List<Map<String, Object>> extractRows(List<DataRow> data) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (DataRow row : data) {
            Map<String, Object> m = new LinkedHashMap<>();
            row.getAll().forEach((k, v) -> m.put(k, v.getValue()));
            list.add(m);
        }
        return list;
    }

    /**
     * Creates a JavaFX {@link Property} from a {@link JsonElement} based on the expected type.
     *
     * @param type the expected Java type of the value
     * @param e    the JSON element containing the value
     * @return a JavaFX {@link Property} wrapping the parsed value
     */
    private static Property<?> createProperty(
            Class<?> type, JsonElement e) {

        if (type == Integer.class)
            return new SimpleIntegerProperty(e.getAsInt());
        if (type == Double.class)
            return new SimpleDoubleProperty(e.getAsDouble());
        return new SimpleStringProperty(e.getAsString());
    }

//    public record Result(
//            List<ColumnDefinition> schema,
//            List<DataRow> rows) {
//        return null;
//    }
}
