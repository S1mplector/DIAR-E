package dev.diar.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane(new Label("DIAR-E: Mood & Achievement Logger"));
        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("DIAR-E");
        stage.setScene(scene);
        stage.show();
    }
}
