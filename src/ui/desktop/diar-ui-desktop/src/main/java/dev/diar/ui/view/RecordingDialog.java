package dev.diar.ui.view;

import dev.diar.app.service.RecordingService;
import dev.diar.app.port.AudioCapturePort.AudioDevice;
import dev.diar.core.model.Recording;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import javax.sound.sampled.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Comparator;

public class RecordingDialog extends Dialog<ButtonType> {
    private final RecordingService recordingService;
    private String currentRecordingId;
    private Button recordButton;
    private ProgressBar levelMeter;
    private Label statusLabel;
    private Label timerLabel;
    private Timeline timer;
    private int elapsedSeconds = 0;
    private ListView<Recording> recordingsList;
    private Button playButton;
    private Button pauseButton;
    private Clip currentClip;
    private Slider volumeSlider;
    private Canvas waveformCanvas;
    private Timeline vizTimer;
    private double[] peaks; // precomputed per-pixel peaks [-1..1]
    private long clipLengthUs;
    private int sampleRate = 16000;
    private boolean paused = false;
    private Image imgPlay, imgPause, imgRecord;
    private String playingRecordingId;
    private String cssUrl;
    private Canvas spectrumCanvas;
    private volatile float[] spectrumBins;
    private Slider micGainSlider;
    private Label micGainValue;
    private Label micHealthLabel;
    private ComboBox<AudioDevice> deviceCombo;
    private Timeline deviceRefreshTimer;
    private ObservableList<Recording> recordingsMaster = FXCollections.observableArrayList();
    private FilteredList<Recording> recordingsFiltered;
    private SortedList<Recording> recordingsSorted;
    private TextField searchField;
    private ComboBox<String> sortCombo;

    public RecordingDialog(RecordingService recordingService) {
        this.recordingService = recordingService;
        setupUI();
    }

