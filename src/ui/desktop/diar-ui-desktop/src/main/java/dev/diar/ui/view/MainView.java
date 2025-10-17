package dev.diar.ui.view;

import dev.diar.app.service.BlockService;
import dev.diar.app.service.CategoryService;
import dev.diar.app.service.RecordingService;
import dev.diar.ui.ApplicationContext;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

public class MainView extends BorderPane {
    private final CategoryService categoryService;
    private final BlockService blockService;
    private final RecordingService recordingService;
    private final ApplicationContext applicationContext;
    
    private TilePane categoryGrid;
    private Label statusLabel;
    private Label energyLabel;
    private Timeline energyRefreshTimer;

    public MainView(CategoryService categoryService, BlockService blockService, RecordingService recordingService, ApplicationContext applicationContext) {
        this.categoryService = categoryService;
        this.blockService = blockService;
        this.recordingService = recordingService;
        this.applicationContext = applicationContext;
        
        setupUI();
        loadCategories();
    }

    private void setupUI() {
        // Set WALL-E inspired brown/rusty theme
        setStyle("-fx-background-color: #3a2f27;");
        
        // Menu + Header
        VBox topBox = new VBox();
        topBox.getChildren().add(createMenuBar());
        topBox.getChildren().add(createHeader());
        setTop(topBox);
        
        // Center content
        ScrollPane centerContent = createCenterContent();
        setCenter(centerContent);
        
        // Bottom status bar
        HBox statusBar = createStatusBar();
        setBottom(statusBar);
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("File");
        MenuItem exportItem = new MenuItem("Export...");
        exportItem.setOnAction(e -> doExport());
        MenuItem importItem = new MenuItem("Import...");
        importItem.setOnAction(e -> doImport());
        fileMenu.getItems().addAll(exportItem, importItem);

        Menu toolsMenu = new Menu("Tools");
        MenuItem morningRoutineItem = new MenuItem("Run Morning Routine");
        morningRoutineItem.setOnAction(e -> doMorningRoutine());
        toolsMenu.getItems().add(morningRoutineItem);

        Menu settingsMenu = new Menu("Settings");
        MenuItem storageItem = new MenuItem("Storage...");
        storageItem.setOnAction(e -> new SettingsDialog(applicationContext).showAndWait());
        settingsMenu.getItems().add(storageItem);

        menuBar.getMenus().addAll(fileMenu, toolsMenu, settingsMenu);
        return menuBar;
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(20));
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: #5a4a3a;");
        
        ImageView logoView = null;
        try {
            var url = getClass().getResource("/images/walle.png");
            if (url != null) {
                Image img = new Image(url.toExternalForm(), 48, 48, true, true);
                logoView = new ImageView(img);
                logoView.setFitWidth(48);
                logoView.setFitHeight(48);
                logoView.setPreserveRatio(true);
            }
        } catch (Exception ignored) { }
        
