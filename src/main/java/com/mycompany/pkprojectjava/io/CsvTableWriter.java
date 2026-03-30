package com.mycompany.pkprojectjava.io;

import com.mycompany.pkprojectjava.model.ColumnDefinition;
import com.mycompany.pkprojectjava.model.DataRow;
import javafx.beans.property.Property;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Utility class for writing table data to CSV files.
 */
public class CsvTableWriter {

    /**
     * Writes the provided schema and data to a CSV file.
     *
     * @param file   the target file to write to
     * @param schema the list of column definitions
     * @param data   the list of data rows to write
     */
    public static void write(
            File file,
            List<ColumnDefinition> schema,
            List<DataRow> data) {

        validate(schema, data);

        try (BufferedWriter writer =
                     new BufferedWriter(
                             new OutputStreamWriter(
                                     new FileOutputStream(file),
                                     StandardCharsets.UTF_8))) {

            // header
            for (int i = 0; i < schema.size(); i++) {
                writer.write(schema.get(i).getName());
                if (i < schema.size() - 1) writer.write(";");
            }
            writer.newLine();

            // rows
            for (DataRow row : data) {
                int i = 0;
                for (ColumnDefinition col : schema) {
                    Property<?> p = row.get(col.getName());
                    writer.write(p == null ? "" : p.getValue().toString());
                    if (i++ < schema.size() - 1) writer.write(";");
                }
                writer.newLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Validates that each data row contains the correct number of columns as defined by the schema.
     *
     * @param schema the list of column definitions
     * @param data   the list of data rows to validate
     * @throws IllegalStateException if a row's column count does not match the schema size
     */
    private static void validate(
            List<ColumnDefinition> schema,
            List<DataRow> data) {

        for (int r = 0; r < data.size(); r++) {
            if (data.get(r).getAll().size() != schema.size()) {
                throw new IllegalStateException(
                        "Row " + r + " column count mismatch");
            }
        }
    }
}
