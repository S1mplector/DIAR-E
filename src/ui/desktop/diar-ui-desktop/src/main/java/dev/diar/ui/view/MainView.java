package dev.diar.ui.view;

import dev.diar.app.service.BlockService;
import dev.diar.app.service.CategoryService;
import dev.diar.app.service.RecordingService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

public class MainView extends BorderPane {
    private final CategoryService categoryService;
    private final BlockService blockService;
    private final RecordingService recordingService;
    
    private VBox categoryListView;
    private Label statusLabel;

    public MainView(CategoryService categoryService, BlockService blockService, RecordingService recordingService) {
        this.categoryService = categoryService;
        this.blockService = blockService;
        this.recordingService = recordingService;
        
        setupUI();
        loadCategories();
    }

    private void setupUI() {
        // Set WALL-E inspired brown/rusty theme
        setStyle("-fx-background-color: #3a2f27;");
        
        // Header
        VBox header = createHeader();
        setTop(header);
        
        // Center content
        ScrollPane centerContent = createCenterContent();
        setCenter(centerContent);
        
        // Bottom status bar
        HBox statusBar = createStatusBar();
        setBottom(statusBar);
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(20));
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: #5a4a3a;");
        
        Label titleLabel = new Label("DIAR-E");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.web("#f4e4c1"));
        
        Label subtitleLabel = new Label("Daily Achievement Logger");
        subtitleLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitleLabel.setTextFill(Color.web("#d4c4a1"));
        
        Button addCategoryButton = new Button("+ New Category");
        styleButton(addCategoryButton, "#7a9b8e");
        addCategoryButton.setOnAction(e -> showAddCategoryDialog());
        
        Button recordingButton = new Button("🎤 Audio Diary");
        styleButton(recordingButton, "#9b7a8e");
        recordingButton.setOnAction(e -> showRecordingDialog());
        
        HBox buttonBox = new HBox(10, addCategoryButton, recordingButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        header.getChildren().addAll(titleLabel, subtitleLabel, buttonBox);
        return header;
    }

    private ScrollPane createCenterContent() {
        categoryListView = new VBox(15);
        categoryListView.setPadding(new Insets(20));
        categoryListView.setAlignment(Pos.TOP_CENTER);
        
        ScrollPane scrollPane = new ScrollPane(categoryListView);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #3a2f27; -fx-background-color: #3a2f27;");
        
        return scrollPane;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox();
        statusBar.setPadding(new Insets(10));
        statusBar.setStyle("-fx-background-color: #2a1f17;");
        
        statusLabel = new Label("Ready");
        statusLabel.setTextFill(Color.web("#d4c4a1"));
        statusLabel.setFont(Font.font("Monospace", 12));
        
        statusBar.getChildren().add(statusLabel);
        return statusBar;
    }

    private void loadCategories() {
        categoryListView.getChildren().clear();
        
        var categories = categoryService.getAllCategories();
        if (categories.isEmpty()) {
            Label emptyLabel = new Label("No categories yet. Add one to start building towers!");
            emptyLabel.setTextFill(Color.web("#d4c4a1"));
            emptyLabel.setFont(Font.font("System", FontPosture.ITALIC, 14));
            categoryListView.getChildren().add(emptyLabel);
        } else {
            for (var category : categories) {
                categoryListView.getChildren().add(new CategoryCard(category, blockService, this::loadCategories));
            }
        }
    }

    private void showAddCategoryDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("New Category");
        dialog.setHeaderText("Create a new achievement category");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Category name (e.g., 'Exercise', 'Reading')");
        
        Spinner<Integer> targetSpinner = new Spinner<>();
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(5, Integer.MAX_VALUE, 10);
        targetSpinner.setValueFactory(valueFactory);
        targetSpinner.setEditable(true);
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Tower Target:"), 0, 1);
        grid.add(targetSpinner, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK && !nameField.getText().isBlank()) {
                try {
                    categoryService.createCategory(nameField.getText(), targetSpinner.getValue());
                    loadCategories();
                    updateStatus("Category created: " + nameField.getText());
                } catch (Exception e) {
                    showError("Failed to create category: " + e.getMessage());
                }
            }
        });
    }

    private void showRecordingDialog() {
        RecordingDialog dialog = new RecordingDialog(recordingService);
        dialog.showAndWait();
    }

    private void styleButton(Button button, String bgColor) {
        button.setStyle(
            "-fx-background-color: " + bgColor + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 10 20; " +
            "-fx-background-radius: 5;"
        );
        button.setOnMouseEntered(e -> button.setOpacity(0.8));
        button.setOnMouseExited(e -> button.setOpacity(1.0));
    }

    public void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }
}
