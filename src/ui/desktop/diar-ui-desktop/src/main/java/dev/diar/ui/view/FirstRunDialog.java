package dev.diar.ui.view;

import dev.diar.ui.AppSettings;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;

public class FirstRunDialog extends Dialog<ButtonType> {
    public FirstRunDialog(Runnable onApplied) {
        setTitle("Welcome to DIAR-E");
        setHeaderText("Initial Setup");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: #3a2f27;");

        Label accentLbl = new Label("Accent:");
        ComboBox<AppSettings.Accent> accentBox = new ComboBox<>();
        accentBox.getItems().setAll(AppSettings.Accent.values());
        accentBox.getSelectionModel().select(AppSettings.getAccent());

        Label scaleLbl = new Label("UI Scale:");
        Spinner<Double> scaleSpinner = new Spinner<>();
        SpinnerValueFactory.DoubleSpinnerValueFactory vf =
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.75, 2.0, AppSettings.getUiScale(), 0.05);
        scaleSpinner.setValueFactory(vf);
        scaleSpinner.setEditable(true);

        CheckBox autoUpdate = new CheckBox("Enable automatic update check");
        autoUpdate.setSelected(AppSettings.isAutoUpdateEnabled());

        grid.add(accentLbl, 0, 0);
        grid.add(accentBox, 1, 0);
        grid.add(scaleLbl, 0, 1);
        grid.add(scaleSpinner, 1, 1);
        grid.add(autoUpdate, 1, 2);

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK);
        String css = getClass().getResource("/css/app.css") != null ? getClass().getResource("/css/app.css").toExternalForm() : null;
        if (css != null) getDialogPane().getStylesheets().add(css);
        getDialogPane().setStyle("-fx-background-color: #3a2f27; -fx-base: #3a2f27; -fx-control-inner-background: #2e2e2e; -fx-text-background-color: #d4c4a1; -fx-focus-color: -diar-highlight; -fx-faint-focus-color: rgba(122,106,90,0.25);");

        setResultConverter(bt -> bt);
        showAndWait().ifPresent(result -> {
            AppSettings.setAccent(accentBox.getValue());
            AppSettings.setUiScale(scaleSpinner.getValue());
            AppSettings.setAutoUpdateEnabled(autoUpdate.isSelected());
            AppSettings.setFirstRun(false);
            if (onApplied != null) onApplied.run();
        });
    }
}
