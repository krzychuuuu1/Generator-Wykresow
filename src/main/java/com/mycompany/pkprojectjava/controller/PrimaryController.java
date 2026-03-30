package com.mycompany.pkprojectjava.controller;

import com.mycompany.pkprojectjava.io.CsvTableWriter;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.AreaChart;
import javafx.scene.layout.StackPane;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import com.mycompany.pkprojectjava.io.DelimitedFileLoader;
import com.mycompany.pkprojectjava.model.ColumnDefinition;
import com.mycompany.pkprojectjava.model.DataRow;
import com.mycompany.pkprojectjava.service.DynamicTableService;

import java.io.File;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
// import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
// import javax.imageio.ImageIO;
import java.util.Optional;

import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.control.CheckBox;
import javafx.collections.transformation.FilteredList;

/**
 * Primary JavaFX controller responsible for handling user interactions
 * in the main application window.
 *
 * <p>This controller manages:
 * <ul>
 *   <li>Loading delimited text/CSV files</li>
 *   <li>Populating a dynamic {@link TableView}</li>
 *   <li>Maintaining the in-memory data model</li>
 *   <li>Basic UI actions such as add, delete, copy, paste, and chart creation</li>
 * </ul>
 *
 * <p>The controller relies on {@link DelimitedFileLoader} for data ingestion
 * and {@link DynamicTableService} for runtime table construction.</p>
 */
public class PrimaryController implements Initializable {

    /**
     * Table view displaying loaded {@link DataRow} objects.
     * The table structure is created dynamically based on the loaded schema.
     */
    @FXML
    private TableView<DataRow> table;

    /**
     * Bar chart used to visualize selected or aggregated data.
     * The X axis represents categorical values, while the Y axis represents numeric values.
     */
    @FXML
    private BarChart<String, Number> barChart;

    /**
     * StackPane container where the current chart is displayed.
     */
    @FXML
    private StackPane chartContainer;

    /**
     * The currently active chart (Bar, Line, or Area).
     */
    private XYChart<String, Number> currentChart;

    /**
     * Menu for recent files.
     */
    @FXML
    private Menu recentMenu;

    /**
     * Radio menu item for switching to Bar Chart.
     */
    @FXML
    private RadioMenuItem barChartItem;

    /**
     * Radio menu item for switching to Line Chart.
     */
    @FXML
    private RadioMenuItem lineChartItem;

    /**
     * Radio menu item for switching to Area Chart.
     */
    @FXML
    private RadioMenuItem areaChartItem;

    /**
     * Toggle group for chart type selection.
     */
    @FXML
    private ToggleGroup chartTypeGroup;

    /**
     * Radio menu item for Light Theme.
     */
    @FXML
    private RadioMenuItem lightThemeItem;

    /**
     * Radio menu item for Dark Theme.
     */
    @FXML
    private RadioMenuItem darkThemeItem;

    /**
     * Toggle group for theme selection.
     */
    @FXML
    private ToggleGroup themeGroup;

    /**
     * Current theme: "light" or "dark".
     */
    private String currentTheme = "light";

    /**
     * Text field for filtering table data.
     */
    @FXML
    private TextField filterField;

    /**
     * Label showing filter results count.
     */
    @FXML
    private Label filterLabel;

    /**
     * Label showing statistics for selected column.
     */
    @FXML
    private Label statsLabel;

    /**
     * HBox panel containing statistics.
     */
    @FXML
    private HBox statsPanel;

    /**
     * CheckBox to toggle regression line display.
     */
    @FXML
    private CheckBox showRegressionCheckBox;

    /**
     * Label showing regression equation.
     */
    @FXML
    private Label regressionEquationLabel;

    /**
     * Filtered list for table filtering.
     */
    private FilteredList<DataRow> filteredData;

    /**
     * Regression line series for chart.
     */
    private XYChart.Series<String, Number> regressionSeries;

    /**
     * List of regression line series for multi-series charts.
     */
    private List<XYChart.Series<String, Number>> regressionSeriesList = new ArrayList<>();

    /**
     * Predefined colors for chart series.
     */
    private static final String[] SERIES_COLORS = {
        "#1f77b4", // Blue
        "#ff7f0e", // Orange
        "#2ca02c", // Green
        "#d62728", // Red
        "#9467bd", // Purple
        "#8c564b", // Brown
        "#e377c2", // Pink
        "#7f7f7f", // Gray
        "#bcbd22", // Olive
        "#17becf"  // Cyan
    };

    /**
     * Observable list backing the table view.
     * Holds all loaded data rows.
     */
    private final ObservableList<DataRow> data = FXCollections.observableArrayList();

    /**
     * Schema definition describing the structure of loaded data.
     * Each entry corresponds to a dynamically generated table column.
     */
    private List<ColumnDefinition> schema;

    /**
     * The file currently opened or being saved.
     */
    private File currentFile;

    /**
     * Stack for undo operations.
     */
    private final Deque<Runnable> undoStack = new ArrayDeque<>();

    /**
     * Stack for redo operations.
     */
    private final Deque<Runnable> redoStack = new ArrayDeque<>();

    /**
     * Initializes the controller after its root element has been completely processed.
     *
     * @param url the location used to resolve relative paths for the root object
     * @param rb  the resources used to localize the root object, or {@code null} if not localized
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Wrap data in FilteredList
        filteredData = new FilteredList<>(data, p -> true);
        table.setItems(filteredData);

        updateRecentMenu();
        currentChart = barChart;
        setupDragAndDrop();
        setupColumnReordering();
        loadThemePreference();

        // Setup filter listener
        if (filterField != null) {
            filterField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterTable(newValue);
            });
        }

        // Setup table selection listener for statistics
        if (table != null) {
            table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                updateStatistics();
            });

            // Setup column sorting
            setupColumnSorting();
        }

        // Apply theme when scene is available
        table.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                applyTheme(currentTheme);
            }
        });

        updateFilterLabel();
    }

    /**
     * Updates the Open Recent menu with the list of recent files.
     */
    private void updateRecentMenu() {
        if (recentMenu == null) return;
        recentMenu.getItems().clear();
        for (File file : recentFiles) {
            MenuItem item = new MenuItem(file.getName());
            item.setOnAction(e -> openFile(file));
            recentMenu.getItems().add(item);
        }
    }

