// -- Notes for fast understanding and rember --
// ---------------------------------------------
// Shortly, this java file declare some very 
// important stuff, but normaly it doesn't 
// matter, only if error occurs with that 
// add something what is needed       
// ---------------------------------------------

/**
 * Defines the {@code com.mycompany.pkprojectjava} module.
 * <p>
 * This module represents a JavaFX-based application and declares its
 * dependencies, exported packages, and reflective access requirements
 * needed for FXML loading and JavaFX runtime behavior.
 * </p>
 *
 * <h2>Module Dependencies</h2>
 * <ul>
 *   <li>{@code javafx.controls} – Provides JavaFX UI controls.</li>
 *   <li>{@code javafx.fxml} – Enables FXML-based UI definitions and controllers.</li>
 * </ul>
 *
 * <h2>Exported Packages</h2>
 * <p>
 * The following packages are exported for use by other modules:
 * </p>
 * <ul>
 *   <li>{@code com.mycompany.pkprojectjava} – Core application classes.</li>
 *   <li>{@code com.mycompany.pkprojectjava.model} – Domain and data model classes.</li>
 *   <li>{@code com.mycompany.pkprojectjava.service} – Business logic and service layer.</li>
 *   <li>{@code com.mycompany.pkprojectjava.io} – Input/output and persistence utilities.</li>
 * </ul>
 *
 * <h2>Reflective Access</h2>
 * <p>
 * Certain packages are opened to {@code javafx.fxml} to allow runtime
 * reflection, which is required for FXML controller instantiation and
 * property injection.
 * </p>
 */
module com.mycompany.pkprojectjava {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;
    requires exp4j;

    // Allows JavaFX FXML to access controller classes via reflection
    opens com.mycompany.pkprojectjava.controller to javafx.fxml;

    // Exported API packages
    exports com.mycompany.pkprojectjava;
    exports com.mycompany.pkprojectjava.model;
    exports com.mycompany.pkprojectjava.service;
    exports com.mycompany.pkprojectjava.io;

    // Allows FXML access to application-level classes if needed
    opens com.mycompany.pkprojectjava to javafx.fxml;
    requires com.google.gson;
}
