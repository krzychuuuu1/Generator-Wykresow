package com.mycompany.pkprojectjava.controller;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.ArrayList;

/** Okno dialogowe do wpisania wielu wzorów (serii) na jeden wykres. */
public class MultiFormulaDialog extends Dialog<List<String>> {

    private final ListView<String> formulaListView;
    private final ObservableList<String> formulas;
    private final TextField formulaInput;

    public MultiFormulaDialog() {
        setTitle("Wiele wzorów");
        setHeaderText("Dodaj wzory do wykresu (użyj zmiennej 'x')");

        formulas = FXCollections.observableArrayList();
        formulaListView = new ListView<>(formulas);
        formulaListView.setPrefHeight(200);
        formulaListView.setPrefWidth(450);

        // Przykładowe wzory (możesz usunąć, jeśli wolisz pustą listę na start)
        formulas.addAll("2*x + 3", "x^2", "sin(x)");

        formulaInput = new TextField();
        formulaInput.setPromptText("Wpisz wzór (np. cos(x), x^3, e^x)");
        HBox.setHgrow(formulaInput, Priority.ALWAYS);

        Button addButton = new Button("Dodaj");
        addButton.setOnAction(e -> addFormula());

        Button removeButton = new Button("Usuń zaznaczony");
        removeButton.setOnAction(e -> removeFormula());

        Button clearButton = new Button("Wyczyść listę");
        clearButton.setOnAction(e -> formulas.clear());

        // Enter w polu dodaje wzór (zamiast zamykać okno)
        formulaInput.setOnAction(e -> addFormula());

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label infoLabel = new Label("Przykłady: 2*x+3, x^2, sin(x), cos(x), e^x, log(x), sqrt(x)");
        infoLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        HBox inputBox = new HBox(10);
        inputBox.getChildren().addAll(formulaInput, addButton);

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(removeButton, clearButton);

        Label listLabel = new Label("Wzory do narysowania:");

        content.getChildren().addAll(
            infoLabel,
            new Separator(),
            inputBox,
            buttonBox,
            new Separator(),
            listLabel,
            formulaListView
        );

        getDialogPane().setContent(content);

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.disableProperty().bind(javafx.beans.binding.Bindings.isEmpty(formulas));

        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new ArrayList<>(formulas);
            }
            return null;
        });

        getDialogPane().setPrefWidth(500);
    }

    /** Dodaje wzór z pola tekstowego do listy. */
    private void addFormula() {
        String formula = formulaInput.getText().trim();
        if (!formula.isEmpty()) {
            formulas.add(formula);
            formulaInput.clear();
            formulaInput.requestFocus();
        }
    }

    /** Usuwa zaznaczony wzór z listy. */
    private void removeFormula() {
        int selectedIndex = formulaListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            formulas.remove(selectedIndex);
        }
    }

    /** Nakłada CSS (motyw) na okno dialogowe. */
    public void applyTheme(String cssUrl) {
        if (cssUrl != null) {
            getDialogPane().getStylesheets().clear();
            getDialogPane().getStylesheets().add(cssUrl);
        }
    }
}
