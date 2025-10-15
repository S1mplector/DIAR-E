package dev.diar.ui.view;

import dev.diar.app.service.TowerViewService;
import dev.diar.core.model.Category;
import dev.diar.core.model.LogEntry;
import dev.diar.core.model.Tower;
import dev.diar.ui.ApplicationContext;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Bounds;
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
import java.util.ArrayList;
import java.util.Locale;

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
    private VBox rightPane;
    private TextField blockFilterField;
    private ListView<BlockItem> blockListView;
    private List<BlockItem> currentBlocks = new ArrayList<>();
    private List<StackPane> blockNodes = new ArrayList<>();
    private ScrollPane currentScroll;
    private int highlightedIndex = -1;
    private Label stageBadge;
    private ComboBox<String> styleBox;

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

        // Center Left: render pane (image or placeholder)
        renderPane = new StackPane();
        renderPane.setPrefSize(520, 480);
        renderPane.setStyle("-fx-background-color: #2e2e2e; -fx-border-color: #555; -fx-border-width: 1;");
        stageBadge = new Label("");
        stageBadge.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 6;");
        StackPane.setAlignment(stageBadge, Pos.TOP_RIGHT);
        renderPane.getChildren().add(stageBadge);

        // Center Right: block list panel
        rightPane = new VBox(8);
        rightPane.setPadding(new Insets(8));
        Label blocksHdr = new Label("Blocks");
        blocksHdr.setFont(Font.font("System", 14));
        blockFilterField = new TextField();
        blockFilterField.setPromptText("Filter blocks...");
        blockFilterField.textProperty().addListener((obs, ov, nv) -> applyBlockFilter());
        blockListView = new ListView<>();
        blockListView.setPrefWidth(300);
        blockListView.setCellFactory(lv -> new ListCell<>(){
            @Override
            protected void updateItem(BlockItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String ts = item.entry.createdAt().toLocalDateTime().toString().replace('T',' ');
                    String note = item.entry.note() != null ? item.entry.note() : "";
                    setText(item.index + ".  " + ts + (note.isEmpty()? "" : (" — " + note)));
                }
            }
        });
        blockListView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) highlightAndScroll(n.index);
        });
        rightPane.getChildren().addAll(blocksHdr, blockFilterField, blockListView);
        rightPane.setPrefWidth(320);

        // Join center as HBox: render left + separator + right list
        HBox centerBox = new HBox(10, renderPane, new Separator(Orientation.VERTICAL), rightPane);

        // Bottom: buttons
        Button viewBlocksBtn = new Button("View Blocks");
        viewBlocksBtn.setOnAction(e -> viewBlocks());
        // Zoom control
        Label zoomLbl = new Label("Zoom");
        zoomSlider = new Slider(0.25, 1.5, blockScale);
        zoomSlider.setPrefWidth(180);
        zoomSlider.valueProperty().addListener((obs, ov, nv) -> { blockScale = nv.doubleValue(); rerenderSelected(); });
        // Style selector
        Label styleLbl = new Label("Style");
        styleBox = new ComboBox<>();
        styleBox.getItems().addAll("Pyramid", "Ziggurat", "Spire");
        styleBox.getSelectionModel().select(0);
        styleBox.valueProperty().addListener((obs, ov, nv) -> rerenderSelected());
        infoLabel = new Label("");
        infoLabel.setTextFill(Color.web("#ddd"));
        infoLabel.setFont(Font.font("System", 12));
        HBox bottom = new HBox(10, viewBlocksBtn, new Separator(), zoomLbl, zoomSlider, new Separator(), styleLbl, styleBox, new Separator(), infoLabel);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(8, 0, 0, 0));

        root.setLeft(towersList);
        root.setCenter(centerBox);
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
        renderPane.getChildren().add(stageBadge);
        if (tower == null) {
            blockListView.getItems().clear();
            return;
        }
        int stage = service.stageForTower(tower);
        infoLabel.setText("Tower: " + tower.blocksCompleted() + "/" + tower.blockTarget() + " (Stage " + stage + ")");
        stageBadge.setText("Stage " + stage + "/10");

        // Procedural rendering: stack blocks bottom-up
        // Load logs for this tower to map block -> log
        List<LogEntry> entries = service.logsForTower(categoryId, tower);
        currentBlocks.clear();
        blockNodes.clear();

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

        // choose layout parameters based on style
        String style = styleBox != null && styleBox.getValue() != null ? styleBox.getValue() : "Pyramid";
        int baseWidth = Math.min(8, Math.max(2, (int)Math.ceil(Math.sqrt(Math.max(1, completed)) )));
        int rowsPerWidth;
        double jitterAmp;
        switch (style) {
            case "Ziggurat" -> { rowsPerWidth = 3; jitterAmp = 0; }
            case "Spire" -> { rowsPerWidth = 1; jitterAmp = (Math.max(32, (blockImage != null ? blockImage.getWidth() : 256) * blockScale)) * 0.08; }
            default -> { rowsPerWidth = 2; jitterAmp = (Math.max(32, (blockImage != null ? blockImage.getWidth() : 256) * blockScale)) * 0.04; }
        }
        int currentWidth = baseWidth;
        int rowsAtThisWidth = 0;
        int rowNo = 0;

        while (blocksRemaining > 0) {
            int rowCount = Math.min(currentWidth, blocksRemaining);
            HBox row = new HBox(6);
            row.setAlignment(Pos.CENTER);
            // lateral jitter per row for more organic feel
            double offset = jitterAmp * Math.sin(rowNo * 0.9);
            row.setTranslateX(offset);

            for (int i = 0; i < rowCount; i++) {
                LogEntry entry = entries.get(cursor);
                int blockIndex = cursor + 1;
                cursor++;
                blocksRemaining--;

                StackPane blockNode = createBlockNode(blockImage);
                Tooltip tip = new Tooltip(entry.createdAt().toLocalDateTime().toString().replace('T',' ') +
                        (entry.note() != null ? ("\n" + entry.note()) : ""));
                Tooltip.install(blockNode, tip);
                final int idxRef = blockIndex;
                blockNode.setOnMouseClicked(e -> {
                    showBlockDialog(idxRef, entry);
                    selectInBlockList(idxRef);
                });
                // subtle hover highlight
                blockNode.setOnMouseEntered(e -> {
                    blockNode.setStyle("-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.18), 12, 0.25, 0, 0);");
                    selectInBlockList(idxRef);
                });
                blockNode.setOnMouseExited(e -> blockNode.setStyle(""));
                row.getChildren().add(blockNode);
                blockNodes.add(blockNode);
                currentBlocks.add(new BlockItem(blockIndex, entry));
            }

            blockStack.getChildren().add(row);

            rowsAtThisWidth++;
            if (rowsAtThisWidth >= rowsPerWidth && currentWidth > 1) {
                currentWidth--;
                rowsAtThisWidth = 0;
            }
            rowNo++;
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
        currentScroll = sp;

        // Populate right list
        applyBlockFilter();
        highlightedIndex = -1;
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

    private void applyBlockFilter() {
        String q = blockFilterField.getText() != null ? blockFilterField.getText().trim().toLowerCase(Locale.ROOT) : "";
        blockListView.getItems().clear();
        if (q.isEmpty()) {
            blockListView.getItems().addAll(currentBlocks);
        } else {
            for (BlockItem bi : currentBlocks) {
                String text = (bi.entry.note() != null ? bi.entry.note() : "");
                if (text.toLowerCase(Locale.ROOT).contains(q)) {
                    blockListView.getItems().add(bi);
                }
            }
        }
    }

    private void selectInBlockList(int index) {
        for (BlockItem bi : blockListView.getItems()) {
            if (bi.index == index) {
                blockListView.getSelectionModel().select(bi);
                break;
            }
        }
    }

    private void highlightAndScroll(int index) {
        // clear previous
        if (highlightedIndex >= 0 && highlightedIndex - 1 < blockNodes.size()) {
            blockNodes.get(highlightedIndex - 1).setStyle("");
        }
        highlightedIndex = index;
        if (index - 1 >= 0 && index - 1 < blockNodes.size()) {
            StackPane node = blockNodes.get(index - 1);
            node.setStyle("-fx-border-color: #f4e4c1; -fx-border-width: 2;");
            if (currentScroll != null) {
                double contentHeight = currentScroll.getContent().getBoundsInLocal().getHeight();
                Bounds nb = node.getBoundsInParent();
                double y = nb.getMinY();
                double vh = currentScroll.getViewportBounds().getHeight();
                double targetV = Math.max(0, Math.min(1, (y - vh * 0.3) / Math.max(1, contentHeight - vh)));
                currentScroll.setVvalue(targetV);
            }
        }
    }

    private static class BlockItem {
        final int index;
        final LogEntry entry;
        BlockItem(int index, LogEntry entry) { this.index = index; this.entry = entry; }
        @Override public String toString() { return index + ""; }
    }
}
