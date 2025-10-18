package dev.diar.ui.view;

import dev.diar.ui.update.UpdaterService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UpdateDialog extends Dialog<ButtonType> {
    private final UpdaterService updater;
    private final UpdaterService.UpdateInfo info;

    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label progressLabel = new Label("Ready");

    public UpdateDialog(UpdaterService updater, UpdaterService.UpdateInfo info) {
        this.updater = updater;
        this.info = info;
        setTitle("Update Available");
        setHeaderText(info.name + " (" + info.tag + ")");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #3a2f27;");

        TextArea notes = new TextArea(info.body == null ? "" : info.body);
        notes.setEditable(false);
        notes.setWrapText(true);
        notes.setPrefRowCount(12);

        VBox center = new VBox(10, new Label("Release Notes:"), notes);
        center.setStyle("-fx-background-color: #3a2f27;");
        VBox.setVgrow(notes, Priority.ALWAYS);

        progressBar.setPrefWidth(420);
        progressBar.setProgress(0);
        HBox bottom = new HBox(10, progressBar, progressLabel);

        root.setCenter(center);
        root.setBottom(bottom);

        getDialogPane().setContent(root);
        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        String css = getClass().getResource("/css/app.css") != null ? getClass().getResource("/css/app.css").toExternalForm() : null;
        if (css != null) getDialogPane().getStylesheets().add(css);
        getDialogPane().setStyle("-fx-background-color: #3a2f27; -fx-base: #3a2f27; -fx-control-inner-background: #2e2e2e; -fx-text-background-color: #d4c4a1; -fx-focus-color: -diar-highlight; -fx-faint-focus-color: rgba(122,106,90,0.25);");

        ButtonType downloadBtnType = new ButtonType("Download and Install", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().add(downloadBtnType);
        Button downloadBtn = (Button) getDialogPane().lookupButton(downloadBtnType);
        downloadBtn.setDisable(info.assets.stream().noneMatch(a -> a.isWindowsInstaller()));

        downloadBtn.setOnAction(ev -> doDownloadAndInstall());
    }

    private void doDownloadAndInstall() {
        var installer = info.assets.stream().filter(UpdaterService.UpdateAsset::isWindowsInstaller).findFirst().orElse(null);
        if (installer == null) {
            progressLabel.setText("No Windows installer asset found.");
            return;
        }
        progressLabel.setText("Starting download...");
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        new Thread(() -> {
            try {
                Path dest = Files.createTempDirectory("diar-update");
                Path file = updater.download(installer, dest, (read, total) -> Platform.runLater(() -> {
                    if (total > 0) {
                        double p = Math.max(0, Math.min(1, read / (double) total));
                        progressBar.setProgress(p);
                        progressLabel.setText(String.format("%.0f%%", p * 100));
                    } else {
                        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                        progressLabel.setText(read / 1024 + " KB");
                    }
                }));
                Platform.runLater(() -> {
                    progressLabel.setText("Download complete. Launching installer...");
                    progressBar.setProgress(1);
                });
                launchInstaller(file);
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    progressLabel.setText("Update failed: " + ex.getMessage());
                    progressBar.setProgress(0);
                });
            }
        }, "updater-download").start();
    }

    private void launchInstaller(Path file) throws IOException {
        String name = file.getFileName().toString().toLowerCase();
        ProcessBuilder pb;
        if (name.endsWith(".msi")) {
            pb = new ProcessBuilder("msiexec", "/i", file.toAbsolutePath().toString());
        } else if (name.endsWith(".exe")) {
            pb = new ProcessBuilder(file.toAbsolutePath().toString());
        } else {
            throw new IOException("Unsupported installer: " + name);
        }
        pb.inheritIO().start();
        Platform.exit();
    }
}
