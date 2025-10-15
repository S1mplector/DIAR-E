package dev.diar.ui;

import dev.diar.ui.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
        stage.setTitle("DIAR-E - Daily Achievement Logger");
        stage.setScene(scene);
        stage.show();
    }
}