    /**
     * Opens and parses a delimited file, updating the table and chart.
     *
     * @param file the file to open
     */
    private void openFile(File file) {
        try {
            DelimitedFileLoader.Result result = DelimitedFileLoader.load(file, ";");
            schema = result.getSchema();
            data.setAll(result.getRows());
            DynamicTableService.build(table, schema);
            if (currentChart != null) {
                currentChart.getData().clear();
            }
            currentFile = file;
            logAction("Otwarto " + file.getName());
            addToRecentFiles(file);
            updateRecentMenu();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the "New" action by creating a blank 2x2 table.
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void createNew(ActionEvent event) {
        schema = new ArrayList<>();
        schema.add(new ColumnDefinition("Kolumna 1", String.class));
        schema.add(new ColumnDefinition("Kolumna 2", String.class));

        data.clear();
        for (int i = 0; i < 2; i++) {
            DataRow row = new DataRow();
            for (ColumnDefinition col : schema) {
                row.put(col.getName(), new SimpleStringProperty(""));
            }
            data.add(row);
        }

        DynamicTableService.build(table, schema);
        barChart.getData().clear();
        currentFile = null;
        logAction("Utworzono nową tabelę 2x2");
    }

    /**
     * Handles the "Add Row" action.
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void addRow(ActionEvent event) {
        insertRowUndoable(data.size());
    }

    /**
     * Handles the "Delete Row" action.
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void deleteRow(ActionEvent event) {
        int index = table.getSelectionModel().getSelectedIndex();
        if (index >= 0) {
            deleteRowUndoable(index);
        } else if (!data.isEmpty()) {
            deleteRowUndoable(data.size() - 1);
        }
    }

    /**
     * Handles the "Add Column" action.
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void addColumn(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog("Nowa kolumna");
        dialog.setTitle("Dodaj kolumnę");
        dialog.setHeaderText("Podaj nazwę nowej kolumny:");
        dialog.showAndWait().ifPresent(name -> {
            insertColumnUndoable(schema != null ? schema.size() : 0, name, String.class);
        });
    }

    /**
     * Handles the "Delete Column" action.
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void deleteColumn(ActionEvent event) {
        if (schema == null || schema.isEmpty()) return;

        List<String> choices = new ArrayList<>();
        for (ColumnDefinition col : schema) {
            choices.add(col.getName());
        }

        javafx.scene.control.ChoiceDialog<String> dialog = new javafx.scene.control.ChoiceDialog<>(choices.get(choices.size() - 1), choices);
        dialog.setTitle("Usuń kolumnę");
        dialog.setHeaderText("Wybierz kolumnę do usunięcia:");
        dialog.showAndWait().ifPresent(this::deleteColumnUndoable);
    }

    /**
     * Inserts a new row at the specified index and adds it to the undo stack.
     *
     * @param index the index at which to insert the row
     */
    private void insertRowUndoable(int index) {
        insertRowInternal(index);
        
        undoStack.push(() -> {
            DataRow row = data.get(index);
            data.remove(index);
            logAction("Cofnięto: Usunięto wiersz");
            redoStack.push(() -> {
                insertRowInternal(index);
                logAction("Ponowiono: Przywrócono wiersz");
            });
        });
        redoStack.clear();
    }

    /**
     * Performs the actual insertion of a new row at the specified index.
     *
     * @param index the index at which to insert the row
     */
    private void insertRowInternal(int index) {
        DataRow newRow = new DataRow();
        if (schema != null) {
            for (ColumnDefinition col : schema) {
                newRow.put(col.getName(), createDefaultProperty(col.getType()));
            }
        }
        data.add(index, newRow);
        logAction("Dodano wiersz na pozycji " + index);
    }

    /**
     * Deletes the row at the specified index and adds the action to the undo stack.
     *
     * @param index the index of the row to delete
     */
    private void deleteRowUndoable(int index) {
        DataRow removedRow = data.get(index);
        deleteRowInternal(index);

        undoStack.push(() -> {
            data.add(index, removedRow);
            logAction("Cofnięto: Przywrócono wiersz");
            redoStack.push(() -> {
                deleteRowInternal(index);
                logAction("Ponowiono: Usunięto wiersz");
            });
        });
        redoStack.clear();
    }

    /**
     * Performs the actual deletion of the row at the specified index.
     *
     * @param index the index of the row to delete
     */
    private void deleteRowInternal(int index) {
        data.remove(index);
        logAction("Usunięto wiersz na pozycji " + index);
    }

    /**
     * Inserts a new column at the specified index and adds it to the undo stack.
     *
     * @param index the index at which to insert the column
     * @param name  the name of the new column
     * @param type  the data type of the new column
     */
    private void insertColumnUndoable(int index, String name, Class<?> type) {
        insertColumnInternal(index, name, type);

        undoStack.push(() -> {
            ColumnDefinition colDef = schema.get(index);
            deleteColumnInternal(name);
            logAction("Cofnięto: Usunięto kolumnę " + name);
            redoStack.push(() -> {
                insertColumnInternal(index, name, type);
                logAction("Ponowiono: Przywrócono kolumnę " + name);
            });
        });
        redoStack.clear();
    }

    /**
     * Performs the actual insertion of a new column at the specified index.
     *
     * @param index the index at which to insert the column
     * @param name  the name of the new column
     * @param type  the data type of the new column
     */
    private void insertColumnInternal(int index, String name, Class<?> type) {
        if (schema == null) schema = new ArrayList<>();
        ColumnDefinition colDef = new ColumnDefinition(name, type);
        schema.add(index, colDef);

        for (DataRow row : data) {
            row.put(name, createDefaultProperty(type));
        }
        DynamicTableService.build(table, schema);
        logAction("Dodano kolumnę: " + name);
    }

    /**
     * Deletes the column with the specified name and adds the action to the undo stack.
     *
     * @param name the name of the column to delete
     */
    private void deleteColumnUndoable(String name) {
        int index = -1;
        for (int i = 0; i < schema.size(); i++) {
            if (schema.get(i).getName().equals(name)) {
                index = i;
                break;
            }
        }
        if (index == -1) return;

        ColumnDefinition removedCol = schema.get(index);
        List<javafx.beans.property.Property<?>> removedData = new ArrayList<>();
        for (DataRow row : data) {
            removedData.add(row.getAll().get(name));
        }
        
        deleteColumnInternal(name);

        int finalIndex = index;
        undoStack.push(() -> {
            schema.add(finalIndex, removedCol);
            for (int i = 0; i < data.size(); i++) {
                data.get(i).put(name, removedData.get(i));
            }
            DynamicTableService.build(table, schema);
            logAction("Cofnięto: Przywrócono kolumnę " + name);
            redoStack.push(() -> {
                deleteColumnInternal(name);
                logAction("Ponowiono: Usunięto kolumnę " + name);
            });
        });
        redoStack.clear();
    }

    /**
     * Performs the actual deletion of the column with the specified name.
     *
     * @param name the name of the column to delete
     */
    private void deleteColumnInternal(String name) {
        schema.removeIf(col -> col.getName().equals(name));
        for (DataRow row : data) {
            row.getAll().remove(name);
        }
        DynamicTableService.build(table, schema);
        logAction("Usunięto kolumnę " + name);
    }

    /**
     * Handles the "Export Chart" action.
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void exportChart(ActionEvent event) {
        if (currentChart == null) return;

        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Obraz PNG", "*.png"),
            new FileChooser.ExtensionFilter("Obraz JPG", "*.jpg")
        );
        File file = chooser.showSaveDialog(new Stage());
        if (file == null) return;

        WritableImage image = currentChart.snapshot(new SnapshotParameters(), null);
        
        // Use a background task or just try to use standard AWT/Swing if available via reflection or if we can safely assume it's there
        // Actually, without javafx-swing, we can't easily convert WritableImage to BufferedImage for ImageIO.
        // However, many environments have it. Let's try to use it and see.
        
        try {
            // Attempt to use SwingFXUtils via reflection to avoid direct dependency issues if it's missing at compile time
            // but we added it to pom.xml (well, I tried and reverted, but let's try a simpler approach)
            
            java.awt.image.RenderedImage renderedImage = null;
            try {
                Class<?> swingFXUtilsClass = Class.forName("javafx.embed.swing.SwingFXUtils");
                java.lang.reflect.Method fromFXImageMethod = swingFXUtilsClass.getMethod("fromFXImage", javafx.scene.image.Image.class, java.awt.image.BufferedImage.class);
                renderedImage = (java.awt.image.RenderedImage) fromFXImageMethod.invoke(null, image, null);
            } catch (Exception e) {
                System.out.println("Nie znaleziono SwingFXUtils lub eksport nie powiódł się. Nie można wyeksportować obrazu.");
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Błąd eksportu");
                alert.setHeaderText("Brak zależności");
                alert.setContentText("Nie można wyeksportować wykresu, ponieważ javafx-swing nie jest dostępne.");
                applyThemeToDialog(alert.getDialogPane());
                alert.showAndWait();
                return;
            }

            String format = file.getName().endsWith(".png") ? "png" : "jpg";
            javax.imageio.ImageIO.write(renderedImage, format, file);
            logAction("Wyeksportowano wykres do " + file.getName());

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Błąd eksportu");
            alert.setHeaderText("Nie udało się zapisać obrazu");
            alert.setContentText(e.getMessage());
            applyThemeToDialog(alert.getDialogPane());
            alert.showAndWait();
        }
    }

    /**
     * Handles the "Open" action.
     *
     * <p>Displays a {@link FileChooser} allowing the user to select a TXT or CSV file.
     * The selected file is parsed using {@link DelimitedFileLoader}, and the resulting
     * data and schema are applied to the table.</p>
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void open(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pliki TXT / CSV", "*.txt", "*.csv"));

        File file = chooser.showOpenDialog(new Stage());
        if (file == null) return;

        try {
            DelimitedFileLoader.Result result = DelimitedFileLoader.load(file, ";");

            schema = result.getSchema();
            data.setAll(result.getRows());

            DynamicTableService.build(table, schema);

            currentFile = file;
            logAction("Otwarto " + file.getName());
            addToRecentFiles(file);

            System.out.println("Załadowano " + schema.size() + " kolumn.");
            System.out.println("Załadowano " + data.size() + " wierszy.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the "Delete" action by delegating to {@link #deleteRow(ActionEvent)}.
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void delete(ActionEvent event) {
        deleteRow(event);
    }

    /**
     * Handles the "Close" action.
     *
     * <p>Saves the current table if modified and then clears the view.</p>
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void close(ActionEvent event) {
        if (schema != null && !data.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Zapisz zmiany?");
            alert.setHeaderText("Czy chcesz zapisać zmiany przed zamknięciem?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                save(event);
            }
        }
        
        data.clear();
        schema = new ArrayList<>();
        DynamicTableService.build(table, schema);
        barChart.getData().clear();
        currentFile = null;
        logAction("Zapisano i zamknięto tabelę");
    }

    /**
     * Buffer for copying a row.
     */
    private DataRow clipboard;

    /**
     * Handles the "Copy" action.
     *
     * <p>Copies the selected row to a simple internal buffer.</p>
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void copy(ActionEvent event) {
        clipboard = table.getSelectionModel().getSelectedItem();
        if (clipboard != null) {
            logAction("Skopiowano wiersz");
        }
    }

    /**
     * Handles the "Paste" action.
     *
     * <p>Pastes the buffered row into the table.</p>
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void paste(ActionEvent event) {
        if (clipboard == null) return;
        
        DataRow newRow = new DataRow();
        for (ColumnDefinition col : schema) {
            Property<?> originalProp = clipboard.get(col.getName());
            Property<?> newProp = createDefaultProperty(col.getType());
            if (originalProp != null && originalProp.getValue() != null) {
                ((Property<Object>)newProp).setValue(originalProp.getValue());
            }
            newRow.put(col.getName(), newProp);
        }
        data.add(newRow);
        logAction("Wklejono wiersz");
    }

    /**
     * Handles the "Quit" action.
     *
     * <p>Terminates the application.</p>
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void quit(ActionEvent event) {
        javafx.application.Platform.exit();
    }

    /**
     * Handles the "Save" action.
     *
     * <p>Intended to persist the current data set to its original source.</p>
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void save(ActionEvent event) {
        if (currentFile == null) {
            saveAs(event);
            return;
        }
        performSave(currentFile);
    }

    /** Zaznacza wszystkie wiersze. */
    @FXML
    void selectAll(ActionEvent event) {
        table.getSelectionModel().selectAll();
        logAction("Zaznaczono wszystkie wiersze");
    }

    /** Odznacza wszystkie wiersze. */
    @FXML
    void unselectAll(ActionEvent event) {
        table.getSelectionModel().clearSelection();
        logAction("Odznaczono wszystkie wiersze");
    }

    /** Wyświetla okno „O programie”. */
    @FXML
    void about(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("O programie");
        alert.setHeaderText("Aplikacja: tabela danych i wykresy (JavaFX)");
        alert.setContentText("Wersja 1.0\nAutor: Piotrek");
        applyThemeToDialog(alert.getDialogPane());
        alert.showAndWait();
    }

    /** Czyści aktualny wykres. */
    @FXML
    void clearChart(ActionEvent event) {
        if (currentChart != null) {
            currentChart.getData().clear();
        }
    }

    /** Przełącza typ wykresu na słupkowy. */
    @FXML
    void switchToBarChart(ActionEvent event) {
        switchChartType(new BarChart<>(new CategoryAxis(), new NumberAxis()), "wykres słupkowy");
        if (barChartItem != null) barChartItem.setSelected(true);
    }

    /** Przełącza typ wykresu na liniowy. */
    @FXML
    void switchToLineChart(ActionEvent event) {
        switchChartType(new LineChart<>(new CategoryAxis(), new NumberAxis()), "wykres liniowy");
        if (lineChartItem != null) lineChartItem.setSelected(true);
    }

    /** Przełącza typ wykresu na warstwowy. */
    @FXML
    void switchToAreaChart(ActionEvent event) {
        switchChartType(new AreaChart<>(new CategoryAxis(), new NumberAxis()), "wykres warstwowy");
        if (areaChartItem != null) areaChartItem.setSelected(true);
    }

    /**
     * Switches the chart type while preserving existing series data.
     *
     * @param newChart the new chart instance
     * @param chartTypeName the name of the chart type for logging
     */
    private void switchChartType(XYChart<String, Number> newChart, String chartTypeName) {
        // Save current chart data
        List<XYChart.Series<String, Number>> savedSeries = new ArrayList<>();
        String title = "";
        String xLabel = "";
        String yLabel = "";

        if (currentChart != null) {
            title = currentChart.getTitle();
            xLabel = currentChart.getXAxis().getLabel();
            yLabel = currentChart.getYAxis().getLabel();

            // Save all series (including regression lines)
            for (XYChart.Series<String, Number> series : currentChart.getData()) {
                XYChart.Series<String, Number> newSeries = new XYChart.Series<>();
                newSeries.setName(series.getName());

                // Copy all data points
                for (XYChart.Data<String, Number> dataPoint : series.getData()) {
                    newSeries.getData().add(new XYChart.Data<>(
                        dataPoint.getXValue(),
                        dataPoint.getYValue()
                    ));
                }

                savedSeries.add(newSeries);
            }
        }

        // Setup new chart
        setupChart(newChart);

        // Restore metadata
        currentChart.setTitle(title);
        currentChart.getXAxis().setLabel(xLabel);
        currentChart.getYAxis().setLabel(yLabel);

        // Restore series data
        if (!savedSeries.isEmpty()) {
            for (XYChart.Series<String, Number> series : savedSeries) {
                currentChart.getData().add(series);
            }

            // Reapply colors to series
            applySeriesColors();

            // Reapply regression styles if any regression series exist
            javafx.application.Platform.runLater(() -> {
                int colorIndex = 0;
                for (XYChart.Series<String, Number> series : currentChart.getData()) {
                    if (series.getName().startsWith("Regression:")) {
                        applyRegressionStyle(series, colorIndex);
                    } else {
                        colorIndex++;
                    }
                }
            });
        } else if (data != null && !data.isEmpty()) {
            // Only auto-generate from table if no existing chart data
            generateChartInternal("0,1");
        }

        logAction("Przełączono na " + chartTypeName);
    }

    /**
     * Internal method to generate chart data based on column indices.
     *
     * @param colIndices a string containing comma-separated category and value column indices
     */
    private void generateChartInternal(String colIndices) {
        if (data.isEmpty() || schema == null || schema.size() < 2) return;

        try {
            String[] parts = colIndices.split(",");
            int catIdx = Integer.parseInt(parts[0].trim());
            int valIdx = Integer.parseInt(parts[1].trim());

            if (catIdx >= schema.size() || valIdx >= schema.size() || catIdx < 0 || valIdx < 0) return;

            ColumnDefinition catCol = schema.get(catIdx);
            ColumnDefinition valCol = schema.get(valIdx);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(valCol.getName() + " by " + catCol.getName());

            for (DataRow row : data) {
                Property<?> catProp = row.get(catCol.getName());
                Property<?> valProp = row.get(valCol.getName());

                if (catProp != null && valProp != null) {
                    Object val = valProp.getValue();
                    Number numVal = null;
                    if (val instanceof Number) {
                        numVal = (Number) val;
                    } else if (val != null) {
                        try {
                            numVal = Double.parseDouble(val.toString());
                        } catch (NumberFormatException e) {
                        }
                    }

                    if (numVal != null) {
                        series.getData().add(new XYChart.Data<>(
                            catProp.getValue() != null ? catProp.getValue().toString() : "",
                            numVal
                        ));
                    }
                }
            }
            currentChart.getData().clear();
            currentChart.getData().add(series);
        } catch (Exception e) {
            // ignore auto-gen errors
        }
    }

    /**
     * Configures the chart container with the specified chart and applies common settings.
     *
     * @param newChart the new chart to display
     */
    private void setupChart(XYChart<String, Number> newChart) {
        chartContainer.getChildren().clear();
        chartContainer.getChildren().add(newChart);
        currentChart = newChart;
        currentChart.setAnimated(false);
        
        // Ensure axes have labels enabled (sometimes JavaFX requires them to be non-empty or set to show)
        if (currentChart.getXAxis() != null) {
            currentChart.getXAxis().setAnimated(false);
            // Pre-set an empty string label to ensure the axis is prepared to show one
            if (currentChart.getXAxis().getLabel() == null) {
                currentChart.getXAxis().setLabel("");
            }
        }
        if (currentChart.getYAxis() != null) {
            currentChart.getYAxis().setAnimated(false);
            // Pre-set an empty string label to ensure the axis is prepared to show one
            if (currentChart.getYAxis().getLabel() == null) {
                currentChart.getYAxis().setLabel("");
            }
        }
    }

    /**
     * Handles the "Set Chart Title" action.
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void setChartTitle(ActionEvent event) {
        if (currentChart == null) return;
        TextInputDialog dialog = new TextInputDialog(currentChart.getTitle());
        dialog.setTitle("Tytuł wykresu");
        dialog.setHeaderText("Podaj tytuł wykresu:");
        dialog.showAndWait().ifPresent(title -> {
            currentChart.setTitle(title);
            // Force layout update to ensure title is shown
            currentChart.requestLayout();
            if (currentChart.getParent() != null) {
                currentChart.getParent().requestLayout();
            }
            currentChart.layout();
        });
    }

    /**
     * Handles the "Set X Axis Title" action.
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void setXAxisTitle(ActionEvent event) {
        if (currentChart == null) return;

        javafx.scene.chart.Axis<?> xAxis = currentChart.getXAxis();
        String currentLabel = xAxis.getLabel();

        TextInputDialog dialog = new TextInputDialog(
                currentLabel != null ? currentLabel : ""
        );
        dialog.setTitle("Tytuł osi X");
        dialog.setHeaderText("Podaj tytuł osi X:");

        dialog.showAndWait().ifPresent(labelx -> {
            xAxis.setLabel(null);   // force internal label rebuild
            xAxis.setLabel(labelx);

            // Force layout update on axis, chart, and parent container
            xAxis.requestLayout();
            currentChart.requestLayout();
            if (currentChart.getParent() != null) {
                currentChart.getParent().requestLayout();
            }
            currentChart.layout();
            currentChart.setAnimated(false);
            currentChart.setAnimated(true);
        });
    }

    /**
     * Handles the "Set Y Axis Title" action.
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void setYAxisTitle(ActionEvent event) {
        if (currentChart == null) return;

        javafx.scene.chart.Axis<?> yAxis = currentChart.getYAxis();
        String currentLabel = yAxis.getLabel();

        TextInputDialog dialog = new TextInputDialog(
                currentLabel != null ? currentLabel : ""
        );
        dialog.setTitle("Tytuł osi Y");
        dialog.setHeaderText("Podaj tytuł osi Y:");

        dialog.showAndWait().ifPresent(labely -> {
            yAxis.setLabel(null);   // force internal label rebuild
            yAxis.setLabel(labely);

            // Force layout update on axis, chart, and parent container
            yAxis.requestLayout();
            currentChart.requestLayout();
            if (currentChart.getParent() != null) {
                currentChart.getParent().requestLayout();
            }
            currentChart.layout();
            currentChart.setAnimated(false);
            currentChart.setAnimated(true);
        });
    }

    /**
     * Performs the actual save operation to the specified file.
     *
     * @param file the target file for saving
     */
    private void performSave(File file) {
        System.out.println("Zapis wszystkich kolumn do " + file.getAbsolutePath());
        List<ColumnDefinition> saveSchema = new ArrayList<>(schema);

        CsvTableWriter.write(file, saveSchema, data);
        logAction("Zapisano do " + file.getName());
    }

    /**
     * Handles the "Save As" action.
     *
     * <p>Intended to persist the current data set to a user-selected location.</p>
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void saveAs(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("CSV / TXT", "*.csv", "*.txt"),
            new FileChooser.ExtensionFilter("PDF", "*.pdf")
        );

        File file = chooser.showSaveDialog(new Stage());
        if (file == null) return;

        currentFile = file;
        String name = file.getName().toLowerCase();
        
        if (name.endsWith(".pdf")) {
            System.out.println("Eksport wykresu do PDF jest symulowany.");
            logAction("Wyeksportowano wykres do " + file.getName());
        } else {
            performSave(file);
        }
        
        addToRecentFiles(file);
    }

    /**
     * Logs an action to the console for history tracking.
     *
     * @param action the action description to log
     */
    private void logAction(String action) {
        System.out.println("[HISTORIA] " + action);
        // In a real app, this would write to a file
    }

    /**
     * List of recently opened files.
     */
    private final List<File> recentFiles = new ArrayList<>();

    /**
     * Adds a file to the recent files list and updates the menu.
     *
     * @param file the file to add
     */
    private void addToRecentFiles(File file) {
        if (!recentFiles.contains(file)) {
            recentFiles.add(0, file);
        } else {
            recentFiles.remove(file);
            recentFiles.add(0, file);
        }
        if (recentFiles.size() > 5) {
            recentFiles.remove(5);
        }
        updateRecentMenu();
    }

    /**
     * Sets up drag-and-drop functionality for table rows to allow reordering.
     */
    private void setupDragAndDrop() {
        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<DataRow> row = new javafx.scene.control.TableRow<>();
            
            row.setOnDragDetected(event -> {
                if (!row.isEmpty()) {
                    Integer index = row.getIndex();
                    javafx.scene.input.Dragboard db = row.startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
                    db.setDragView(row.snapshot(null, null));
                    javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                    cc.put(javafx.scene.input.DataFormat.PLAIN_TEXT, index.toString());
                    db.setContent(cc);
                    event.consume();
                }
            });

            row.setOnDragOver(event -> {
                javafx.scene.input.Dragboard db = event.getDragboard();
                if (db.hasContent(javafx.scene.input.DataFormat.PLAIN_TEXT)) {
                    if (row.getIndex() != Integer.parseInt(db.getContent(javafx.scene.input.DataFormat.PLAIN_TEXT).toString())) {
                        event.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
                        event.consume();
                    }
                }
            });

            row.setOnDragDropped(event -> {
                javafx.scene.input.Dragboard db = event.getDragboard();
                if (db.hasContent(javafx.scene.input.DataFormat.PLAIN_TEXT)) {
                    int draggedIndex = Integer.parseInt(db.getContent(javafx.scene.input.DataFormat.PLAIN_TEXT).toString());
                    DataRow draggedItem = data.remove(draggedIndex);

                    int dropIndex ;
                    if (row.isEmpty()) {
                        dropIndex = data.size();
                    } else {
                        dropIndex = row.getIndex();
                    }

                    data.add(dropIndex, draggedItem);

                    event.setDropCompleted(true);
                    table.getSelectionModel().select(dropIndex);
                    event.consume();
                    
                    logAction("Przesunięto wiersz z " + draggedIndex + " na " + dropIndex);
                    // Could add undo for this too
                }
            });

            return row ;
        });
    }

