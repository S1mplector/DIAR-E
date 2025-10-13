package dev.diar.ui.view;

import dev.diar.app.service.BlockService;
import dev.diar.core.model.Category;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class CategoryCard extends VBox {
    private final Category category;
    private final BlockService blockService;
    private final Runnable onUpdate;

    public CategoryCard(Category category, BlockService blockService, Runnable onUpdate) {
        this.category = category;
        this.blockService = blockService;
        this.onUpdate = onUpdate;
        
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
        
        HBox buttonBox = new HBox(addBlockButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        getChildren().addAll(nameLabel, targetLabel, progressBar, progressLabel, buttonBox);
    }

    private void showAddBlockDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Block");
        dialog.setHeaderText("Add a block to " + category.name());
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        Label label = new Label("Note (optional):");
        TextArea noteArea = new TextArea();
        noteArea.setPromptText("What did you achieve?");
        noteArea.setPrefRowCount(3);
        noteArea.setWrapText(true);
        
        content.getChildren().addAll(label, noteArea);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    String note = noteArea.getText().trim();
                    blockService.addBlock(category.id(), note.isEmpty() ? null : note);
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
}
