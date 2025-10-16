package dev.diar.ui.view;

import dev.diar.app.service.BlockService;
import dev.diar.core.model.Category;
import dev.diar.ui.ApplicationContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class CategoryCard extends VBox {
    private final Category category;
    private final BlockService blockService;
    private final Runnable onUpdate;
    private final ApplicationContext applicationContext;

    public CategoryCard(Category category, BlockService blockService, Runnable onUpdate, ApplicationContext applicationContext) {
        this.category = category;
        this.blockService = blockService;
        this.onUpdate = onUpdate;
        this.applicationContext = applicationContext;
        
        setupUI();
    }

    private void setupUI() {
        setPadding(new Insets(15));
        setSpacing(10);
        setAlignment(Pos.CENTER_LEFT);
        setMaxWidth(500);
        setStyle(
            "-fx-background-color: #5a4a3a; " +
            "-fx-background-radius: 10; " +
            "-fx-border-color: #7a6a5a; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 10;"
        );
        
        Label nameLabel = new Label(category.name());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        nameLabel.setTextFill(Color.web("#f4e4c1"));
        
        Label targetLabel = new Label("Tower Target: " + category.towerBlockTarget() + " blocks");
        targetLabel.setFont(Font.font("System", 12));
        targetLabel.setTextFill(Color.web("#d4c4a1"));

        // Progress section
        int target = category.towerBlockTarget();
        int completed = blockService.getActiveTower(category.id())
            .map(t -> t.blocksCompleted())
            .orElse(0);
        double progress = target > 0 ? (double) completed / (double) target : 0.0;

        ProgressBar progressBar = new ProgressBar(progress);
        progressBar.setPrefWidth(400);
        progressBar.setStyle("-fx-accent: #7a9b8e;");

        Label progressLabel = new Label(completed + " / " + target + " blocks");
        progressLabel.setTextFill(Color.web("#d4c4a1"));
        progressLabel.setFont(Font.font("System", 12));
        
        Button addBlockButton = new Button("+ Add Block");
        addBlockButton.setStyle(
            "-fx-background-color: #7a9b8e; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 8 16; " +
            "-fx-background-radius: 5;"
        );
        addBlockButton.setOnAction(e -> showAddBlockDialog());

        Button viewTowersButton = new Button("🏗 View Towers");
        viewTowersButton.setOnAction(e -> new TowerGalleryDialog(applicationContext, category.id()).show());
        viewTowersButton.setStyle(
            "-fx-background-color: #9b7a8e; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 8 16; " +
            "-fx-background-radius: 5;"
        );

        Button deleteButton = new Button("🧨 Demolish Tower");
        deleteButton.setOnAction(e -> confirmAndDelete());
        deleteButton.setStyle(
            "-fx-background-color: #c74440; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 8 16; " +
            "-fx-background-radius: 5;"
        );
        
        HBox buttonBox = new HBox(10, addBlockButton, viewTowersButton, deleteButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        getChildren().addAll(nameLabel, targetLabel, progressBar, progressLabel, buttonBox);
    }

    private void showAddBlockDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Block");
        dialog.setHeaderText("Add a block to " + category.name());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField();

        TextArea noteArea = new TextArea();
        noteArea.setPrefRowCount(4);
        noteArea.setWrapText(true);

        TextField tagsField = new TextField();

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Tags:"), 0, 1);
        grid.add(tagsField, 1, 1);
        grid.add(new Label("Note:"), 0, 2);
        grid.add(noteArea, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    String title = titleField.getText().trim();
                    String body = noteArea.getText().trim();
                    StringBuilder sb = new StringBuilder();
                    if (!title.isEmpty()) sb.append(title);
                    String head = sb.toString().trim();
                    // metadata line (tags only)
                    StringBuilder meta = new StringBuilder();
                    String tagsRaw = tagsField.getText() != null ? tagsField.getText().trim() : "";
                    if (!tagsRaw.isEmpty()) {
                        String[] parts = tagsRaw.split("[,\n\r\t ]+");
                        for (String p : parts) {
                            if (p.isBlank()) continue;
                            if (meta.length() > 0) meta.append(' ');
                            if (p.startsWith("#")) meta.append(p);
                            else meta.append('#').append(p);
                        }
                    }
                    String metaLine = meta.toString();
                    String finalNote;
                    if (!body.isEmpty()) {
                        finalNote = head.isEmpty() ? body : head + "\n\n" + body;
                        if (!metaLine.isEmpty()) finalNote = finalNote + "\n\n" + metaLine;
                    } else {
                        if (head.isEmpty() && metaLine.isEmpty()) finalNote = null;
                        else if (head.isEmpty()) finalNote = metaLine;
                        else if (metaLine.isEmpty()) finalNote = head;
                        else finalNote = head + "\n\n" + metaLine;
                    }
                    blockService.addBlock(category.id(), finalNote);
                    onUpdate.run();
                    
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Block Added");
                    success.setHeaderText(null);
                    success.setContentText("Block added to " + category.name() + "! 🎉");
                    success.showAndWait();
                } catch (Exception e) {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Failed to add block: " + e.getMessage());
                    error.showAndWait();
                }
            }
        });
    }

    private void confirmAndDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Demolish Tower");
        confirm.setHeaderText("Demolish '" + category.name() + "'?");
        confirm.setContentText("This will remove the entire category. This action cannot be undone.");
        confirm.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    applicationContext.getCategoryService().deleteCategory(category.id());
                    onUpdate.run();
                } catch (Exception ex) {
                    Alert err = new Alert(Alert.AlertType.ERROR, "Failed to demolish: " + ex.getMessage());
                    err.showAndWait();
                }
            }
        });
    }
}