    /**
     * Sets up a listener to detect column reordering in the table and updates the schema accordingly.
     */
    private void setupColumnReordering() {
        table.getColumns().addListener((ListChangeListener<javafx.scene.control.TableColumn<DataRow, ?>>) c -> {
            while (c.next()) {
                if (c.wasReplaced() || c.wasPermutated()) {
                    updateSchemaFromColumns();
                }
            }
        });
    }

    /**
     * Updates the internal schema based on the current order and visibility of table columns.
     */
    private void updateSchemaFromColumns() {
        if (schema == null) return;
        List<ColumnDefinition> newSchema = new ArrayList<>();
        for (javafx.scene.control.TableColumn<DataRow, ?> col : table.getColumns()) {
            String colName = null;
            if (col.getGraphic() instanceof javafx.scene.control.Label) {
                colName = ((javafx.scene.control.Label) col.getGraphic()).getText();
            } else {
                colName = col.getText();
            }
            
            final String finalColName = colName;
            schema.stream()
                .filter(d -> d.getName().equals(finalColName))
                .findFirst()
                .ifPresent(newSchema::add);
        }
        if (newSchema.size() == schema.size()) {
            schema.clear();
            schema.addAll(newSchema);
            logAction("Zmieniono kolejność kolumn");
        }
    }


