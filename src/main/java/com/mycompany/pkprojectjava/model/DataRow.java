// -- Notes for fast understanding and rember --
// ---------------------------------------------
//              
// ---------------------------------------------

package com.mycompany.pkprojectjava.model;

import javafx.beans.property.Property;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a single logical row of data where each column name is mapped
 * to a JavaFX {@link Property}.
 * <p>
 * This class is typically used as a lightweight, dynamic data container for
 * table-like structures (e.g., JavaFX {@code TableView}) where column definitions
 * may vary at runtime. A {@link LinkedHashMap} is used internally to preserve
 * insertion order of columns.
 * </p>
 *
 * @author Piotrek
 */
public class DataRow {

    /**
     * Stores column-to-property mappings for this row.
     * <p>
     * The map key represents the column identifier, while the value is the
     * associated JavaFX {@link Property}.
     * </p>
     */
    private final Map<String, Property<?>> values = new LinkedHashMap<>();

    /**
     * Associates the specified JavaFX {@link Property} with the given column name.
     * <p>
     * If a value already exists for the specified column, it will be replaced.
     * </p>
     *
     * @param column the column identifier; must not be {@code null}
     * @param value  the JavaFX property to associate with the column; may be {@code null}
     */
    public void put(String column, Property<?> value) {
        values.put(column, value);
    }

    /**
     * Returns the JavaFX {@link Property} associated with the specified column name.
     * <p>
     * This method performs an unchecked cast to the requested generic type.
     * Callers are responsible for ensuring that the requested type matches
     * the actual property type stored for the column.
     * </p>
     *
     * @param <T>    the expected value type of the property
     * @param column the column identifier
     * @return the property associated with the given column, or {@code null}
     *         if no mapping exists
     * @throws ClassCastException if the stored property is not compatible
     *                            with the requested generic type
     */
    @SuppressWarnings("unchecked")
    public <T> Property<T> get(String column) {
        return (Property<T>) values.get(column);
    }

    /**
     * Returns all column-to-property mappings contained in this data row.
     * <p>
     * The returned map is the internal backing map; modifications to it will
     * directly affect this {@code DataRow}.
     * </p>
     *
     * @return a map of column names to JavaFX properties
     */
    public Map<String, Property<?>> getAll() {
        return values;
    }
}
