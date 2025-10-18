package dev.diar.ui.view;

import dev.diar.app.service.BlockService;
import dev.diar.core.model.Category;
import dev.diar.ui.ApplicationContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.HPos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;

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
        
        String targetText = (category.towerBlockTarget() == Integer.MAX_VALUE) ? "No limit (∞)" : ("Tower Target: " + category.towerBlockTarget() + " blocks");
        Label targetLabel = new Label(targetText);
        targetLabel.setFont(Font.font("System", 12));
        targetLabel.setTextFill(Color.web("#d4c4a1"));

        // Progress section
        int target = category.towerBlockTarget();
        boolean infinite = (target == Integer.MAX_VALUE);
        int completed = blockService.getActiveTower(category.id())
            .map(t -> t.blocksCompleted())
            .orElse(0);
        double progress = infinite ? ProgressBar.INDETERMINATE_PROGRESS : (target > 0 ? (double) completed / (double) target : 0.0);

        ProgressBar progressBar = new ProgressBar(progress);
        progressBar.setPrefWidth(400);
        progressBar.setStyle("-fx-accent: #7a9b8e;");

        String progressText = infinite ? (completed + " blocks") : (completed + " / " + target + " blocks");
        Label progressLabel = new Label(progressText);
        progressLabel.setTextFill(Color.web("#d4c4a1"));
        progressLabel.setFont(Font.font("System", 12));
        
        // Load button icons
        ImageView addBlockIcon = null;
        try {
            var res = getClass().getResource("/images/block.png");
            if (res != null) {
                Image img = new Image(res.toExternalForm(), 18, 18, true, true);
                addBlockIcon = new ImageView(img);
                addBlockIcon.setFitWidth(18);
                addBlockIcon.setFitHeight(18);
                addBlockIcon.setPreserveRatio(true);
            }
        } catch (Exception ignored) {}
        ImageView viewTowersIcon = null;
        try {
            var res = getClass().getResource("/images/tower.png");
            if (res != null) {
                Image img = new Image(res.toExternalForm(), 18, 18, true, true);
                viewTowersIcon = new ImageView(img);
                viewTowersIcon.setFitWidth(18);
                viewTowersIcon.setFitHeight(18);
                viewTowersIcon.setPreserveRatio(true);
            }
        } catch (Exception ignored) {}

        Button addBlockButton = new Button("Add Block");
        addBlockButton.setStyle(
            "-fx-background-color: #7a9b8e; " +
            "-fx-text-fill: #f4e4c1; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 8 16; " +
            "-fx-background-radius: 5;"
        );
        if (addBlockIcon != null) {
            addBlockButton.setGraphic(addBlockIcon);
            addBlockButton.setContentDisplay(ContentDisplay.RIGHT);
            addBlockButton.setGraphicTextGap(8);
        }
        addBlockButton.setOnAction(e -> showAddBlockDialog());

        Button viewTowersButton = new Button("View Towers");
        viewTowersButton.setOnAction(e -> new TowerGalleryDialog(applicationContext, category.id()).show());
        viewTowersButton.setStyle(
            "-fx-background-color: #9b7a8e; " +
            "-fx-text-fill: #f4e4c1; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 8 16; " +
            "-fx-background-radius: 5;"
        );
        if (viewTowersIcon != null) {
            viewTowersButton.setGraphic(viewTowersIcon);
            viewTowersButton.setContentDisplay(ContentDisplay.RIGHT);
            viewTowersButton.setGraphicTextGap(8);
        }

        Button deleteButton = new Button("Demolish Tower");
        deleteButton.setOnAction(e -> confirmAndDelete());
        deleteButton.setStyle(
            "-fx-background-color: #c74440; " +
            "-fx-text-fill: #f4e4c1; " +
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
        titleField.setStyle("-fx-background-color: #5a4a3a; -fx-control-inner-background: #2e2e2e; -fx-text-fill: #f4e4c1;");

        TextArea noteArea = new TextArea();
        noteArea.setPrefRowCount(4);
        noteArea.setWrapText(true);
        noteArea.setStyle("-fx-background-color: #5a4a3a; -fx-control-inner-background: #2e2e2e; -fx-text-fill: #f4e4c1;");

        // Block image preview
        Image blockImg = null;
        try {
            var res = getClass().getResource("/images/block.png");
            if (res != null) blockImg = new Image(res.toExternalForm());
        } catch (Exception ignored) {}
        if (blockImg == null) {
            File catBlock = new File(System.getProperty("user.home"), ".diar-e/assets/blocks/" + category.id() + ".png");
            File defBlock = new File(System.getProperty("user.home"), ".diar-e/assets/blocks/default.png");
            File chosen = catBlock.exists() ? catBlock : (defBlock.exists() ? defBlock : null);
            if (chosen != null) blockImg = new Image(chosen.toURI().toString());
        }
        ImageView preview = new ImageView(blockImg);
        if (blockImg != null) {
            preview.setFitWidth(96);
            preview.setFitHeight(96);
            preview.setPreserveRatio(true);
        }

        GridPane.setColumnSpan(preview, 2);
        GridPane.setHalignment(preview, HPos.CENTER);
        grid.add(preview, 0, 0);
        Label titleLbl = new Label("Title:");
        titleLbl.setStyle("-fx-text-fill: #d4c4a1;");
        Label descLbl = new Label("Description:");
        descLbl.setStyle("-fx-text-fill: #d4c4a1;");
        grid.add(titleLbl, 0, 1);
        grid.add(titleField, 1, 1);
        grid.add(descLbl, 0, 2);
        grid.add(noteArea, 1, 2);

        // Static background image via CSS to avoid motion/reflow artifacts
        StackPane container = new StackPane();
        container.setStyle("-fx-background-image: url('/images/towers.jpg'); " +
                           "-fx-background-size: cover; " +
                           "-fx-background-position: center center; " +
                           "-fx-background-repeat: no-repeat;");
        grid.setStyle("-fx-background-color: rgba(58,47,39,0.82); -fx-background-radius: 10;");
        StackPane.setAlignment(grid, Pos.CENTER);
        container.getChildren().add(grid);
        dialog.getDialogPane().setContent(container);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        String css = getClass().getResource("/css/app.css") != null ? getClass().getResource("/css/app.css").toExternalForm() : null;
        if (css != null) {
            dialog.getDialogPane().getStylesheets().add(css);
        }
        // Match Create Category dialog styling
        dialog.getDialogPane().setStyle("-fx-background-color: #3a2f27; -fx-base: #3a2f27; -fx-control-inner-background: #2e2e2e; -fx-text-background-color: #d4c4a1; -fx-focus-color: #FFC107; -fx-faint-focus-color: rgba(255,193,7,0.20);");
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        if (okBtn != null) {
            okBtn.setStyle("-fx-background-color: #3a2f27; -fx-text-fill: #f4e4c1; -fx-font-weight: bold; -fx-border-color: #2a1f17; -fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6;");
        }
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.setStyle("-fx-background-color: #3a2f27; -fx-text-fill: #f4e4c1; -fx-font-weight: bold; -fx-border-color: #2a1f17; -fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6;");
        }
        dialog.setOnShown(ev -> {
            javafx.scene.Node header = dialog.getDialogPane().lookup(".header-panel");
            if (header != null) header.setStyle("-fx-background-color: #5a4a3a;");
            javafx.scene.Node contentReg = dialog.getDialogPane().lookup(".content");
            if (contentReg != null) contentReg.setStyle("-fx-background-color: #3a2f27;");
            javafx.scene.Node buttonBar = dialog.getDialogPane().lookup(".button-bar");
            if (buttonBar != null) buttonBar.setStyle("-fx-background-color: #5a4a3a;");
        });
        
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    String title = titleField.getText().trim();
                    String body = noteArea.getText().trim();
                    String finalNote;
                    if (!title.isEmpty() && !body.isEmpty()) finalNote = title + "\n\n" + body;
                    else if (!title.isEmpty()) finalNote = title;
                    else if (!body.isEmpty()) finalNote = body;
                    else finalNote = null;
                    blockService.addBlock(category.id(), finalNote);
                    onUpdate.run();
                    
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Block Added");
                    success.setHeaderText(null);
                    success.setContentText("Block added to " + category.name() + "!");
                    if (css != null) success.getDialogPane().getStylesheets().add(css);
                    // Match Create Category dialog visual style
                    success.getDialogPane().setStyle("-fx-background-color: #3a2f27; -fx-base: #3a2f27; -fx-control-inner-background: #2e2e2e; -fx-text-background-color: #d4c4a1; -fx-focus-color: #FFC107; -fx-faint-focus-color: rgba(255,193,7,0.20);");
                    Button okBtn2 = (Button) success.getDialogPane().lookupButton(ButtonType.OK);
                    if (okBtn2 != null) {
                        okBtn2.setStyle("-fx-background-color: #3a2f27; -fx-text-fill: #f4e4c1; -fx-font-weight: bold; -fx-border-color: #2a1f17; -fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6;");
                    }
                    success.setOnShown(ev2 -> {
                        javafx.scene.Node header2 = success.getDialogPane().lookup(".header-panel");
                        if (header2 != null) header2.setStyle("-fx-background-color: #5a4a3a;");
                        javafx.scene.Node contentReg2 = success.getDialogPane().lookup(".content");
                        if (contentReg2 != null) contentReg2.setStyle("-fx-background-color: #3a2f27;");
                        javafx.scene.Node buttonBar2 = success.getDialogPane().lookup(".button-bar");
                        if (buttonBar2 != null) buttonBar2.setStyle("-fx-background-color: #5a4a3a;");
                    });
                    success.showAndWait();
                } catch (Exception e) {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Failed to add block: " + e.getMessage());
                    if (css != null) error.getDialogPane().getStylesheets().add(css);
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
        String css = getClass().getResource("/css/app.css") != null ? getClass().getResource("/css/app.css").toExternalForm() : null;
        if (css != null) confirm.getDialogPane().getStylesheets().add(css);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    applicationContext.getCategoryService().deleteCategory(category.id());
                    onUpdate.run();
                } catch (Exception ex) {
                    Alert err = new Alert(Alert.AlertType.ERROR, "Failed to demolish: " + ex.getMessage());
                    if (css != null) err.getDialogPane().getStylesheets().add(css);
                    err.showAndWait();
                }
            }
        });
    }
}