    /**
     * Handles the "Add" action by delegating to {@link #addRow(ActionEvent)}.
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void add(ActionEvent event) {
        addRow(event);
    }
    
    /**
     * Inserts a column at the specified index and updates the table.
     *
     * @param index the index at which to insert the column
     * @param name  the name of the new column
     * @param type  the data type of the new column
     */
    void insertColumn(int index, String name, Class<?> type) {

        schema.add(index, new ColumnDefinition(name, type));

        for (DataRow row : data) {
            row.getAll().put(name, createDefaultProperty(type));
        }

        DynamicTableService.build(table, schema);
        logAction("Wstawiono kolumnę: " + name);
    }



    /**
     * Handles the chart generation action.
     *
     * <p>Generates a chart based on selected columns and rows.</p>
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void generateChart(ActionEvent event) {
        if (data.isEmpty() || schema == null || schema.size() < 2) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Niewystarczające dane");
            alert.setHeaderText(null);
            alert.setContentText("Za mało danych do wykresu (potrzebne przynajmniej 2 kolumny i 1 wiersz).");
            alert.showAndWait();
            return;
        }

        if (currentChart == null) {
            switchToBarChart(null);
        }

        // Keep current titles
        String title = currentChart.getTitle();
        String xLabel = currentChart.getXAxis().getLabel();
        String yLabel = currentChart.getYAxis().getLabel();

        // Show choice dialog for columns
        TextInputDialog colDialog = new TextInputDialog("0,1");
        colDialog.setTitle("Konfiguracja wykresu");
        colDialog.setHeaderText("Wybierz indeksy kolumn (Kategoria, Wartość):");
        colDialog.setContentText("Indeksy (np. 0,1):");

        Optional<String> colResult = colDialog.showAndWait();
        if (!colResult.isPresent()) return;

        try {
            String[] parts = colResult.get().split(",");
            int catIdx = Integer.parseInt(parts[0].trim());
            int valIdx = Integer.parseInt(parts[1].trim());

            if (catIdx >= schema.size() || valIdx >= schema.size() || catIdx < 0 || valIdx < 0) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Nieprawidłowe indeksy");
                alert.setHeaderText(null);
                alert.setContentText("Nieprawidłowe indeksy kolumn.");
                alert.showAndWait();
                return;
            }

            ColumnDefinition catCol = schema.get(catIdx);
            ColumnDefinition valCol = schema.get(valIdx);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(valCol.getName() + " by " + catCol.getName());

            int count = 0;
            for (DataRow row : data) {
                Property<?> catProp = row.get(catCol.getName());
                Property<?> valProp = row.get(valCol.getName());

                if (catProp != null && valProp != null) {
                    Object val = valProp.getValue();
                    Number numVal = null;
                    if (val instanceof Number) {
                        numVal = (Number) val;
                    } else if (val != null) {
                        try {
                            numVal = Double.parseDouble(val.toString());
                        } catch (NumberFormatException e) {
                            // skip non-numeric
                        }
                    }

                    if (numVal != null) {
                        series.getData().add(new XYChart.Data<>(
                            catProp.getValue() != null ? catProp.getValue().toString() : "",
                            numVal
                        ));
                        count++;
                    }
                }
            }

            currentChart.setAnimated(false);
            if (currentChart.getXAxis() instanceof CategoryAxis) {
                ((CategoryAxis) currentChart.getXAxis()).setAnimated(false);
            }
            if (currentChart.getYAxis() instanceof NumberAxis) {
                ((NumberAxis) currentChart.getYAxis()).setAnimated(false);
            }

            currentChart.getData().clear();
            currentChart.getData().add(series);
            
            // Re-apply titles in case they were lost or if we want to ensure they stay
            currentChart.setTitle(title);
            currentChart.getXAxis().setLabel(xLabel);
            currentChart.getYAxis().setLabel(yLabel);

            logAction("Wygenerowano wykres używając kolumn " + catIdx + " i " + valIdx + " (" + count + " punktów)");

        } catch (Exception e) {
            System.out.println("Błąd podczas generowania wykresu: " + e.getMessage());
        }
    }
    
    /**
     * Creates a default JavaFX {@link Property} based on the specified type.
     *
     * @param type the data type for which to create a property
     * @return a new property instance with a default value
     */
    private Property<?> createDefaultProperty(Class<?> type) {
        if (type == Integer.class) return new SimpleIntegerProperty(0);
        if (type == Double.class) return new SimpleDoubleProperty(0.0);
        return new SimpleStringProperty("");
    }