        Label titleLabel = new Label("DIAR-E");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.web("#f4e4c1"));
        
        HBox titleBox = new HBox(12);
        titleBox.setAlignment(Pos.CENTER);
        if (logoView != null) {
            titleBox.getChildren().addAll(logoView, titleLabel);
        } else {
            titleBox.getChildren().add(titleLabel);
        }
        
        Label subtitleLabel = new Label("Daily Achievement Logger");
        subtitleLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitleLabel.setTextFill(Color.web("#d4c4a1"));
        
        Button addCategoryButton = new Button("Start New Tower");
        styleButton(addCategoryButton, "#7a9b8e");
        addCategoryButton.setOnAction(e -> showAddCategoryDialog());
        
        Button recordingButton = new Button("Audio Diary");
        styleButton(recordingButton, "#9b7a8e");
        recordingButton.setOnAction(e -> showRecordingDialog());
        
        HBox buttonBox = new HBox(10, addCategoryButton, recordingButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        header.getChildren().addAll(titleBox, subtitleLabel, buttonBox);
        return header;
    }

    private ScrollPane createCenterContent() {
        categoryGrid = new TilePane();
        categoryGrid.setPadding(new Insets(20));
        categoryGrid.setHgap(16);
        categoryGrid.setVgap(16);
        categoryGrid.setPrefColumns(2);
        categoryGrid.setTileAlignment(Pos.TOP_LEFT);

        ScrollPane scrollPane = new ScrollPane(categoryGrid);
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        energyLabel = new Label("");
        energyLabel.setTextFill(Color.web("#d4c4a1"));
        energyLabel.setFont(Font.font("Monospace", 12));
        updateEnergyLabel();

        // periodic refresh
        energyRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(10), e -> updateEnergyLabel()));
        energyRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        energyRefreshTimer.play();

        statusBar.getChildren().addAll(statusLabel, spacer, energyLabel);
        return statusBar;
    }

    private void loadCategories() {
        categoryGrid.getChildren().clear();
        
        var categories = categoryService.getAllCategories();
        if (categories.isEmpty()) {
            Label emptyLabel = new Label("No categories yet. Add one to start building towers!");
            emptyLabel.setTextFill(Color.web("#d4c4a1"));
            emptyLabel.setFont(Font.font("System", FontPosture.ITALIC, 14));
            categoryGrid.getChildren().add(emptyLabel);
        } else {
            for (var category : categories) {
                CategoryCard card = new CategoryCard(category, blockService, this::loadCategories, applicationContext);
                card.setPrefWidth(420);
                categoryGrid.getChildren().add(card);
            }
        }
    }

    private void showAddCategoryDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Start New Tower");
        dialog.setHeaderText("Create a new tower/category");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Tower name (e.g., 'Exercise', 'Reading')");
        
        Spinner<Integer> targetSpinner = new Spinner<>();
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(5, Integer.MAX_VALUE, 10);
        targetSpinner.setValueFactory(valueFactory);
        targetSpinner.setEditable(true);

        CheckBox infiniteBox = new CheckBox("No limit (∞)");
        infiniteBox.selectedProperty().addListener((obs, ov, nv) -> {
            targetSpinner.setDisable(nv);
        });
        
        grid.add(new Label("Tower Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Blocks to Complete:"), 0, 1);
        grid.add(targetSpinner, 1, 1);
        grid.add(infiniteBox, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        String css = getClass().getResource("/css/app.css") != null ? getClass().getResource("/css/app.css").toExternalForm() : null;
        if (css != null) dialog.getDialogPane().getStylesheets().add(css);
        
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK && !nameField.getText().isBlank()) {
                try {
                    int target = infiniteBox.isSelected() ? Integer.MAX_VALUE : targetSpinner.getValue();
                    categoryService.createCategory(nameField.getText(), target);
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
            "-fx-text-fill: #f4e4c1; " +
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
        String css = getClass().getResource("/css/app.css") != null ? getClass().getResource("/css/app.css").toExternalForm() : null;
        if (css != null) alert.getDialogPane().getStylesheets().add(css);
        alert.showAndWait();
    }

    private void doExport() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export Data");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            Window w = getScene() != null ? getScene().getWindow() : null;
            var file = chooser.showSaveDialog(w);
            if (file != null) {
                applicationContext.getExportImportService().exportAll(file.toPath());
                updateStatus("Exported to: " + file.getAbsolutePath());
            }
        } catch (Exception ex) {
            showError("Export failed: " + ex.getMessage());
        }
    }

    private void doImport() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Import Data");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            Window w = getScene() != null ? getScene().getWindow() : null;
            var file = chooser.showOpenDialog(w);
            if (file != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Remap IDs to avoid collisions?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
                confirm.setHeaderText("Import Options");
                String css = getClass().getResource("/css/app.css") != null ? getClass().getResource("/css/app.css").toExternalForm() : null;
                if (css != null) confirm.getDialogPane().getStylesheets().add(css);
                var res = confirm.showAndWait();
                if (res.isPresent() && res.get() != ButtonType.CANCEL) {
                    boolean remap = res.get() == ButtonType.YES;
                    applicationContext.getExportImportService().importAll(file.toPath(), remap);
                    loadCategories();
                    updateStatus("Imported from: " + file.getAbsolutePath());
                }
            }
        } catch (Exception ex) {
            showError("Import failed: " + ex.getMessage());
        }
    }

    private void doMorningRoutine() {
        try {
            boolean ran = applicationContext.getMorningRoutineCoordinator().runIfDue();
            if (ran) {
                updateStatus("Morning routine completed.");
                updateEnergyLabel();
            } else {
                updateStatus("Morning routine not due.");
            }
        } catch (Exception ex) {
            showError("Morning routine failed: " + ex.getMessage());
        }
    }

    private void updateEnergyLabel() {
        try {
            int level = applicationContext.getEnergyService().getLevel();
            boolean exhausted = applicationContext.getEnergyService().isExhausted();
            energyLabel.setText("Energy: " + level + "%" + (exhausted ? " (exhausted)" : ""));
        } catch (Exception ignored) {
            energyLabel.setText("");
        }
    }
}