    private void setupUI() {
        setTitle("Audio Diary");
        setHeaderText("Record your thoughts and reflections");
        cssUrl = getClass().getResource("/css/app.css") != null ? getClass().getResource("/css/app.css").toExternalForm() : null;
        
        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        content.setPrefWidth(360);
        content.setStyle("-fx-background-color: #3a2f27;");
        content.setMaxWidth(360);
        
        statusLabel = new Label("Ready to record");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        statusLabel.setTextFill(Color.web("#f4e4c1"));
        
        timerLabel = new Label("00:00");
        timerLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 24));
        timerLabel.setTextFill(Color.web("#f4e4c1"));
        
        levelMeter = new ProgressBar(0);
        levelMeter.setPrefWidth(300);
        levelMeter.setStyle("-fx-accent: #7a9b8e;");
        // Hide the idle sensitivity bar entirely
        levelMeter.setVisible(false);
        levelMeter.setManaged(false);

        // Live spectrum (shown during recording)
        spectrumCanvas = new Canvas(360, 120);
        spectrumCanvas.setVisible(false);
        spectrumCanvas.setManaged(false);
        micHealthLabel = new Label("");
        micHealthLabel.setTextFill(Color.web("#d4c4a1"));
        micHealthLabel.setVisible(false);
        micHealthLabel.setManaged(false);
        
        // Load icons
        imgRecord = loadIcon("record.png");
        imgPlay = loadIcon("play.png");
        imgPause = loadIcon("pause.png");

        // Mic input gain (sensitivity) slider UI
        Label micGainLabel = new Label("Mic Boost");
        micGainLabel.setTextFill(Color.web("#f4e4c1"));
        double initialGain;
        try { initialGain = recordingService.getInputGain(); } catch (Exception ex) { initialGain = 1.0; }
        micGainSlider = new Slider(0.5, 8.0, initialGain);
        micGainSlider.getStyleClass().add("walle-slider");
        micGainSlider.setPrefWidth(220);
        micGainSlider.setBlockIncrement(0.1);
        micGainValue = new Label(formatGain(initialGain));
        micGainValue.setTextFill(Color.web("#d4c4a1"));
        micGainSlider.valueProperty().addListener((o, ov, nv) -> {
            double g = nv.doubleValue();
            recordingService.setInputGain(g);
            micGainValue.setText(formatGain(g));
        });
        HBox micGainBox = new HBox(10, micGainLabel, micGainSlider, micGainValue);
        micGainBox.setAlignment(Pos.CENTER);
        micGainBox.setStyle("-fx-background-color: #3a2f27;");
        micGainBox.setMaxWidth(360);

        // Input device selector
        Label devLabel = new Label("Input Device");
        devLabel.setTextFill(Color.web("#f4e4c1"));
        deviceCombo = new ComboBox<>();
        deviceCombo.setPrefWidth(220);
        deviceCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(AudioDevice it, boolean empty) {
                super.updateItem(it, empty);
                setText(empty || it == null ? null : it.name());
            }
        });
        deviceCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(AudioDevice it, boolean empty) {
                super.updateItem(it, empty);
                setText(empty || it == null ? null : it.name());
            }
        });
        try {
            java.util.List<AudioDevice> devices = recordingService.listInputDevices();
            deviceCombo.getItems().setAll(devices);
            String cur = recordingService.getInputDevice();
            AudioDevice sel = devices.stream().filter(d -> d.id().equals(cur)).findFirst().orElse(devices.isEmpty()?null:devices.get(0));
            if (sel != null) deviceCombo.getSelectionModel().select(sel);
        } catch (Exception ignored) { }
        deviceCombo.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                try { recordingService.setInputDevice(nv.id()); } catch (Exception ignored) { }
            }
        });
        Button devRefreshBtn = new Button("\u21BB"); // ↻
        devRefreshBtn.setBackground(Background.EMPTY);
        devRefreshBtn.setStyle("-fx-background-color: #3a2f27; -fx-text-fill: #f4e4c1; -fx-border-color: #2a1f17; -fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6;");
        devRefreshBtn.setTooltip(new Tooltip("Refresh devices"));
        devRefreshBtn.setOnAction(ev -> refreshDeviceList(true));
        HBox deviceBox = new HBox(10, devLabel, deviceCombo, devRefreshBtn);
        deviceBox.setAlignment(Pos.CENTER);
        deviceBox.setStyle("-fx-background-color: #3a2f27;");
        deviceBox.setMaxWidth(360);

        recordButton = new Button(imgRecord != null ? "" : "Record");
        if (imgRecord != null) recordButton.setGraphic(iconView(imgRecord, 36));
        recordButton.setTooltip(new Tooltip("Start Recording"));
        recordButton.setBackground(Background.EMPTY);
        recordButton.setBorder(Border.EMPTY);
        recordButton.setStyle("-fx-background-color: transparent; -fx-padding: 0;" );
        recordButton.getStyleClass().add("cassette-btn");
        installPressAnimation(recordButton);
        recordButton.setOnAction(e -> toggleRecording());

        // Recordings section
        Label recordingsLabel = new Label("Recordings");
        recordingsLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        recordingsLabel.setTextFill(Color.web("#f4e4c1"));

        // Search + Sort bar
        searchField = new TextField();
        searchField.setPromptText("Search by name or date...");
        searchField.setPrefWidth(180);
        sortCombo = new ComboBox<>();
        sortCombo.getItems().setAll("Newest first", "Oldest first", "Name A-Z", "Name Z-A", "Duration longest", "Duration shortest");
        sortCombo.getSelectionModel().select("Newest first");
        sortCombo.setPrefWidth(160);
        HBox searchSortBar = new HBox(10, searchField, sortCombo);
        searchSortBar.setAlignment(Pos.CENTER);
        searchSortBar.setStyle("-fx-background-color: #3a2f27;");
        searchSortBar.setMaxWidth(360);

        recordingsList = new ListView<>();
        recordingsList.setStyle("-fx-background-insets: 0; -fx-background-color: #3a2f27; -fx-control-inner-background: #3a2f27; -fx-text-fill: #f4e4c1; " +
                "-fx-selection-bar: #FFC107; -fx-selection-bar-non-focused: #E0B000; -fx-selection-bar-text: #3a2f27;");
        recordingsList.setPrefHeight(180);
        recordingsList.setPrefWidth(360);
        recordingsList.setMaxWidth(360);
        // Wire filtered/sorted pipeline
        recordingsFiltered = new FilteredList<>(recordingsMaster, r -> true);
        recordingsSorted = new SortedList<>(recordingsFiltered);
        recordingsList.setItems(recordingsSorted);
        // Search filter behavior
        searchField.textProperty().addListener((o, ov, nv) -> {
            String q = nv == null ? "" : nv.trim().toLowerCase();
            recordingsFiltered.setPredicate(rec -> {
                if (q.isEmpty()) return true;
                String name = new java.io.File(rec.filePath()).getName().toLowerCase();
                String date = rec.createdAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).toLowerCase();
                return name.contains(q) || date.contains(q);
            });
        });
        // Sort behavior
        sortCombo.valueProperty().addListener((o, ov, nv) -> recordingsSorted.setComparator(comparatorFor(nv)));
        recordingsSorted.setComparator(comparatorFor(sortCombo.getValue()));
        recordingsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Recording item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTextFill(Color.web("#f4e4c1"));
                    setContextMenu(null);
                } else {
                    String ts = item.createdAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    setText(ts + "  •  " + new File(item.filePath()).getName());
                    // Ensure readable text color on selection highlight
                    setTextFill(isSelected() ? Color.web("#3a2f27") : Color.web("#f4e4c1"));
                    selectedProperty().addListener((o, ov, nv) -> setTextFill(nv ? Color.web("#3a2f27") : Color.web("#f4e4c1")));
                    // Context menu for rename/delete
                    MenuItem rename = new MenuItem("Rename...");
                    rename.setOnAction(e -> {
                        if (playingRecordingId != null && playingRecordingId.equals(item.id())) {
                            showError("Stop playback before renaming this recording.");
                            return;
                        }
                        String currentName = new File(item.filePath()).getName();
                        String base = currentName.toLowerCase().endsWith(".wav") ? currentName.substring(0, currentName.length()-4) : currentName;
                        TextInputDialog td = new TextInputDialog(base);
                        td.setTitle("Rename Recording");
                        td.setHeaderText("Enter a new name for the recording (will save as .wav)");
                        if (cssUrl != null) td.getDialogPane().getStylesheets().add(cssUrl);
                        td.getDialogPane().setStyle("-fx-background-color: #3a2f27; -fx-base: #3a2f27; -fx-control-inner-background: #2e2e2e; -fx-text-background-color: #d4c4a1; -fx-focus-color: #FFC107; -fx-faint-focus-color: rgba(255,193,7,0.20);");
                        Button okR = (Button) td.getDialogPane().lookupButton(ButtonType.OK);
                        if (okR != null) okR.setStyle("-fx-background-color: #3a2f27; -fx-text-fill: #f4e4c1; -fx-font-weight: bold; -fx-border-color: #2a1f17; -fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6;");
                        Button cancelR = (Button) td.getDialogPane().lookupButton(ButtonType.CANCEL);
                        if (cancelR != null) cancelR.setStyle("-fx-background-color: #3a2f27; -fx-text-fill: #f4e4c1; -fx-font-weight: bold; -fx-border-color: #2a1f17; -fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6;");
                        td.setOnShown(evx -> {
                            javafx.scene.Node header = td.getDialogPane().lookup(".header-panel");
                            if (header != null) header.setStyle("-fx-background-color: #5a4a3a;");
                            javafx.scene.Node contentReg = td.getDialogPane().lookup(".content");
                            if (contentReg != null) contentReg.setStyle("-fx-background-color: #3a2f27;");
                            javafx.scene.Node buttonBar = td.getDialogPane().lookup(".button-bar");
                            if (buttonBar != null) buttonBar.setStyle("-fx-background-color: #5a4a3a;");
                        });
                        td.showAndWait().ifPresent(newName -> {
                            if (newName != null && !newName.trim().isBlank()) {
                                try {
                                    recordingService.renameRecording(item.id(), newName.trim());
                                    loadRecordings();
                                } catch (Exception ex) {
                                    showError("Rename failed: " + ex.getMessage());
                                }
                            }
                        });
                    });
                    MenuItem del = new MenuItem("Delete...");
                    del.setOnAction(e -> {
                        if (playingRecordingId != null && playingRecordingId.equals(item.id())) {
                            stopPlayback();
                        }
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this recording?", ButtonType.OK, ButtonType.CANCEL);
                        confirm.setHeaderText("Confirm Delete");
                        if (cssUrl != null) confirm.getDialogPane().getStylesheets().add(cssUrl);
                        confirm.getDialogPane().setStyle("-fx-background-color: #3a2f27; -fx-base: #3a2f27; -fx-control-inner-background: #2e2e2e; -fx-text-background-color: #d4c4a1; -fx-focus-color: #FFC107; -fx-faint-focus-color: rgba(255,193,7,0.20);");
                        Button okD = (Button) confirm.getDialogPane().lookupButton(ButtonType.OK);
                        if (okD != null) okD.setStyle("-fx-background-color: #3a2f27; -fx-text-fill: #f4e4c1; -fx-font-weight: bold; -fx-border-color: #2a1f17; -fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6;");
                        Button cancelD = (Button) confirm.getDialogPane().lookupButton(ButtonType.CANCEL);
                        if (cancelD != null) cancelD.setStyle("-fx-background-color: #3a2f27; -fx-text-fill: #f4e4c1; -fx-font-weight: bold; -fx-border-color: #2a1f17; -fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6;");
                        confirm.setOnShown(evy -> {
                            javafx.scene.Node header = confirm.getDialogPane().lookup(".header-panel");
                            if (header != null) header.setStyle("-fx-background-color: #5a4a3a;");
                            javafx.scene.Node contentReg = confirm.getDialogPane().lookup(".content");
                            if (contentReg != null) contentReg.setStyle("-fx-background-color: #3a2f27;");
                            javafx.scene.Node buttonBar = confirm.getDialogPane().lookup(".button-bar");
                            if (buttonBar != null) buttonBar.setStyle("-fx-background-color: #5a4a3a;");
                        });
                        confirm.showAndWait().ifPresent(btn -> {
                            if (btn == ButtonType.OK) {
                                try {
                                    recordingService.deleteRecording(item.id());
                                    loadRecordings();
                                } catch (Exception ex) {
                                    showError("Delete failed: " + ex.getMessage());
                                }
                            }
                        });
                    });
                    ContextMenu cm = new ContextMenu(rename, del);
                    setContextMenu(cm);
                }
            }
        });

        playButton = new Button(imgPlay != null ? "" : "Play");
        if (imgPlay != null) playButton.setGraphic(iconView(imgPlay, 36));
        playButton.setTooltip(new Tooltip("Play selected"));
        playButton.setBackground(Background.EMPTY);
        playButton.setBorder(Border.EMPTY);
        playButton.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        playButton.getStyleClass().add("cassette-btn");
        installPressAnimation(playButton);
        playButton.setOnAction(e -> {
            Recording sel = recordingsList.getSelectionModel().getSelectedItem();
            if (paused && currentClip != null) {
                try {
                    currentClip.start();
                    paused = false;
                    updatePauseButtonIcon();
                    startVisualizer();
                } catch (Exception ignored) { }
            } else if (sel != null) {
                playRecording(sel);
            }
        });

        pauseButton = new Button();
        updatePauseButtonIcon();
        pauseButton.setBackground(Background.EMPTY);
        pauseButton.setBorder(Border.EMPTY);
        pauseButton.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        pauseButton.getStyleClass().add("cassette-btn");
        installPressAnimation(pauseButton);
        pauseButton.setOnAction(e -> togglePause());

        // Volume control
        Label volLabel = new Label("Volume");
        volLabel.setTextFill(Color.web("#f4e4c1"));
        volumeSlider = new Slider(0.0, 1.0, 0.8);
        volumeSlider.setBlockIncrement(0.05);
        volumeSlider.setPrefWidth(200);
        volumeSlider.valueProperty().addListener((obs, ov, nv) -> applyVolume());

        ToolBar playbackBar = new ToolBar(playButton, pauseButton, new Separator(), volLabel, volumeSlider);
        playbackBar.setStyle("-fx-background-color: #3a2f27;");
        playbackBar.setPrefWidth(360);
        playbackBar.setMaxWidth(360);

        // Waveform canvas
        waveformCanvas = new Canvas(360, 100);
        waveformCanvas.widthProperty().addListener((o, ov, nv) -> renderVisualizer());
        waveformCanvas.heightProperty().addListener((o, ov, nv) -> renderVisualizer());

        loadRecordings();
        
        content.getChildren().addAll(statusLabel, timerLabel, /*levelMeter,*/ spectrumCanvas, micHealthLabel, deviceBox, recordButton, micGainBox,
            recordingsLabel, searchSortBar, recordingsList, playbackBar, waveformCanvas);
        
        getDialogPane().setContent(content);
        if (cssUrl != null) getDialogPane().getStylesheets().add(cssUrl);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        // Dialog theming and buttons
        getDialogPane().setStyle("-fx-background-color: #3a2f27; -fx-base: #3a2f27; -fx-control-inner-background: #2e2e2e; -fx-text-background-color: #d4c4a1; -fx-focus-color: #FFC107; -fx-faint-focus-color: rgba(255,193,7,0.20);");
        getDialogPane().setPrefWidth(380);
        getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
        getDialogPane().setMaxWidth(Region.USE_PREF_SIZE);
        Button closeBtn = (Button) getDialogPane().lookupButton(ButtonType.CLOSE);
        if (closeBtn != null) closeBtn.setStyle("-fx-background-color: #3a2f27; -fx-text-fill: #f4e4c1; -fx-font-weight: bold; -fx-border-color: #2a1f17; -fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6;");
        this.setOnShown(evv -> {
            javafx.scene.Node header = getDialogPane().lookup(".header-panel");
            if (header != null) header.setStyle("-fx-background-color: #5a4a3a;");
            javafx.scene.Node contentReg = getDialogPane().lookup(".content");
            if (contentReg != null) contentReg.setStyle("-fx-background-color: #3a2f27;");
            javafx.scene.Node buttonBar = getDialogPane().lookup(".button-bar");
            if (buttonBar != null) buttonBar.setStyle("-fx-background-color: #5a4a3a;");
            // Start device hot-plug polling
            refreshDeviceList(true);
            if (deviceRefreshTimer == null) {
                deviceRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(2), e -> refreshDeviceList(true)));
                deviceRefreshTimer.setCycleCount(Timeline.INDEFINITE);
                deviceRefreshTimer.play();
            }
            try {
                Stage st = (Stage) getDialogPane().getScene().getWindow();
                if (st != null) st.sizeToScene();
            } catch (Exception ignored) {}
        });
        
        this.setOnCloseRequest(e -> {
            if (recordingService.isRecording()) {
                stopRecording();
            }
            if (timer != null) {
                timer.stop();
            }
            stopPlayback();
            if (deviceRefreshTimer != null) {
                deviceRefreshTimer.stop();
                deviceRefreshTimer = null;
            }
        });
    }

    private void toggleRecording() {
        if (recordingService.isRecording()) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        try {
            currentRecordingId = recordingService.startRecordingWithSpectrum(level -> {
                // no-op: sensitivity bar removed from UI
            }, bins -> {
                // called from capture thread
                this.spectrumBins = bins;
                Platform.runLater(this::renderSpectrum);
            });
            
            statusLabel.setText("Recording... 🎤");
            statusLabel.setTextFill(Color.RED);
            recordButton.setTooltip(new Tooltip("Stop Recording"));
            spectrumCanvas.setVisible(true);
            spectrumCanvas.setManaged(true);
            micHealthLabel.setVisible(true);
            micHealthLabel.setManaged(true);
            if (deviceCombo != null) deviceCombo.setDisable(true);
            
            elapsedSeconds = 0;
            timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                elapsedSeconds++;
                updateTimerLabel();
            }));
            timer.setCycleCount(Timeline.INDEFINITE);
            timer.play();
            
        } catch (Exception e) {
            showError("Failed to start recording: " + e.getMessage());
        }
    }

    private void stopRecording() {
        try {
            recordingService.stopRecording(currentRecordingId);
            
            if (timer != null) {
                timer.stop();
            }
            
            statusLabel.setText("Recording saved!");
            statusLabel.setTextFill(Color.GREEN);
            recordButton.setTooltip(new Tooltip("Start Recording"));
            spectrumCanvas.setVisible(false);
            spectrumCanvas.setManaged(false);
            micHealthLabel.setVisible(false);
            micHealthLabel.setManaged(false);
            spectrumBins = null;
            if (deviceCombo != null) deviceCombo.setDisable(false);
            
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Recording Saved");
            success.setHeaderText(null);
            success.setContentText("Your audio diary entry has been saved!");
            if (cssUrl != null) success.getDialogPane().getStylesheets().add(cssUrl);
            success.getDialogPane().setStyle("-fx-background-color: #3a2f27; -fx-base: #3a2f27; -fx-control-inner-background: #2e2e2e; -fx-text-background-color: #d4c4a1; -fx-focus-color: #FFC107; -fx-faint-focus-color: rgba(255,193,7,0.20);");
            Button okBtn2 = (Button) success.getDialogPane().lookupButton(ButtonType.OK);
            if (okBtn2 != null) okBtn2.setStyle("-fx-background-color: #3a2f27; -fx-text-fill: #f4e4c1; -fx-font-weight: bold; -fx-border-color: #2a1f17; -fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6;");
            success.setOnShown(evs -> {
                javafx.scene.Node header = success.getDialogPane().lookup(".header-panel");
                if (header != null) header.setStyle("-fx-background-color: #5a4a3a;");
                javafx.scene.Node contentReg = success.getDialogPane().lookup(".content");
                if (contentReg != null) contentReg.setStyle("-fx-background-color: #3a2f27;");
                javafx.scene.Node buttonBar = success.getDialogPane().lookup(".button-bar");
                if (buttonBar != null) buttonBar.setStyle("-fx-background-color: #5a4a3a;");
            });
            success.showAndWait();
            
            elapsedSeconds = 0;
            updateTimerLabel();
            loadRecordings();
            
        } catch (Exception e) {
            showError("Failed to stop recording: " + e.getMessage());
        }
    }

    private void updateTimerLabel() {
        int minutes = elapsedSeconds / 60;
        int seconds = elapsedSeconds % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        if (cssUrl != null) alert.getDialogPane().getStylesheets().add(cssUrl);
        alert.getDialogPane().setStyle("-fx-background-color: #3a2f27; -fx-base: #3a2f27; -fx-control-inner-background: #2e2e2e; -fx-text-background-color: #d4c4a1; -fx-focus-color: #FFC107; -fx-faint-focus-color: rgba(255,193,7,0.20);");
        Button okE = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        if (okE != null) okE.setStyle("-fx-background-color: #3a2f27; -fx-text-fill: #f4e4c1; -fx-font-weight: bold; -fx-border-color: #2a1f17; -fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6;");
        alert.setOnShown(eve -> {
            javafx.scene.Node header = alert.getDialogPane().lookup(".header-panel");
            if (header != null) header.setStyle("-fx-background-color: #5a4a3a;");
            javafx.scene.Node contentReg = alert.getDialogPane().lookup(".content");
            if (contentReg != null) contentReg.setStyle("-fx-background-color: #3a2f27;");
            javafx.scene.Node buttonBar = alert.getDialogPane().lookup(".button-bar");
            if (buttonBar != null) buttonBar.setStyle("-fx-background-color: #5a4a3a;");
        });
        alert.showAndWait();
    }

    private void loadRecordings() {
        try {
            List<Recording> list = recordingService.getAllRecordings();
            recordingsMaster.setAll(list);
        } catch (Exception e) {
            // keep silent in UI; optional toast could be added
        }
    }

    private void playRecording(Recording rec) {
        try {
            stopPlayback();
            File wav = new File(rec.filePath());
            // Preload waveform data and open a fresh stream for Clip
            preloadWaveform(wav);
            AudioInputStream ais2 = AudioSystem.getAudioInputStream(wav);
            currentClip = AudioSystem.getClip();
            currentClip.open(ais2);
            applyVolume();
            currentClip.start();
            paused = false;
            updatePauseButtonIcon();
            playingRecordingId = rec.id();
            startVisualizer();
        } catch (Exception e) {
            showError("Failed to play recording: " + e.getMessage());
        }
    }

    private void stopPlayback() {
        try {
            if (currentClip != null) {
                currentClip.stop();
                currentClip.close();
                currentClip = null;
            }
            stopVisualizer();
            paused = false;
            updatePauseButtonIcon();
            playingRecordingId = null;
        } catch (Exception ignored) {
        }
    }

    private void applyVolume() {
        try {
            if (currentClip == null) return;
            if (currentClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) currentClip.getControl(FloatControl.Type.MASTER_GAIN);
                double vol = volumeSlider != null ? volumeSlider.getValue() : 0.8;
                // Convert linear [0..1] to dB within control's range
                float min = gain.getMinimum();
                float max = gain.getMaximum();
                float dB;
                if (vol <= 0.0001) {
                    dB = min;
                } else {
                    float target = (float) (20.0 * Math.log10(vol));
                    dB = Math.max(min, Math.min(max, target));
                }
                gain.setValue(dB);
            }
        } catch (Exception ignored) {
        }
    }

    // ===== Waveform visualizer helpers =====
    private void preloadWaveform(File wav) throws Exception {
        peaks = null;
        clipLengthUs = 0L;
        try (AudioInputStream in0 = AudioSystem.getAudioInputStream(wav)) {
            AudioFormat base = in0.getFormat();
            AudioFormat fmt = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                base.getSampleRate(),
                16,
                1,
                2,
                base.getSampleRate(),
                false
            );
            try (AudioInputStream in = AudioSystem.getAudioInputStream(fmt, in0)) {
                int sr = (int) fmt.getSampleRate();
                sampleRate = sr;
                long frameLength = in.getFrameLength();
                clipLengthUs = (long) ((frameLength * 1_000_000.0) / fmt.getFrameRate());
                int totalSamples = (int) Math.min(Integer.MAX_VALUE, frameLength);
                int bytesToRead = totalSamples * fmt.getFrameSize();
                byte[] data = in.readNBytes(bytesToRead);
                int samples = data.length / 2;
                double[] mono = new double[samples];
                int idx = 0;
                for (int i = 0; i < samples; i++) {
                    int lo = data[idx++] & 0xff;
                    int hi = data[idx++];
                    short s = (short) ((hi << 8) | lo);
                    mono[i] = s / 32768.0;
                }
                buildPeaks(mono, 1200);
            }
        }
    }

    private void buildPeaks(double[] mono, int maxBins) {
        int bins = Math.max(100, Math.min(maxBins, Math.max(1, mono.length / 50)));
        double[] p = new double[bins];
        double win = (double) mono.length / bins;
        for (int i = 0; i < bins; i++) {
            int start = (int) Math.floor(i * win);
            int end = (int) Math.min(mono.length, Math.floor((i + 1) * win));
            double peak = 0.0;
            for (int j = start; j < end; j++) {
                double v = Math.abs(mono[j]);
                if (v > peak) peak = v;
            }
            p[i] = peak;
        }
        this.peaks = p;
    }

    private void startVisualizer() {
        stopVisualizer();
        vizTimer = new Timeline(new KeyFrame(Duration.millis(33), e -> renderVisualizer()));
        vizTimer.setCycleCount(Timeline.INDEFINITE);
        vizTimer.play();
        if (currentClip != null) {
            currentClip.addLineListener(ev -> {
                if (ev.getType() == LineEvent.Type.STOP) {
                    Platform.runLater(this::stopVisualizer);
                }
            });
        }
    }

    private void stopVisualizer() {
        if (vizTimer != null) {
            vizTimer.stop();
            vizTimer = null;
        }
        renderVisualizer();
    }

    private void renderVisualizer() {
        if (waveformCanvas == null) return;
        GraphicsContext g = waveformCanvas.getGraphicsContext2D();
        double w = waveformCanvas.getWidth();
        double h = waveformCanvas.getHeight();
        g.setFill(Color.web("#3a2f27"));
        g.fillRect(0, 0, w, h);
        if (peaks == null || peaks.length == 0) return;

        int bins = (int) Math.min(peaks.length, Math.max(100, w));
        double scaleX = (double) peaks.length / bins;
        double mid = h / 2.0;
        g.setStroke(Color.web("#7a9b8e"));
        for (int i = 0; i < bins; i++) {
            int src = (int) Math.floor(i * scaleX);
            double v = peaks[src];
            double mag = v * (h * 0.45);
            double x = i * (w / bins);
            g.strokeLine(x, mid - mag, x, mid + mag);
        }

        double px = 0;
        if (currentClip != null && clipLengthUs > 0) {
            px = (currentClip.getMicrosecondPosition() / (double) clipLengthUs) * w;
        }
        g.setStroke(Color.web("#f4e4c1"));
        g.strokeLine(px, 0, px, h);
    }

    private void renderSpectrum() {
        if (spectrumCanvas == null) return;
        GraphicsContext g = spectrumCanvas.getGraphicsContext2D();
        double w = spectrumCanvas.getWidth();
        double h = spectrumCanvas.getHeight();
        g.setFill(Color.web("#3a2f27"));
        g.fillRect(0, 0, w, h);
        float[] bins = this.spectrumBins;
        if (bins == null || bins.length == 0) return;
        int n = bins.length;
        double bw = Math.max(2, w / (n * 1.2));
        double gap = Math.max(1, bw * 0.2);
        g.setFill(Color.web("#FFC107"));
        double peak = 0.0;
        for (int i = 0; i < n; i++) {
            double x = i * (bw + gap) + gap * 0.5;
            double mag = Math.min(1.0, Math.max(0.0, bins[i])) * (h - 8);
            g.fillRoundRect(x, h - mag - 4, bw, mag, 3, 3);
            if (bins[i] > peak) peak = bins[i];
        }
        if (micHealthLabel != null) {
            String txt;
            Color col;
            if (peak < 0.25) { txt = "Level: Low"; col = Color.web("#d4c4a1"); }
            else if (peak > 0.9) { txt = "Level: Clipping"; col = Color.web("#c74440"); }
            else { txt = "Level: Healthy"; col = Color.web("#7a9b8e"); }
            micHealthLabel.setText(txt + String.format("  (peak %.0f%%)", peak * 100));
            micHealthLabel.setTextFill(col);
        }
    }

    private void updatePauseButtonIcon() {
        if (pauseButton == null) return;
        if (imgPause != null) pauseButton.setGraphic(iconView(imgPause, 36));
        pauseButton.setTooltip(new Tooltip("Pause"));
    }

    private String formatGain(double g) {
        return String.format("x%.2f", g);
    }

    private void togglePause() {
        try {
            if (currentClip == null) return;
            if (!paused) {
                currentClip.stop();
                paused = true;
                updatePauseButtonIcon();
                stopVisualizer();
            }
        } catch (Exception ignored) { }
    }

    private void refreshDeviceList(boolean keepSelection) {
        if (deviceCombo == null) return;
        try {
            java.util.List<AudioDevice> newDevices = recordingService.listInputDevices();
            var items = deviceCombo.getItems();
            boolean same = items.size() == newDevices.size();
            if (same) {
                for (int i = 0; i < items.size(); i++) {
                    AudioDevice a = items.get(i);
                    AudioDevice b = newDevices.get(i);
                    if (a == null || b == null || !a.id().equals(b.id()) || !a.name().equals(b.name())) { same = false; break; }
                }
            }
            String selId = null;
            if (keepSelection) {
                var sel = deviceCombo.getSelectionModel().getSelectedItem();
                if (sel != null) selId = sel.id();
            } else {
                selId = recordingService.getInputDevice();
            }
            if (!same) {
                deviceCombo.getItems().setAll(newDevices);
            }
            AudioDevice target = null;
            if (selId != null) {
                for (AudioDevice d : newDevices) { if (d.id().equals(selId)) { target = d; break; } }
            }
            if (target == null) {
                for (AudioDevice d : newDevices) { if ("default".equals(d.id())) { target = d; break; } }
            }
            if (target == null && !newDevices.isEmpty()) target = newDevices.get(0);
            if (target != null) deviceCombo.getSelectionModel().select(target);
        } catch (Exception ignored) { }
    }

    private Comparator<Recording> comparatorFor(String mode) {
        String m = mode == null ? "Newest first" : mode;
        switch (m) {
            case "Oldest first":
                return (a, b) -> a.createdAt().compareTo(b.createdAt());
            case "Name A-Z":
                return (a, b) -> new java.io.File(a.filePath()).getName().toLowerCase()
                        .compareTo(new java.io.File(b.filePath()).getName().toLowerCase());
            case "Name Z-A":
                return (a, b) -> new java.io.File(b.filePath()).getName().toLowerCase()
                        .compareTo(new java.io.File(a.filePath()).getName().toLowerCase());
            case "Duration longest":
                return (a, b) -> {
                    Integer da = a.durationSeconds();
                    Integer db = b.durationSeconds();
                    if (da == null && db == null) return 0;
                    if (da == null) return 1; // null last
                    if (db == null) return -1;
                    return Integer.compare(db, da); // descending
                };
            case "Duration shortest":
                return (a, b) -> {
                    Integer da = a.durationSeconds();
                    Integer db = b.durationSeconds();
                    if (da == null && db == null) return 0;
                    if (da == null) return 1; // null last
                    if (db == null) return -1;
                    return Integer.compare(da, db); // ascending
                };
            case "Newest first":
            default:
                return (a, b) -> b.createdAt().compareTo(a.createdAt());
        }
    }

    private Image loadIcon(String name) {
        try {
            var r = getClass().getResource("/images/" + name);
            return r != null ? new Image(r.toExternalForm()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private ImageView iconView(Image img) { return iconView(img, 16); }
    private ImageView iconView(Image img, int size) {
        ImageView iv = new ImageView(img);
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        iv.getStyleClass().add("cassette-icon");
        return iv;
    }

    private void installPressAnimation(Button b) {
        b.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> applyPressedTransform(b, true));
        b.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> applyPressedTransform(b, false));
        b.addEventHandler(MouseEvent.MOUSE_EXITED, e -> applyPressedTransform(b, false));
    }

    private void applyPressedTransform(Button b, boolean pressed) {
        double scale = pressed ? 0.92 : 1.0;
        double ty = pressed ? 2.0 : 0.0;
        b.setScaleX(scale);
        b.setScaleY(scale);
        b.setTranslateY(ty);
        javafx.scene.Node g = b.getGraphic();
        if (g != null) {
            g.setScaleX(scale);
            g.setScaleY(scale);
            g.setTranslateY(ty);
        }
    }
}
