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
import java.time.format.DateTimeFormatter;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;

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
    private Timeline buildTimeline;

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
        // enforce dark-brown background regardless of platform defaults
        root.setStyle("-fx-background-color: #3a2f27;");

        // Left: towers list
        towersList = new ListView<>();
        towersList.setStyle("-fx-background-insets: 0; -fx-background-color: #3a2f27; -fx-control-inner-background: #3a2f27; -fx-text-fill: #f4e4c1;");
        towersList.setPrefWidth(260);
        towersList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Tower item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    int stage = service.stageForTower(item);
                    String tgt = (item.blockTarget() == Integer.MAX_VALUE) ? "∞" : String.valueOf(item.blockTarget());
                    String label = (item.completedOn() != null ? "Completed" : "Active") +
                            " • " + item.blocksCompleted() + "/" + tgt +
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
        stageBadge.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-text-fill: #f4e4c1; -fx-padding: 4 8; -fx-background-radius: 6;");
        StackPane.setAlignment(stageBadge, Pos.TOP_RIGHT);
        renderPane.getChildren().add(stageBadge);

        // Center Right: block list panel
        rightPane = new VBox(8);
        rightPane.setPadding(new Insets(8));
        rightPane.setStyle("-fx-background-color: #3a2f27;");
        Label blocksHdr = new Label("Blocks");
        blocksHdr.setFont(Font.font("System", 14));
        blocksHdr.setTextFill(Color.web("#d4c4a1"));
        blockFilterField = new TextField();
        blockFilterField.setStyle("-fx-background-color: #5a4a3a; -fx-control-inner-background: #2e2e2e; -fx-text-fill: #f4e4c1;");
        blockFilterField.setPromptText("Filter blocks...");
        blockFilterField.textProperty().addListener((obs, ov, nv) -> applyBlockFilter());
        blockListView = new ListView<>();
        blockListView.setStyle("-fx-background-insets: 0; -fx-background-color: #3a2f27; -fx-control-inner-background: #3a2f27; -fx-text-fill: #f4e4c1;");
        blockListView.setPrefWidth(300);
        blockListView.setCellFactory(lv -> new ListCell<>(){
            @Override
            protected void updateItem(BlockItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String note = item.entry.note() != null ? item.entry.note() : "";
                    String title = "";
                    if (!note.isEmpty()) {
                        String[] lines = note.split("\\R");
                        for (String ln : lines) {
                            String t = ln.trim();
                            if (!t.isEmpty()) { title = t; break; }
                        }
                    }
                    if (title.isEmpty()) title = "(untitled)";
                    String ts = item.entry.createdAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    setText(item.index + ".  " + title + " — " + ts);
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
        centerBox.setStyle("-fx-background-color: #3a2f27;");

        // Bottom: zoom and info
        Label zoomLbl = new Label("Zoom");
        zoomLbl.setTextFill(Color.web("#d4c4a1"));
        zoomSlider = new Slider(0.05, 1.5, blockScale);
        zoomSlider.getStyleClass().add("walle-slider");
        zoomSlider.setPrefWidth(180);
        zoomSlider.valueProperty().addListener((obs, ov, nv) -> { blockScale = nv.doubleValue(); rerenderSelected(); });
        // Force style of platform slider subnodes
        zoomSlider.skinProperty().addListener((o, ov, nv) -> Platform.runLater(this::styleZoomSlider));
        zoomSlider.sceneProperty().addListener((o, ov, nv) -> Platform.runLater(this::styleZoomSlider));
        Platform.runLater(this::styleZoomSlider);
        infoLabel = new Label("");
        infoLabel.setTextFill(Color.web("#d4c4a1"));
        infoLabel.setFont(Font.font("System", 12));
        HBox bottom = new HBox(10, zoomLbl, zoomSlider, new Separator(), infoLabel);
        bottom.setStyle("-fx-background-color: #3a2f27;");
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(8, 0, 0, 0));

        root.setLeft(towersList);
        root.setCenter(centerBox);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 900, 560);
        String css = getClass().getResource("/css/app.css") != null ? getClass().getResource("/css/app.css").toExternalForm() : null;
        if (css != null) {
            scene.getStylesheets().add(css);
        }
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
        if (buildTimeline != null) { buildTimeline.stop(); buildTimeline = null; }
        int stage = service.stageForTower(tower);
        if (tower.blockTarget() == Integer.MAX_VALUE) {
            infoLabel.setText("Tower: " + tower.blocksCompleted() + " blocks (∞)");
            stageBadge.setText("∞");
        } else {
            infoLabel.setText("Tower: " + tower.blocksCompleted() + "/" + tower.blockTarget() + " (Stage " + stage + ")");
            stageBadge.setText("Stage " + stage + "/10");
        }

        // Procedural rendering: stack blocks bottom-up
        // Load logs for this tower to map block -> log
        List<LogEntry> entries = service.logsForTower(categoryId, tower);
        currentBlocks.clear();
        blockNodes.clear();

        // Load block image once: prefer classpath resource /images/block.png, fallback to ~/.diar-e assets
        if (blockImage == null) {
            try {
                var res = getClass().getResource("/images/block.png");
                if (res != null) {
                    blockImage = new Image(res.toExternalForm());
                }
            } catch (Exception ignored) {}
            if (blockImage == null) {
                File catBlock = new File(System.getProperty("user.home"), ".diar-e/assets/blocks/" + categoryId + ".png");
                File defBlock = new File(System.getProperty("user.home"), ".diar-e/assets/blocks/default.png");
                File chosen = catBlock.exists() ? catBlock : (defBlock.exists() ? defBlock : null);
                if (chosen != null) {
                    blockImage = new Image(chosen.toURI().toString());
                }
            }
        }

        int completed = Math.max(0, Math.min(tower.blocksCompleted(), entries.size()));

        // Layout: container with spacer (pushes content to bottom), block stack, and ground bar
        VBox container = new VBox(4);
        container.setPadding(new Insets(8));
        container.setFillWidth(true);
        container.setStyle("-fx-background-color: #2e2e2e;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox blockStack = new VBox(6);
        blockStack.setAlignment(Pos.BOTTOM_CENTER);

        // Render completed blocks with a pyramid-like layout: wide base narrowing upwards
        int blocksRemaining = completed;
        int cursor = 0; // index into entries

        // Procedural layout that evolves with tower size (Wall-E style):
        // small towers (under 10 blocks) are spire-like; medium towers stabilize; large towers ziggurat.
        int baseWidth = Math.min(8, Math.max(2, (int)Math.ceil(Math.sqrt(Math.max(1, completed)) )));
        double wref = Math.max(32, (blockImage != null ? blockImage.getWidth() : 256) * blockScale);
        int rowsPerWidth = (completed < 10) ? 1 : (completed < 40 ? 2 : 3);
        double jitterAmp = wref * (completed < 10 ? 0.08 : 0.04);
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

            blockStack.getChildren().add(0, row);

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
        playBuildAnimation();
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
        String css = getClass().getResource("/css/app.css") != null ? getClass().getResource("/css/app.css").toExternalForm() : null;
        if (css != null) dlg.getDialogPane().getStylesheets().add(css);
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
        pane.setOpacity(0.0);
        pane.setScaleY(0.3);
        pane.setTranslateY(10);
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
        String css = getClass().getResource("/css/app.css") != null ? getClass().getResource("/css/app.css").toExternalForm() : null;
        if (css != null) dlg.getDialogPane().getStylesheets().add(css);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }

    private void rerenderSelected() {
        Tower sel = towersList.getSelectionModel().getSelectedItem();
        if (sel != null) {
            renderTower(sel);
        }
    }

    private void styleZoomSlider() {
        try {
            if (zoomSlider == null) return;
            var track = zoomSlider.lookup(".track");
            if (track instanceof Region r) {
                r.setStyle("-fx-background-color: #42372f; -fx-background-insets: 0; -fx-background-radius: 8; -fx-background-image: null;");
            }
            var thumb = zoomSlider.lookup(".thumb");
            if (thumb instanceof Region r2) {
                r2.setStyle("-fx-background-color: #FFC107; -fx-background-insets: 0; -fx-background-radius: 10; -fx-background-image: null; -fx-border-color: transparent;");
            }
        } catch (Exception ignored) {
        }
    }

    private void playBuildAnimation() {
        if (buildTimeline != null) buildTimeline.stop();
        buildTimeline = new Timeline();
        double d = 28.0;
        // Fast path: exactly one block — show it immediately to avoid any timing quirks
        if (blockNodes.size() == 1) {
            StackPane n = blockNodes.get(0);
            n.setOpacity(1.0);
            n.setScaleY(1.0);
            n.setTranslateY(0.0);
            if (currentScroll != null) currentScroll.setVvalue(1.0);
            return;
        }
        // Ensure a non-zero duration so that even a single block animates in
        for (int i = 0; i < blockNodes.size(); i++) {
            StackPane n = blockNodes.get(i);
            double t = (i + 1) * d; // shift by one frame to avoid 0ms-only timelines
            KeyFrame kf = new KeyFrame(Duration.millis(t),
                new KeyValue(n.opacityProperty(), 1.0),
                new KeyValue(n.scaleYProperty(), 1.0),
                new KeyValue(n.translateYProperty(), 0.0)
            );
            buildTimeline.getKeyFrames().add(kf);
        }
        if (currentScroll != null) {
            buildTimeline.currentTimeProperty().addListener((o, ov, nv) -> currentScroll.setVvalue(1.0));
        }
        buildTimeline.playFromStart();
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
