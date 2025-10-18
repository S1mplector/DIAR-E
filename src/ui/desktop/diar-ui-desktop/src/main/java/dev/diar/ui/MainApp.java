package dev.diar.ui;

import dev.diar.ui.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class MainApp extends Application {
    private static ApplicationContext applicationContext;

    public static void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }

    @Override
    public void start(Stage stage) {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext not set. Call setApplicationContext before launching.");
        }

        MainView mainView = new MainView(
            applicationContext.getCategoryService(),
            applicationContext.getBlockService(),
            applicationContext.getRecordingService(),
            applicationContext
        );

        Scene scene = new Scene(mainView, 900, 700);
        String css = getClass().getResource("/css/app.css") != null ? getClass().getResource("/css/app.css").toExternalForm() : null;
        if (css != null) {
            scene.getStylesheets().add(css);
        }
        // Apply settings early (accent + scale)
        mainView.applyAppSettings();

        stage.setTitle("DIAR-E - Daily Achievement Logger");
        stage.setScene(scene);
        try {
            var iconUrl = getClass().getResource("/images/plant.png");
            if (iconUrl != null) {
                stage.getIcons().add(new Image(iconUrl.toExternalForm()));
            }
        } catch (Exception ignored) { }
        stage.setOnCloseRequest(ev -> {
            if (AppSettings.isConfirmOnExit()) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Exit DIAR-E?", ButtonType.OK, ButtonType.CANCEL);
                if (css != null) confirm.getDialogPane().getStylesheets().add(css);
                var res = confirm.showAndWait();
                if (res.isEmpty() || res.get() != ButtonType.OK) {
                    ev.consume();
                }
            }
        });
        stage.show();

        // Optional auto-update check on startup (silent)
        javafx.application.Platform.runLater(() -> {
            if (AppSettings.isAutoUpdateEnabled()) {
                mainView.checkForUpdates(true);
            }
        });
    }
}
