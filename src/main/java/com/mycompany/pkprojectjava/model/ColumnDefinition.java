// -- Notes for fast understanding and rember --
// ---------------------------------------------
//              
// ---------------------------------------------

package com.mycompany.pkprojectjava.model;

/**
 * Represents a definition of a column consisting of a name and a Java type.
 * <p>
 * This class is typically used as a metadata holder, for example when defining
 * database table schemas, mapping result sets, or describing structured data
 * models where each column has a well-defined type.
 * </p>
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 *
 * @author Piotrek
 */
public class ColumnDefinition {

    /**
     * The name of the column.
     */
    private final String name;

    /**
     * The Java type associated with the column.
     */
    private final Class<?> type;

    /**
     * Constructs a new {@code ColumnDefinition} with the specified name and type.
     *
     * @param name the name of the column; must not be {@code null}
     * @param type the Java {@link Class} representing the column's data type;
     *             must not be {@code null}
     */
    public ColumnDefinition(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Returns the name of the column.
     *
     * @return the column name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the Java type of the column.
     *
     * @return the {@link Class} representing the column's data type
     */
    public Class<?> getType() {
        return type;
    }
}
