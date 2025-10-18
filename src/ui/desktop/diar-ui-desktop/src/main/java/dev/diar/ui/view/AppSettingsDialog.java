package dev.diar.ui.view;

import dev.diar.ui.AppSettings;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;

public class AppSettingsDialog extends Dialog<ButtonType> {
    public AppSettingsDialog(Runnable onApplied) {
        setTitle("Application Settings");
        setHeaderText("Preferences");

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

        CheckBox confirmExit = new CheckBox("Confirm on Exit");
        confirmExit.setSelected(AppSettings.isConfirmOnExit());
        CheckBox autoUpdate = new CheckBox("Enable automatic update check");
        autoUpdate.setSelected(AppSettings.isAutoUpdateEnabled());

        grid.add(accentLbl, 0, 0);
        grid.add(accentBox, 1, 0);
        grid.add(scaleLbl, 0, 1);
        grid.add(scaleSpinner, 1, 1);
        grid.add(confirmExit, 1, 2);
        grid.add(autoUpdate, 1, 3);

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        String css = getClass().getResource("/css/app.css") != null ? getClass().getResource("/css/app.css").toExternalForm() : null;
        if (css != null) getDialogPane().getStylesheets().add(css);
        getDialogPane().setStyle("-fx-background-color: #3a2f27; -fx-base: #3a2f27; -fx-control-inner-background: #2e2e2e; -fx-text-background-color: #d4c4a1; -fx-focus-color: -diar-highlight; -fx-faint-focus-color: rgba(122,106,90,0.25);");

        // Theme the OK/Cancel buttons to avoid default blue fill
        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        if (okBtn != null) {
            okBtn.setDefaultButton(true);
            okBtn.setStyle("-fx-background-color: #3a2f27; -fx-text-fill: #f4e4c1; -fx-font-weight: bold; -fx-border-color: #2a1f17; -fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6;");
        }
        Button cancelBtn = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.setStyle("-fx-background-color: #3a2f27; -fx-text-fill: #f4e4c1; -fx-font-weight: bold; -fx-border-color: #2a1f17; -fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6;");
        }

        setResultConverter(bt -> bt);
        showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                AppSettings.setAccent(accentBox.getValue());
                AppSettings.setUiScale(scaleSpinner.getValue());
                AppSettings.setConfirmOnExit(confirmExit.isSelected());
                AppSettings.setAutoUpdateEnabled(autoUpdate.isSelected());
                if (onApplied != null) onApplied.run();
            }
        });
    }
}
