package dev.diar.ui.view;

import dev.diar.app.service.CategoryService;
import dev.diar.app.service.LogQueryService;
import dev.diar.app.service.RecordingService;
import dev.diar.app.service.TowerViewService;
import dev.diar.core.model.Category;
import dev.diar.ui.ApplicationContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class SettingsDialog extends Stage {
    private final ApplicationContext context;

    private Label dataFolderValue;
    private Label dbPathValue;
    private Label dbSizeValue;
    private Label categoriesCountValue;
    private Label towersCountValue;
    private Label logsCountValue;
    private Label recCountDbValue;
    private Label recCountDiskValue;
    private Label recSizeValue;
    private Label assetsBlocksCountValue;
    private Label assetsBlocksSizeValue;
    private Label totalManagedSizeValue;

    public SettingsDialog(ApplicationContext context) {
        this.context = context;
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Settings — Storage");
        setupUI();
        refreshStats();
    }

    private void setupUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));

        Label header = new Label("Storage Overview");
        header.setStyle("-fx-font-size: 18px; -fx-text-fill: #f4e4c1;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);

        int r = 0;
        dataFolderValue = addRow(grid, r++, "Data Folder:");
        dbPathValue = addRow(grid, r++, "Database Path:");
        dbSizeValue = addRow(grid, r++, "Database Size:");
        grid.add(new Separator(), 0, r++, 2, 1);
        categoriesCountValue = addRow(grid, r++, "Categories:");
        towersCountValue = addRow(grid, r++, "Towers:");
        logsCountValue = addRow(grid, r++, "Blocks (Logs):");
        grid.add(new Separator(), 0, r++, 2, 1);
        recCountDbValue = addRow(grid, r++, "Recordings (DB):");
        recCountDiskValue = addRow(grid, r++, "Recordings (Files):");
        recSizeValue = addRow(grid, r++, "Recordings Size:");
        grid.add(new Separator(), 0, r++, 2, 1);
        assetsBlocksCountValue = addRow(grid, r++, "Block Assets (files):");
        assetsBlocksSizeValue = addRow(grid, r++, "Block Assets Size:");
        grid.add(new Separator(), 0, r++, 2, 1);
        totalManagedSizeValue = addRow(grid, r++, "Total Managed Size:");

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> refreshStats());
        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> close());
        HBox actions = new HBox(10, refreshBtn, closeBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(header, grid, actions);

        Scene scene = new Scene(root, 640, 520);
        String css = getClass().getResource("/css/app.css") != null ? getClass().getResource("/css/app.css").toExternalForm() : null;
        if (css != null) scene.getStylesheets().add(css);
        root.setStyle("-fx-background-color: #3a2f27; -fx-text-fill: #d4c4a1;");
        scene.getRoot().setStyle("-fx-base: #3a2f27; -fx-control-inner-background: #2e2e2e; -fx-text-background-color: #d4c4a1;");
        setScene(scene);
    }

    private Label addRow(GridPane grid, int row, String name) {
        Label label = new Label(name);
        label.setStyle("-fx-text-fill: #d4c4a1;");
        Label value = new Label("");
        value.setStyle("-fx-text-fill: #f4e4c1;");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
        return value;
    }

    private void refreshStats() {
        // Paths
        Path appDir = Path.of(System.getProperty("user.home"), ".diar-e");
        Path dataDir = appDir.resolve("data");
        Path dbPath = dataDir.resolve("diar.db");
        Path recordingsDir = appDir.resolve("recordings");
        Path assetsBlocksDir = appDir.resolve("assets").resolve("blocks");

        dataFolderValue.setText(appDir.toAbsolutePath().toString());
        dbPathValue.setText(dbPath.toAbsolutePath().toString());
        dbSizeValue.setText(formatBytes(fileSize(dbPath)));

        // Counts via services
        CategoryService categoryService = context.getCategoryService();
        TowerViewService towerViewService = context.getTowerViewService();
        LogQueryService logQueryService = context.getLogQueryService();
        RecordingService recordingService = context.getRecordingService();

        List<Category> categories = categoryService.getAllCategories();
        categoriesCountValue.setText(String.valueOf(categories.size()));

        long towers = 0;
        long logs = 0;
        ZonedDateTime from = ZonedDateTime.of(1970,1,1,0,0,0,0, ZoneId.systemDefault());
        ZonedDateTime to = ZonedDateTime.now();
        for (Category c : categories) {
            towers += towerViewService.towersForCategory(c.id()).size();
            logs += logQueryService.logsByCategory(c.id(), from, to).size();
        }
        towersCountValue.setText(String.valueOf(towers));
        logsCountValue.setText(String.valueOf(logs));

        // Recordings
        var allRecs = recordingService.getAllRecordings();
        recCountDbValue.setText(String.valueOf(allRecs.size()));
        long recFiles = countFiles(recordingsDir, "wav");
        long recBytes = dirSize(recordingsDir);
        recCountDiskValue.setText(String.valueOf(recFiles));
        recSizeValue.setText(formatBytes(recBytes));

        // Assets
        long assetsCount = countFiles(assetsBlocksDir, null); // all files
        long assetsBytes = dirSize(assetsBlocksDir);
        assetsBlocksCountValue.setText(String.valueOf(assetsCount));
        assetsBlocksSizeValue.setText(formatBytes(assetsBytes));

        // Total managed size (db + recordings + assets)
        long total = fileSize(dbPath) + recBytes + assetsBytes;
        totalManagedSizeValue.setText(formatBytes(total));
    }

    private static long fileSize(Path p) {
        try {
            if (p != null && Files.exists(p)) return Files.size(p);
        } catch (IOException ignored) {}
        return 0L;
    }

    private static long dirSize(Path dir) {
        if (dir == null || !Files.exists(dir)) return 0L;
        try {
            try (var s = Files.walk(dir)) {
                return s.filter(Files::isRegularFile)
                        .mapToLong(path -> {
                            try { return Files.size(path); } catch (IOException e) { return 0L; }
                        })
                        .sum();
            }
        } catch (IOException e) {
            return 0L;
        }
    }

    private static long countFiles(Path dir, String extensionLower) {
        if (dir == null || !Files.exists(dir)) return 0L;
        try (var s = Files.walk(dir)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> extensionLower == null || p.getFileName().toString().toLowerCase().endsWith("." + extensionLower))
                    .count();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }
}
