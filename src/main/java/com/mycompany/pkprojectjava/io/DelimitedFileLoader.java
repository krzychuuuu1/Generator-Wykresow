// -- Notes for fast understanding and rember --
// ---------------------------------------------
//              
// ---------------------------------------------

package com.mycompany.pkprojectjava.io;

import javafx.beans.property.*;
import com.mycompany.pkprojectjava.model.ColumnDefinition;
import com.mycompany.pkprojectjava.model.DataRow;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility class responsible for loading delimited text files (e.g. CSV, TSV)
 * into a structured in-memory representation.
 * <p>
 * The loader performs the following operations:
 * <ul>
 *     <li>Reads the first line as a header defining column names</li>
 *     <li>Infers column data types (Integer, Double, or String)</li>
 *     <li>Builds {@link DataRow} objects backed by JavaFX {@link Property} instances</li>
 * </ul>
 * </p>
 *
 * <p>
 * Type inference is conservative and based on regular-expression matching
 * across all non-empty values in each column.
 * </p>
 */
public class DelimitedFileLoader {

    /** Regular expression for detecting integer values. */
    private static final Pattern INT = Pattern.compile("-?\\d+");

    /** Regular expression for detecting floating-point values. */
    private static final Pattern DOUBLE = Pattern.compile("-?\\d+\\.\\d+");

    /**
     * Loads a delimited file and converts it into a schema and data rows.
     *
     * @param file      the input file to load
     * @param delimiter the column delimiter (e.g. ",", "\\t", ";")
     * @return a {@link Result} object containing the detected schema and data rows
     * @throws IOException if the file cannot be read or is empty
     */
    public static Result load(File file, String delimiter) throws IOException {

        try (BufferedReader rd = new BufferedReader(new FileReader(file))) {

            String header = rd.readLine(); // read first row, first line
            if (header == null) {
                throw new IOException("Empty file");
            }

            String[] columns = header.split(delimiter); // divide first, header line into columns knowing which character divide columns

            List<String[]> rawRows = new ArrayList<>(); // make array of rows, the rest of lines of file
            String line;
            while ((line = rd.readLine()) != null) {
                rawRows.add(line.split(delimiter, -1)); // add to list of array new line
            }

            List<ColumnDefinition> schema =
                    detectSchema(columns, rawRows);

            List<DataRow> rows =
                    buildRows(schema, rawRows);

            return new Result(schema, rows);
        }
    }

    /**
     * Infers the data type of each column by inspecting all non-empty values.
     *
     * <p>
     * The inference rules are applied in the following order:
     * <ol>
     *     <li>Integer</li>
     *     <li>Double</li>
     *     <li>String (fallback)</li>
     * </ol>
     * </p>
     *
     * @param columns the column names from the header row
     * @param rows    the raw string data rows
     * @return a list of {@link ColumnDefinition} objects describing the schema
     */
    private static List<ColumnDefinition> detectSchema(String[] columns, List<String[]> rows) {

        List<ColumnDefinition> result = new ArrayList<>();

        for (int col = 0; col < columns.length; col++) {
            Class<?> type = String.class;

            for (String[] row : rows) {
                if (col >= row.length) {
                    continue;
                }

                String v = row[col].trim();
                if (v.isEmpty()) {
                    continue;
                }

                if (INT.matcher(v).matches()) {
                    type = Integer.class;
                } else if (DOUBLE.matcher(v).matches()) {
                    type = Double.class;
                } else {
                    type = String.class;
                    break;
                }
            }
            result.add(new ColumnDefinition(columns[col].trim(), type));
        }
        return result;
    }

    /**
     * Constructs {@link DataRow} objects from raw string values using
     * the provided schema.
     *
     * @param schema  the detected column schema
     * @param rawRows the raw string rows read from the file
     * @return a list of populated {@link DataRow} instances
     */
    private static List<DataRow> buildRows(List<ColumnDefinition> schema, List<String[]> rawRows) {

        List<DataRow> rows = new ArrayList<>();

        for (String[] raw : rawRows) {
            DataRow row = new DataRow();

            for (int i = 0; i < schema.size(); i++) {
                ColumnDefinition col = schema.get(i);
                String value = i < raw.length ? raw[i].trim() : "";
                row.put(col.getName(), createProperty(col.getType(), value));
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * Creates a JavaFX {@link Property} instance appropriate for the given type.
     *
     * <p>
     * Empty values are converted to default numeric values (0 or 0.0)
     * or empty strings.
     * </p>
     *
     * @param type the expected Java type of the value
     * @param v    the raw string value
     * @return a JavaFX {@link Property} wrapping the parsed value
     */
    private static Property<?> createProperty(Class<?> type, String v) {
        if (type == Integer.class) {
            return new SimpleIntegerProperty(
                    v.isEmpty() ? 0 : Integer.parseInt(v));
        }
        if (type == Double.class) {
            return new SimpleDoubleProperty(
                    v.isEmpty() ? 0.0 : Double.parseDouble(v));
        }
        return new SimpleStringProperty(v);
    }

    /**
     * Container class holding the result of a delimited file load operation.
     *
     * <p>
     * This class is Java 11–compatible and intentionally simple, providing
     * read-only access to schema and row data.
     * </p>
     */
    public static class Result {

        /** The detected column schema. */
        private final List<ColumnDefinition> schema;

        /** The loaded data rows. */
        private final List<DataRow> rows;

        /**
         * Creates a new result instance.
         *
         * @param schema the detected schema
         * @param rows   the loaded data rows
         */
        public Result(List<ColumnDefinition> schema,List<DataRow> rows) {
            this.schema = schema;
            this.rows = rows;
        }

        /**
         * Returns the column schema.
         *
         * @return the list of {@link ColumnDefinition} objects
         */
        public List<ColumnDefinition> getSchema() {
            return schema;
        }

        /**
         * Returns the loaded data rows.
         *
         * @return the list of {@link DataRow} instances
         */
        public List<DataRow> getRows() {
            return rows;
        }
    }
}
