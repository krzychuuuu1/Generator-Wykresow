package com.mycompany.pkprojectjava;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Punkt startowy aplikacji JavaFX.
 *
 * Uruchamia okno główne i ładuje widok z pliku FXML.
 */
public class App extends Application {

    /** Główna scena aplikacji (wspólna dla widoków). */
    private static Scene scene;

    public static Scene getScene() {
        return scene;
    }

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("primary"), 640, 680);
        stage.setScene(scene);
        stage.setTitle("Moja aplikacja: tabela + wykres (v1)");
        stage.setMaximized(true);
        stage.show();
    }

    /** Podmienia root sceny na inny widok (FXML). */
    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    /** Ładuje plik FXML i zwraca jego węzeł root. */
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}
