package dev.diar.ui.view;

import dev.diar.app.service.TowerViewService;
import dev.diar.core.model.Category;
import dev.diar.core.model.LogEntry;
import dev.diar.core.model.Tower;
import dev.diar.ui.ApplicationContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class TowerGalleryDialog extends Stage {
    private final ApplicationContext context;
    private final String categoryId;
    private final TowerViewService service;

    private ListView<Tower> towersList;
    private StackPane renderPane;
    private Label infoLabel;
    private Image blockImage; // cached block image
    private Slider zoomSlider;
    private double blockScale = 0.5; // scale factor applied to block image size

    public TowerGalleryDialog(ApplicationContext context, String categoryId) {
        this.context = context;
        this.categoryId = categoryId;
        this.service = context.getTowerViewService();
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Towers");
        setupUI();
    }

    private void setupUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Left: towers list
        towersList = new ListView<>();
        towersList.setPrefWidth(260);
        towersList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Tower item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    int stage = service.stageForTower(item);
                    String label = (item.completedOn() != null ? "Completed" : "Active") +
                            " • " + item.blocksCompleted() + "/" + item.blockTarget() +
                            " • Stage " + stage + "/10" + (item.completedOn() != null ? (" • " + item.completedOn()) : "");
                    setText(label);
                }
            }
        });
        towersList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> renderTower(n));

        // Center: render pane (image or placeholder)
        renderPane = new StackPane();
        renderPane.setPrefSize(520, 480);
        renderPane.setStyle("-fx-background-color: #2e2e2e; -fx-border-color: #555; -fx-border-width: 1;");

        // Bottom: buttons
        Button viewBlocksBtn = new Button("View Blocks");
        viewBlocksBtn.setOnAction(e -> viewBlocks());
        // Zoom control
        Label zoomLbl = new Label("Zoom");
        zoomSlider = new Slider(0.25, 1.5, blockScale);
        zoomSlider.setPrefWidth(180);
        zoomSlider.valueProperty().addListener((obs, ov, nv) -> { blockScale = nv.doubleValue(); rerenderSelected(); });
        infoLabel = new Label("");
        infoLabel.setTextFill(Color.web("#ddd"));
        infoLabel.setFont(Font.font("System", 12));
        HBox bottom = new HBox(10, viewBlocksBtn, new Separator(), zoomLbl, zoomSlider, new Separator(), infoLabel);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(8, 0, 0, 0));

        root.setLeft(towersList);
        root.setCenter(renderPane);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 900, 560);
        setScene(scene);

        // Load data
        List<Tower> towers = service.towersForCategory(categoryId);
        towersList.getItems().setAll(towers);
        if (!towers.isEmpty()) {
            towersList.getSelectionModel().select(0);
        }

        // Set window title category name
        context.getCategoryService().getAllCategories().stream()
            .filter(c -> c.id().equals(categoryId)).findFirst()
            .map(Category::name).ifPresent(n -> setTitle("Towers - " + n));
    }

    private void renderTower(Tower tower) {
        renderPane.getChildren().clear();
        if (tower == null) return;
        int stage = service.stageForTower(tower);
        infoLabel.setText("Tower: " + tower.blocksCompleted() + "/" + tower.blockTarget() + " (Stage " + stage + ")");

        // Procedural rendering: stack blocks bottom-up
        // Load logs for this tower to map block -> log
        List<LogEntry> entries = service.logsForTower(categoryId, tower);

        // Load block image once, from ~/.diar-e/assets/blocks/<categoryId>.png then default.png
        if (blockImage == null) {
            File catBlock = new File(System.getProperty("user.home"), ".diar-e/assets/blocks/" + categoryId + ".png");
            File defBlock = new File(System.getProperty("user.home"), ".diar-e/assets/blocks/default.png");
            File chosen = catBlock.exists() ? catBlock : (defBlock.exists() ? defBlock : null);
            if (chosen != null) {
                blockImage = new Image(chosen.toURI().toString());
            }
        }

        int completed = Math.max(0, Math.min(tower.blocksCompleted(), entries.size()));

        // Layout: container with spacer (pushes content to bottom), block stack, and ground bar
        VBox container = new VBox(4);
        container.setPadding(new Insets(8));
        container.setFillWidth(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox blockStack = new VBox(6);
        blockStack.setAlignment(Pos.BOTTOM_CENTER);

        // Render completed blocks with a pyramid-like layout: wide base narrowing upwards
        int blocksRemaining = completed;
        int cursor = 0; // index into entries

        // choose a base width based on tower size and viewport: 2..6 blocks per row
        int baseWidth = Math.min(6, Math.max(2, (int)Math.ceil(Math.sqrt(Math.max(1, completed)) )));
        int currentWidth = baseWidth;
        int rowsAtThisWidth = 0;
        int rowsPerWidth = 2; // stability: two rows per width before narrowing

        while (blocksRemaining > 0) {
            int rowCount = Math.min(currentWidth, blocksRemaining);
            HBox row = new HBox(6);
            row.setAlignment(Pos.CENTER);

            for (int i = 0; i < rowCount; i++) {
                LogEntry entry = entries.get(cursor);
                int blockIndex = cursor + 1;
                cursor++;
                blocksRemaining--;

                StackPane blockNode = createBlockNode(blockImage);
                Tooltip tip = new Tooltip(entry.createdAt().toLocalDateTime().toString().replace('T',' ') +
                        (entry.note() != null ? ("\n" + entry.note()) : ""));
                Tooltip.install(blockNode, tip);
                blockNode.setOnMouseClicked(e -> showBlockDialog(blockIndex, entry));
                // subtle hover highlight
                blockNode.setOnMouseEntered(e -> blockNode.setStyle("-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.15), 10, 0.2, 0, 0);"));
                blockNode.setOnMouseExited(e -> blockNode.setStyle(""));
                row.getChildren().add(blockNode);
            }

            blockStack.getChildren().add(row);

            rowsAtThisWidth++;
            if (rowsAtThisWidth >= rowsPerWidth && currentWidth > 1) {
                currentWidth--;
                rowsAtThisWidth = 0;
            }
        }

        // Ground bar sized relative to current block width
        double baseW = (blockImage != null ? blockImage.getWidth() : 256);
        double w = Math.max(32, baseW * blockScale);
        Rectangle ground = new Rectangle(w + 20, 12, Color.web("#444"));
        ground.setStroke(Color.web("#222"));

        HBox groundBox = new HBox(ground);
        groundBox.setAlignment(Pos.CENTER);

        container.getChildren().addAll(spacer, blockStack, groundBox);

        ScrollPane sp = new ScrollPane(container);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(440);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        renderPane.getChildren().add(sp);
        // Scroll to bottom so the base and latest blocks are visible
        sp.layout();
        sp.setVvalue(1.0);
    }

    private void viewBlocks() {
        Tower sel = towersList.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        List<LogEntry> entries = service.logsForTower(categoryId, sel);
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Tower Blocks");
        dlg.setHeaderText(null);
        ListView<LogEntry> lv = new ListView<>();
        lv.setCellFactory(l -> new ListCell<>(){
            @Override
            protected void updateItem(LogEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String ts = item.createdAt().toLocalDateTime().toString().replace('T',' ');
                    setText(ts + (item.note() != null ? (" — " + item.note()) : ""));
                }
            }
        });
        lv.getItems().setAll(entries);
        dlg.getDialogPane().setContent(new VBox(10, new Label("Blocks in selected tower:"), lv));
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }

    private StackPane createBlockNode(Image img) {
        // Default logical size if no image; otherwise use image dimensions
        double baseW = (img != null ? img.getWidth() : 256);
        double baseH = (img != null ? img.getHeight() : 256);
        double w = Math.max(32, baseW * blockScale);
        double h = Math.max(16, baseH * blockScale);
        StackPane pane = new StackPane();
        pane.setAlignment(Pos.CENTER);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setPreserveRatio(false);
            iv.setFitWidth(w);
            iv.setFitHeight(h);
            pane.getChildren().add(iv);
        } else {
            Rectangle r = new Rectangle(w, h, Color.web("#7a9b8e"));
            r.setStroke(Color.web("#2e2e2e"));
            pane.getChildren().add(r);
        }
        return pane;
    }

    private void showBlockDialog(int index, LogEntry entry) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Block #" + index);
        dlg.setHeaderText(entry.createdAt().toLocalDateTime().toString().replace('T',' '));
        String note = entry.note() != null ? entry.note() : "(No note)";
        TextArea ta = new TextArea(note);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(6);
        dlg.getDialogPane().setContent(new VBox(8, new Label("Note:"), ta));
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }

    private void rerenderSelected() {
        Tower sel = towersList.getSelectionModel().getSelectedItem();
        if (sel != null) {
            renderTower(sel);
        }
    }
}