    /**
     * Handles the "Undo" action.
     *
     * @param e the action event triggered by the user
     */
    @FXML
    void undo(ActionEvent e) {
        if (!undoStack.isEmpty()) {
            undoStack.pop().run();
        }
    }

    /**
     * Handles the "Redo" action.
     *
     * @param e the action event triggered by the user
     */
    @FXML
    void redo(ActionEvent e) {
        if (!redoStack.isEmpty()) {
            redoStack.pop().run();
        }
    }

    /** Generuje losowy wykres (100 punktów) do testów. */
    @FXML
    void generateRandomChart(ActionEvent event) {
        try {
            // Dla czytelności używamy wykresu liniowego
            switchToLineChart(null);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Dane losowe (100 punktów)");

            java.util.Random random = new java.util.Random();

            for (int i = 0; i < 100; i++) {
                double value = random.nextDouble() * 100;
                series.getData().add(new XYChart.Data<>(String.valueOf(i), value));
            }

            currentChart.getData().clear();
            currentChart.getData().add(series);
            currentChart.setTitle("Test: losowy wykres");
            currentChart.getXAxis().setLabel("Indeks punktu");
            currentChart.getYAxis().setLabel("Wartość");

            logAction("Wygenerowano losowy wykres (100 punktów)");

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Błąd");
            alert.setHeaderText("Nie udało się wygenerować losowego wykresu");
            alert.setContentText(e.getMessage());
            applyThemeToDialog(alert.getDialogPane());
            alert.showAndWait();
        }
    }

