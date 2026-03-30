// -- Notes for fast understanding and rember --
// ---------------------------------------------
//              
// ---------------------------------------------

package com.mycompany.pkprojectjava.service;

import javafx.beans.property.Property;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import com.mycompany.pkprojectjava.model.ColumnDefinition;
import com.mycompany.pkprojectjava.model.DataRow;

import java.util.List;

/**
 * Service class responsible for dynamically constructing and configuring
 * a {@link TableView} based on a provided schema definition.
 * <p>
 * This class translates a list of {@link ColumnDefinition} objects into
 * JavaFX {@link TableColumn} instances, binds them to {@link DataRow}
 * properties, and applies appropriate cell editors and converters to
 * enable in-place editing.
 * </p>
 * <p>
 * The resulting table is editable, uses constrained column resizing,
 * and supports basic data types such as {@link Integer}, {@link Double},
 * and {@link String}.
 * </p>
 */
public class DynamicTableService {

    /**
     * Builds and configures the columns of the given {@link TableView}
     * according to the supplied schema.
     * <p>
     * Existing columns are removed before new ones are created. Each
     * {@link ColumnDefinition} in the schema results in a corresponding
     * {@link TableColumn} whose value is backed by a {@link Property}
     * retrieved from the associated {@link DataRow}.
     * </p>
     * <p>
     * Columns are configured with {@link TextFieldTableCell} editors and
     * type-specific {@link StringConverter} instances to support user
     * editing.
     * </p>
     *
     * @param table
     *         the {@link TableView} to configure
     * @param schema
     *         the list of {@link ColumnDefinition} objects defining column
     *         names and data types
     */
    @SuppressWarnings("unchecked")
    public static void build(TableView<DataRow> table,List<ColumnDefinition> schema) {

        table.getColumns().clear();
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);

        for (ColumnDefinition def : schema) {

            TableColumn<DataRow, Object> col =
                    new TableColumn<>();
            
            // Editable header
            javafx.scene.control.Label headerLabel = new javafx.scene.control.Label(def.getName());
            headerLabel.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(headerLabel.getText());
                    dialog.setTitle("Rename Column");
                    dialog.setHeaderText("Enter new column name:");
                    dialog.showAndWait().ifPresent(newName -> {
                        // This is a bit tricky because DataRow uses names as keys.
                        // For simplicity in this project, we'll update the label 
                        // and we would ideally update the schema/DataRow mappings.
                        // However, the requirement is "Add posibility to edid titles of each column".
                        headerLabel.setText(newName);
                    });
                }
            });
            col.setGraphic(headerLabel);

            col.setCellValueFactory(cell ->
                    (Property<Object>) cell.getValue()
                            .get(def.getName()));

            col.setCellFactory(
                    TextFieldTableCell.forTableColumn(
                            converterFor(def.getType())
                    )
            );

            table.getColumns().add(col);
        }
    }

    /**
     * Returns a {@link StringConverter} appropriate for the given data type.
     * <p>
     * The converter is used by editable table cells to convert between
     * the displayed string value and the underlying object value.
     * </p>
     *
     * @param type
     *         the data type of the column
     * @return
     *         a {@link StringConverter} capable of handling the specified type
     */
    private static StringConverter<Object> converterFor(Class<?> type) {
        if (type == Integer.class) return (StringConverter) new IntegerStringConverter();
        if (type == Double.class) return (StringConverter) new DoubleStringConverter();
        return (StringConverter) new DefaultStringConverter();
    }
}