    /** Generuje wykres na podstawie wzoru matematycznego (exp4j). */
    @FXML
    void generateChartFromFormula(ActionEvent event) {
        try {
            TextInputDialog formulaDialog = new TextInputDialog("2*x + 3");
            formulaDialog.setTitle("Wykres ze wzoru");
            formulaDialog.setHeaderText("Podaj wzór matematyczny:");
            formulaDialog.setContentText("Wzór (użyj zmiennej 'x'):\nPrzykłady:\n  2*x + 3\n  x^2 - 4*x + 1\n  sin(x)\n  2*sin(x) + cos(x)");
            applyThemeToDialog(formulaDialog.getDialogPane());

            Optional<String> formulaResult = formulaDialog.showAndWait();
            if (!formulaResult.isPresent() || formulaResult.get().trim().isEmpty()) {
                return;
            }

            String formula = formulaResult.get().trim();

            TextInputDialog rangeDialog = new TextInputDialog("-10, 10");
            rangeDialog.setTitle("Zakres X");
            rangeDialog.setHeaderText("Podaj zakres X:");
            rangeDialog.setContentText("Format: min, max\n(np. -10, 10)");
            applyThemeToDialog(rangeDialog.getDialogPane());

            Optional<String> rangeResult = rangeDialog.showAndWait();
            if (!rangeResult.isPresent()) {
                return;
            }

            String[] rangeParts = rangeResult.get().split(",");
            double xMin = Double.parseDouble(rangeParts[0].trim());
            double xMax = Double.parseDouble(rangeParts[1].trim());

            TextInputDialog pointsDialog = new TextInputDialog("100");
            pointsDialog.setTitle("Liczba punktów");
            pointsDialog.setHeaderText("Podaj liczbę punktów:");
            pointsDialog.setContentText("Punkty (10–1000):");
            applyThemeToDialog(pointsDialog.getDialogPane());

            Optional<String> pointsResult = pointsDialog.showAndWait();
            int numPoints = pointsResult.isPresent() ? Integer.parseInt(pointsResult.get().trim()) : 100;

            if (numPoints < 10) numPoints = 10;
            if (numPoints > 1000) numPoints = 1000;

            boolean populateTable = true;
            if (!data.isEmpty()) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Nadpisać dane tabeli?");
                confirmAlert.setHeaderText("Tabela zawiera już dane");
                confirmAlert.setContentText("Czy nadpisać tabelę danymi z wzoru (x oraz f(x))?");
                applyThemeToDialog(confirmAlert.getDialogPane());

                Optional<ButtonType> result = confirmAlert.showAndWait();
                populateTable = result.isPresent() && result.get() == ButtonType.OK;
            }

            switchToLineChart(null);

            net.objecthunter.exp4j.ExpressionBuilder builder =
                new net.objecthunter.exp4j.ExpressionBuilder(formula).variable("x");
            net.objecthunter.exp4j.Expression expression = builder.build();

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("f(x) = " + formula);

            double step = (xMax - xMin) / (numPoints - 1);

            List<Double> xValues = new ArrayList<>();
            List<Double> yValues = new ArrayList<>();

            for (int i = 0; i < numPoints; i++) {
                double x = xMin + i * step;
                expression.setVariable("x", x);
                double y = expression.evaluate();

                xValues.add(x);
                yValues.add(y);

                String xLabel = String.format("%.2f", x);
                series.getData().add(new XYChart.Data<>(xLabel, y));
            }

            currentChart.getData().clear();
            currentChart.getData().add(series);
            currentChart.setTitle("Wykres ze wzoru: " + formula);
            currentChart.getXAxis().setLabel("x");
            currentChart.getYAxis().setLabel("f(x)");

            if (populateTable) {
                schema = new ArrayList<>();
                schema.add(new ColumnDefinition("x", Double.class));
                schema.add(new ColumnDefinition("f(x)", Double.class));

                data.clear();

                for (int i = 0; i < xValues.size(); i++) {
                    DataRow row = new DataRow();
                    row.put("x", new SimpleDoubleProperty(xValues.get(i)));
                    row.put("f(x)", new SimpleDoubleProperty(yValues.get(i)));
                    data.add(row);
                }

                DynamicTableService.build(table, schema);

                logAction("Wygenerowano wykres i uzupełniono tabelę ze wzoru: " + formula +
                    " (" + numPoints + " punktów, zakres " + xMin + "…" + xMax + ")");
            } else {
                logAction("Wygenerowano wykres ze wzoru: " + formula +
                    " (" + numPoints + " punktów, zakres " + xMin + "…" + xMax + ") – tabela bez zmian");
            }

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Błąd");
            alert.setHeaderText("Nie udało się wygenerować wykresu ze wzoru");
            alert.setContentText("Błąd: " + e.getMessage());
            applyThemeToDialog(alert.getDialogPane());
            alert.showAndWait();
        }
    }

    /** Ustawia motyw jasny. */
    @FXML
    void switchToLightTheme(ActionEvent event) {
        currentTheme = "light";
        applyTheme(currentTheme);
        saveThemePreference();
        logAction("Włączono motyw jasny");
    }

    /** Ustawia motyw ciemny. */
    @FXML
    void switchToDarkTheme(ActionEvent event) {
        currentTheme = "dark";
        applyTheme(currentTheme);
        saveThemePreference();
        logAction("Włączono motyw ciemny");
    }

    /**
     * Applies the specified theme to the application.
     *
     * @param theme the theme to apply ("light" or "dark")
     */
    private void applyTheme(String theme) {
        try {
            javafx.scene.Scene scene = table.getScene();
            if (scene != null) {
                scene.getStylesheets().clear();
                // Use App.class to load from correct package location
                java.net.URL cssUrl = com.mycompany.pkprojectjava.App.class.getResource(theme + "-theme.css");
                if (cssUrl != null) {
                    String cssFile = cssUrl.toExternalForm();
                    scene.getStylesheets().add(cssFile);

                    // Update the toggle selection
                    if ("dark".equals(theme) && darkThemeItem != null) {
                        darkThemeItem.setSelected(true);
                    } else if ("light".equals(theme) && lightThemeItem != null) {
                        lightThemeItem.setSelected(true);
                    }

                    System.out.println("Motyw zastosowany pomyślnie: " + theme);
                } else {
                    System.err.println("Nie znaleziono pliku CSS: " + theme + "-theme.css");
                }
            }
            // Scene is null during initialization - will be applied when scene is ready
        } catch (Exception e) {
            System.err.println("Błąd podczas stosowania motywu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Applies the current theme to a dialog pane.
     *
     * @param dialogPane the dialog pane to theme
     */
    private void applyThemeToDialog(javafx.scene.control.DialogPane dialogPane) {
        try {
            if (dialogPane != null) {
                dialogPane.getStylesheets().clear();
                java.net.URL cssUrl = com.mycompany.pkprojectjava.App.class.getResource(currentTheme + "-theme.css");
                if (cssUrl != null) {
                    String cssFile = cssUrl.toExternalForm();
                    dialogPane.getStylesheets().add(cssFile);
                }
            }
        } catch (Exception e) {
            System.err.println("Błąd podczas stosowania motywu do okna dialogowego: " + e.getMessage());
        }
    }

    /**
     * Loads the user's theme preference from a properties file.
     */
    private void loadThemePreference() {
        try {
            File configFile = getThemeConfigFile();
            if (configFile.exists()) {
                java.util.Properties props = new java.util.Properties();
                try (java.io.FileInputStream fis = new java.io.FileInputStream(configFile)) {
                    props.load(fis);
                    currentTheme = props.getProperty("theme", "light");
                }
            }
        } catch (Exception e) {
            System.err.println("Błąd podczas ładowania preferencji motywu: " + e.getMessage());
            currentTheme = "light";
        }
    }

    /**
     * Saves the user's theme preference to a properties file.
     */
    private void saveThemePreference() {
        try {
            File configFile = getThemeConfigFile();
            configFile.getParentFile().mkdirs();

            java.util.Properties props = new java.util.Properties();
            props.setProperty("theme", currentTheme);

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(configFile)) {
                props.store(fos, "JavaFX Data Table Application Theme Settings");
            }
        } catch (Exception e) {
            System.err.println("Błąd podczas zapisywania preferencji motywu: " + e.getMessage());
        }
    }

    /**
     * Gets the configuration file for storing theme preferences.
     *
     * @return the configuration file
     */
    private File getThemeConfigFile() {
        String userHome = System.getProperty("user.home");
        return new File(userHome, ".pkprojectjava" + File.separator + "config.properties");
    }

    /**
     * Filters the table based on search text.
     */
    private void filterTable(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredData.setPredicate(p -> true);
        } else {
            String lowerCaseFilter = searchText.toLowerCase();
            filteredData.setPredicate(row -> {
                if (schema == null) return true;

                // Search across all columns
                for (ColumnDefinition col : schema) {
                    Property<?> prop = row.get(col.getName());
                    if (prop != null && prop.getValue() != null) {
                        String value = prop.getValue().toString().toLowerCase();
                        if (value.contains(lowerCaseFilter)) {
                            return true;
                        }
                    }
                }
                return false;
            });
        }
        updateFilterLabel();
    }

    /**
     * Updates the filter label showing row counts.
     */
    private void updateFilterLabel() {
        if (filterLabel != null) {
            int showing = filteredData != null ? filteredData.size() : data.size();
            int total = data.size();
            filterLabel.setText(String.format("Wyświetlanie %d z %d wierszy", showing, total));
        }
    }

    /**
     * Sets up column sorting functionality.
     */
    private void setupColumnSorting() {
        // Column sorting will be automatically enabled when columns are created
        // by DynamicTableService. We just need to ensure sortable is set.
    }

    /**
     * Updates statistics panel based on selected column.
     */
    private void updateStatistics() {
        if (statsLabel == null || schema == null || schema.isEmpty()) {
            return;
        }

        // Get selected column index
        javafx.scene.control.TablePosition<?, ?> pos = table.getFocusModel().getFocusedCell();
        if (pos == null || pos.getColumn() < 0) {
            statsLabel.setText("Kliknij na komórkę kolumny, aby zobaczyć statystyki");
            return;
        }

        int colIndex = pos.getColumn();
        if (colIndex >= schema.size()) {
            return;
        }

        ColumnDefinition col = schema.get(colIndex);
        String colName = col.getName();
        Class<?> colType = col.getType();

        // Calculate statistics for numeric columns
        if (colType == Integer.class || colType == Double.class) {
            double sum = 0;
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            int count = 0;

            for (DataRow row : filteredData) {
                Property<?> prop = row.get(colName);
                if (prop != null && prop.getValue() != null) {
                    double value = ((Number) prop.getValue()).doubleValue();
                    sum += value;
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                    count++;
                }
            }

            if (count > 0) {
                double avg = sum / count;
                statsLabel.setText(String.format("Kolumna '%s': Liczba=%d | Suma=%.2f | Średnia=%.2f | Min=%.2f | Max=%.2f",
                        colName, count, sum, avg, min, max));
            } else {
                statsLabel.setText("Kolumna '" + colName + "': Brak danych numerycznych");
            }
        } else {
            // For string columns, show count
            int count = 0;
            for (DataRow row : filteredData) {
                Property<?> prop = row.get(colName);
                if (prop != null && prop.getValue() != null && !prop.getValue().toString().isEmpty()) {
                    count++;
                }
            }
            statsLabel.setText(String.format("Kolumna '%s': %d niepustych wartości", colName, count));
        }
    }

    /**
     * Toggles regression line display on chart.
     */
    @FXML
    void toggleRegression(ActionEvent event) {
        if (showRegressionCheckBox.isSelected()) {
            calculateAndShowRegression();
        } else {
            hideRegression();
        }
    }

    /**
     * Calculates and displays linear regression line(s).
     * For multi-series charts, creates a regression line for each series.
     */
    private void calculateAndShowRegression() {
        if (currentChart == null || currentChart.getData().isEmpty()) {
            showRegressionCheckBox.setSelected(false);
            return;
        }

        // Clear any existing regression series
        regressionSeriesList.clear();

        StringBuilder equationsText = new StringBuilder();
        int seriesCount = 0;
        int dataSeriesIndex = 0;

        // First, identify which series are data series (not regression series)
        List<XYChart.Series<String, Number>> dataSeries = new ArrayList<>();
        for (XYChart.Series<String, Number> series : currentChart.getData()) {
            // Skip series that are already regression lines
            if (!series.getName().startsWith("Regression:")) {
                dataSeries.add(series);
            }
        }

        // Calculate regression for each data series
        for (XYChart.Series<String, Number> series : dataSeries) {
            if (series.getData().size() < 2) {
                dataSeriesIndex++;
                continue;
            }

            // Calculate linear regression
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
            int n = series.getData().size();

            for (int i = 0; i < n; i++) {
                XYChart.Data<String, Number> point = series.getData().get(i);
                double x = i; // Use index as x value
                double y = point.getYValue().doubleValue();

                sumX += x;
                sumY += y;
                sumXY += x * y;
                sumX2 += x * x;
            }

            // Calculate slope (m) and intercept (b) for y = mx + b
            double m = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
            double b = (sumY - m * sumX) / n;

            // Calculate R²
            double meanY = sumY / n;
            double ssTotal = 0, ssResidual = 0;
            for (int i = 0; i < n; i++) {
                double y = series.getData().get(i).getYValue().doubleValue();
                double yPred = m * i + b;
                ssTotal += Math.pow(y - meanY, 2);
                ssResidual += Math.pow(y - yPred, 2);
            }
            double r2 = 1 - (ssResidual / ssTotal);

            // Create regression line series
            XYChart.Series<String, Number> regSeries = new XYChart.Series<>();
            regSeries.setName("Regression: " + series.getName());

            for (int i = 0; i < n; i++) {
                String xLabel = series.getData().get(i).getXValue();
                double yPred = m * i + b;
                regSeries.getData().add(new XYChart.Data<>(xLabel, yPred));
            }

            // Add to chart and list
            currentChart.getData().add(regSeries);
            regressionSeriesList.add(regSeries);

            // Apply dashed line style with darker color after chart renders
            final int colorIndex = dataSeriesIndex;
            final XYChart.Series<String, Number> finalRegSeries = regSeries;

            // Use multiple delayed attempts to ensure node is ready
            javafx.application.Platform.runLater(() -> {
                applyRegressionStyle(finalRegSeries, colorIndex);

                // Try again after a short delay if node wasn't ready
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(50));
                pause.setOnFinished(e -> applyRegressionStyle(finalRegSeries, colorIndex));
                pause.play();

                // One more attempt with longer delay
                javafx.animation.PauseTransition pause2 = new javafx.animation.PauseTransition(javafx.util.Duration.millis(200));
                pause2.setOnFinished(e -> applyRegressionStyle(finalRegSeries, colorIndex));
                pause2.play();
            });

            // Build equations text
            if (seriesCount > 0) {
                equationsText.append(" | ");
            }
            equationsText.append(String.format("R%d: y=%.3fx+%.3f (R²=%.3f)",
                seriesCount + 1, m, b, r2));
            seriesCount++;
            dataSeriesIndex++;
        }

        // For single series, also keep old reference for backwards compatibility
        if (regressionSeriesList.size() == 1) {
            regressionSeries = regressionSeriesList.get(0);
        }

        // Update equation label
        if (seriesCount > 0) {
            regressionEquationLabel.setText(equationsText.toString());
            logAction("Dodano " + seriesCount + " liniowych linii regresji");
        } else {
            showRegressionCheckBox.setSelected(false);
            regressionEquationLabel.setText("Brak serii z wystarczającymi danymi do regresji");
        }
    }

    /**
     * Applies the dashed line style with darker color to a regression series.
     * Hides data point symbols to show only the line.
     *
     * @param regSeries the regression series to style
     * @param colorIndex the index of the color to use
     */
    private void applyRegressionStyle(XYChart.Series<String, Number> regSeries, int colorIndex) {
        String originalColor = SERIES_COLORS[colorIndex % SERIES_COLORS.length];
        String darkerColor = darkenColor(originalColor, 0.3); // 30% darker

        // Style the line (if node is ready)
        if (regSeries.getNode() != null) {
            regSeries.getNode().setStyle(
                "-fx-stroke: " + darkerColor + "; " +
                "-fx-stroke-width: 2px; " +
                "-fx-stroke-dash-array: 5 5;" // Dashed line pattern
            );
        }

        // Hide data point symbols (make them invisible)
        for (XYChart.Data<String, Number> dataPoint : regSeries.getData()) {
            if (dataPoint.getNode() != null) {
                dataPoint.getNode().setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-padding: 0;"
                );
                dataPoint.getNode().setVisible(false);
            }
        }
    }

    /**
     * Hides the regression line(s) from chart.
     */
    private void hideRegression() {
        if (!regressionSeriesList.isEmpty() && currentChart != null) {
            int removedCount = regressionSeriesList.size();
            for (XYChart.Series<String, Number> regSeries : regressionSeriesList) {
                currentChart.getData().remove(regSeries);
            }
            regressionSeriesList.clear();
            logAction("Usunięto " + removedCount + " linii regresji");
        }

        if (regressionSeries != null && currentChart != null) {
            currentChart.getData().remove(regressionSeries);
            regressionSeries = null;
        }

        regressionEquationLabel.setText("");
    }

    /**
     * Generates a multi-series chart from multiple columns.
     */
    @FXML
    void generateMultiSeriesChart(ActionEvent event) {
        try {
            if (data.isEmpty() || schema == null || schema.size() < 2) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Niewystarczające dane");
                alert.setHeaderText(null);
                alert.setContentText("Potrzebne przynajmniej 2 kolumny do wygenerowania wykresu wieloseryjnego.");
                applyThemeToDialog(alert.getDialogPane());
                alert.showAndWait();
                return;
            }

            // Ask for X column
            List<String> columnNames = new ArrayList<>();
            for (ColumnDefinition col : schema) {
                columnNames.add(col.getName());
            }

            javafx.scene.control.ChoiceDialog<String> xDialog =
                new javafx.scene.control.ChoiceDialog<>(columnNames.get(0), columnNames);
            xDialog.setTitle("Wybór kolumny X");
            xDialog.setHeaderText("Wybierz kolumnę dla osi X:");
            applyThemeToDialog(xDialog.getDialogPane());

            Optional<String> xResult = xDialog.showAndWait();
            if (!xResult.isPresent()) return;

            String xColumn = xResult.get();
            columnNames.remove(xColumn);

            // Ask for Y columns (multiple selection dialog)
            TextInputDialog yDialog = new TextInputDialog(String.join(", ", columnNames.subList(0, Math.min(2, columnNames.size()))));
            yDialog.setTitle("Wybór kolumn Y");
            yDialog.setHeaderText("Podaj nazwy kolumn do wykresu (oddzielone przecinkami):");
            yDialog.setContentText("Kolumny:");
            applyThemeToDialog(yDialog.getDialogPane());

            Optional<String> yResult = yDialog.showAndWait();
            if (!yResult.isPresent()) return;

            String[] yColumns = yResult.get().split(",");

            // Switch to line chart for multi-series
            switchToLineChart(null);

            currentChart.getData().clear();

            // Create series for each Y column
            for (String yColName : yColumns) {
                yColName = yColName.trim();

                // Find column definition
                ColumnDefinition yCol = null;
                for (ColumnDefinition col : schema) {
                    if (col.getName().equals(yColName)) {
                        yCol = col;
                        break;
                    }
                }

                if (yCol == null) continue;

                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(yColName);

                // Add data points
                for (DataRow row : data) {
                    Property<?> xProp = row.get(xColumn);
                    Property<?> yProp = row.get(yColName);

                    if (xProp != null && yProp != null && yProp.getValue() instanceof Number) {
                        String xValue = xProp.getValue() != null ? xProp.getValue().toString() : "";
                        Number yValue = (Number) yProp.getValue();
                        series.getData().add(new XYChart.Data<>(xValue, yValue));
                    }
                }

                currentChart.getData().add(series);
            }

            currentChart.setTitle("Wykres wieloseryjny");
            currentChart.getYAxis().setLabel("Wartości");

            logAction("Wygenerowano wykres wieloseryjny (liczba serii: " + yColumns.length + ")");

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Błąd");
            alert.setHeaderText("Nie udało się wygenerować wykresu wieloseryjnego");
            alert.setContentText(e.getMessage());
            applyThemeToDialog(alert.getDialogPane());
            alert.showAndWait();
        }
    }

    /**
     * Generates a multi-series chart from multiple mathematical formulas.
     * Allows comparison of different formulas on the same chart.
     *
     * @param event the action event triggered by the user
     */
    @FXML
    void generateChartFromMultipleFormulas(ActionEvent event) {
        try {
            TextInputDialog rangeDialog = new TextInputDialog("-10, 10");
            rangeDialog.setTitle("Zakres X");
            rangeDialog.setHeaderText("Podaj zakres X (wspólny dla wszystkich wzorów):");
            rangeDialog.setContentText("Format: min, max\n(np. -10, 10)");
            applyThemeToDialog(rangeDialog.getDialogPane());

            Optional<String> rangeResult = rangeDialog.showAndWait();
            if (!rangeResult.isPresent()) {
                return;
            }

            String[] rangeParts = rangeResult.get().split(",");
            double xMin = Double.parseDouble(rangeParts[0].trim());
            double xMax = Double.parseDouble(rangeParts[1].trim());

            TextInputDialog pointsDialog = new TextInputDialog("100");
            pointsDialog.setTitle("Liczba punktów");
            pointsDialog.setHeaderText("Podaj liczbę punktów:");
            pointsDialog.setContentText("Punkty (10–1000):");
            applyThemeToDialog(pointsDialog.getDialogPane());

            Optional<String> pointsResult = pointsDialog.showAndWait();
            int numPoints = pointsResult.isPresent() ?
                Integer.parseInt(pointsResult.get().trim()) : 100;

            // Validate
            if (numPoints < 10) numPoints = 10;
            if (numPoints > 1000) numPoints = 1000;

            // Ask for formulas using custom dialog with Add/Remove buttons
            MultiFormulaDialog formulasDialog = new MultiFormulaDialog();

            // Apply theme to the dialog
            java.net.URL cssUrl = com.mycompany.pkprojectjava.App.class.getResource(currentTheme + "-theme.css");
            if (cssUrl != null) {
                formulasDialog.applyTheme(cssUrl.toExternalForm());
            }

            Optional<List<String>> formulasResult = formulasDialog.showAndWait();
            if (!formulasResult.isPresent() || formulasResult.get().isEmpty()) {
                return;
            }

            List<String> formulasList = formulasResult.get();
            String[] formulas = formulasList.toArray(new String[0]);

            // Check if table has data - ask user if they want to populate
            boolean populateTable = false;
            if (data.isEmpty()) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Uzupełnić tabelę?");
                confirmAlert.setHeaderText("Czy chcesz uzupełnić tabelę?");
                confirmAlert.setContentText("Dodać obliczone wartości do tabeli?\n(Zostaną utworzone kolumny: x, f1(x), f2(x), ...)");
                applyThemeToDialog(confirmAlert.getDialogPane());

                Optional<ButtonType> result = confirmAlert.showAndWait();
                populateTable = result.isPresent() && result.get() == ButtonType.OK;
            } else {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Nadpisać dane tabeli?");
                confirmAlert.setHeaderText("Tabela zawiera już dane");
                confirmAlert.setContentText("Czy nadpisać tabelę danymi ze wzorów?");
                applyThemeToDialog(confirmAlert.getDialogPane());

                Optional<ButtonType> result = confirmAlert.showAndWait();
                populateTable = result.isPresent() && result.get() == ButtonType.OK;
            }

            // Switch to line chart for better visualization
            switchToLineChart(null);

            // Clear existing chart data
            currentChart.getData().clear();

            double step = (xMax - xMin) / (numPoints - 1);

            // Store x values (shared across all formulas)
            List<Double> xValues = new ArrayList<>();
            for (int i = 0; i < numPoints; i++) {
                xValues.add(xMin + i * step);
            }

            // Store y values for each formula (for table population)
            List<List<Double>> allYValues = new ArrayList<>();

            // Process each formula
            int formulaIndex = 1;
            for (String formula : formulas) {
                formula = formula.trim();
                if (formula.isEmpty()) {
                    continue;
                }

                try {
                    // Use exp4j to evaluate formula
                    net.objecthunter.exp4j.ExpressionBuilder builder =
                        new net.objecthunter.exp4j.ExpressionBuilder(formula)
                            .variable("x");

                    net.objecthunter.exp4j.Expression expression = builder.build();

                    // Generate series for this formula
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("f" + formulaIndex + "(x) = " + formula);

                    List<Double> yValues = new ArrayList<>();

                    for (int i = 0; i < numPoints; i++) {
                        double x = xValues.get(i);
                        expression.setVariable("x", x);
                        double y = expression.evaluate();

                        yValues.add(y);

                        // Format x value for display
                        String xLabel = String.format("%.2f", x);
                        series.getData().add(new XYChart.Data<>(xLabel, y));
                    }

                    allYValues.add(yValues);
                    currentChart.getData().add(series);
                    formulaIndex++;

                } catch (Exception e) {
                    // Show error for this specific formula but continue with others
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Błąd wzoru");
                    alert.setHeaderText("Błąd we wzorze: " + formula);
                    alert.setContentText("Błąd: " + e.getMessage() + "\n\nTen wzór zostanie pominięty.");
                    applyThemeToDialog(alert.getDialogPane());
                    alert.showAndWait();
                }
            }

            if (currentChart.getData().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Błąd");
                alert.setHeaderText("Brak poprawnych wzorów");
                alert.setContentText("Nie udało się obliczyć żadnego wzoru. Sprawdź składnię.");
                applyThemeToDialog(alert.getDialogPane());
                alert.showAndWait();
                return;
            }

            // Update chart titles
            currentChart.setTitle("Porównanie wielu wzorów");
            currentChart.getXAxis().setLabel("x");
            currentChart.getYAxis().setLabel("f(x)");

            // Apply distinct colors to each series
            applySeriesColors();

            // Populate table if user agreed
            if (populateTable && !allYValues.isEmpty()) {
                // Create schema with x and f(x), f2(x), ... columns
                schema = new ArrayList<>();
                schema.add(new ColumnDefinition("x", Double.class));

                for (int i = 0; i < allYValues.size(); i++) {
                    schema.add(new ColumnDefinition("f" + (i + 1) + "(x)", Double.class));
                }

                // Clear existing data
                data.clear();

                // Add rows with x and all y values
                for (int i = 0; i < xValues.size(); i++) {
                    DataRow row = new DataRow();
                    row.put("x", new SimpleDoubleProperty(xValues.get(i)));

                    for (int j = 0; j < allYValues.size(); j++) {
                        row.put("f" + (j + 1) + "(x)", new SimpleDoubleProperty(allYValues.get(j).get(i)));
                    }

                    data.add(row);
                }

                // Rebuild table
                DynamicTableService.build(table, schema);

                logAction("Wygenerowano wykres z wielu wzorów i uzupełniono tabelę (wzory: " + allYValues.size() +
                         ", punkty: " + numPoints + ", zakres: " + xMin + "…" + xMax + ")");
            } else {
                logAction("Wygenerowano wykres z wielu wzorów (wzory: " + allYValues.size() +
                         ", punkty: " + numPoints + ", zakres: " + xMin + "…" + xMax + ")");
            }

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Błąd");
            alert.setHeaderText("Nie udało się wygenerować wykresu z wielu wzorów");
            alert.setContentText("Błąd: " + e.getMessage() +
                "\n\nUpewnij się, że wzory są poprawne.\nPrzykłady: 2*x+3, x^2, sin(x), cos(x)");
            applyThemeToDialog(alert.getDialogPane());
            alert.showAndWait();
        }
    }

    /**
     * Nadaje różne kolory seriom na wykresie.
     * JavaFX tworzy węzły serii asynchronicznie, więc stylowanie robimy w Platform.runLater.
     */
    private void applySeriesColors() {
        if (currentChart == null) {
            return;
        }

        javafx.application.Platform.runLater(() -> {
            int colorIndex = 0;

            for (XYChart.Series<String, Number> series : currentChart.getData()) {
                if (isRegressionSeries(series)) {
                    continue;
                }

                String color = SERIES_COLORS[colorIndex % SERIES_COLORS.length];

                // Stylowanie linii/obszarów
                if (series.getNode() != null) {
                    series.getNode().setStyle("-fx-stroke: " + color + ";");
                }

                // Stylowanie słupków/symboli punktów
                for (XYChart.Data<String, Number> point : series.getData()) {
                    if (point.getNode() == null) {
                        continue;
                    }

                    point.getNode().setStyle(
                        "-fx-bar-fill: " + color + ";" +
                        "-fx-background-color: " + color + ", white;"
                    );
                }

                colorIndex++;
            }
        });
    }

    /**
     * Sprawdza czy seria jest linią regresji (na podstawie nazwy).
     */
    private boolean isRegressionSeries(XYChart.Series<String, Number> series) {
        return series != null && series.getName() != null && series.getName().startsWith("Regression:");
    }

    /**
     * Darkens a hex color by a specified factor.
     *
     * @param hexColor the original hex color (e.g., "#1f77b4")
     * @param factor the darkening factor (0.0 to 1.0, where 0.3 means 30% darker)
     * @return the darkened hex color
     */
    private String darkenColor(String hexColor, double factor) {
        try {
            // Remove # if present
            String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;

            // Parse RGB components
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);

            // Darken each component
            r = (int) (r * (1 - factor));
            g = (int) (g * (1 - factor));
            b = (int) (b * (1 - factor));

            // Ensure values are in valid range
            r = Math.max(0, Math.min(255, r));
            g = Math.max(0, Math.min(255, g));
            b = Math.max(0, Math.min(255, b));

            // Convert back to hex
            return String.format("#%02x%02x%02x", r, g, b);
        } catch (Exception e) {
            // If parsing fails, return original color
            return hexColor;
        }
    }

}
