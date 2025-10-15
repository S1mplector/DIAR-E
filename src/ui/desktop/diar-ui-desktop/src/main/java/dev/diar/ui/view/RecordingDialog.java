package dev.diar.ui.view;

import dev.diar.app.service.RecordingService;
import dev.diar.core.model.Recording;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import javax.sound.sampled.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    private Button stopButton;
    private Clip currentClip;
    private Slider volumeSlider;

    public RecordingDialog(RecordingService recordingService) {
        this.recordingService = recordingService;
        setupUI();
    }

    private void setupUI() {
        setTitle("Audio Diary");
        setHeaderText("Record your thoughts and reflections");
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        content.setPrefWidth(400);
        
        statusLabel = new Label("Ready to record");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        timerLabel = new Label("00:00");
        timerLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 24));
        
        levelMeter = new ProgressBar(0);
        levelMeter.setPrefWidth(300);
        levelMeter.setStyle("-fx-accent: #7a9b8e;");
        
        recordButton = new Button("⏺ Start Recording");
        recordButton.setStyle(
            "-fx-background-color: #c74440; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 14; " +
            "-fx-padding: 10 20; " +
            "-fx-background-radius: 5;"
        );
        recordButton.setOnAction(e -> toggleRecording());

        // Recordings section
        Label recordingsLabel = new Label("Recordings");
        recordingsLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        recordingsList = new ListView<>();
        recordingsList.setPrefHeight(180);
        recordingsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Recording item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String ts = item.createdAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    setText(ts + "  •  " + new File(item.filePath()).getName());
                }
            }
        });

        playButton = new Button("▶ Play");
        playButton.setOnAction(e -> {
            Recording sel = recordingsList.getSelectionModel().getSelectedItem();
            if (sel != null) playRecording(sel);
        });

        stopButton = new Button("⏹ Stop");
        stopButton.setOnAction(e -> stopPlayback());

        // Volume control
        Label volLabel = new Label("Volume");
        volumeSlider = new Slider(0.0, 1.0, 0.8);
        volumeSlider.setBlockIncrement(0.05);
        volumeSlider.setPrefWidth(200);
        volumeSlider.valueProperty().addListener((obs, ov, nv) -> applyVolume());

        ToolBar playbackBar = new ToolBar(playButton, stopButton, new Separator(), volLabel, volumeSlider);

        loadRecordings();
        
        content.getChildren().addAll(statusLabel, timerLabel, levelMeter, recordButton,
            new Separator(), recordingsLabel, recordingsList, playbackBar);
        
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        setOnCloseRequest(e -> {
            if (recordingService.isRecording()) {
                stopRecording();
            }
            if (timer != null) {
                timer.stop();
            }
            stopPlayback();
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
            currentRecordingId = recordingService.startRecording(level -> {
                javafx.application.Platform.runLater(() -> levelMeter.setProgress(level));
            });
            
            statusLabel.setText("Recording... 🎤");
            statusLabel.setTextFill(Color.RED);
            recordButton.setText("⏹ Stop Recording");
            recordButton.setStyle(
                "-fx-background-color: #5a5a5a; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 14; " +
                "-fx-padding: 10 20; " +
                "-fx-background-radius: 5;"
            );
            
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
            recordButton.setText("⏺ Start Recording");
            recordButton.setStyle(
                "-fx-background-color: #c74440; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 14; " +
                "-fx-padding: 10 20; " +
                "-fx-background-radius: 5;"
            );
            levelMeter.setProgress(0);
            
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Recording Saved");
            success.setHeaderText(null);
            success.setContentText("Your audio diary entry has been saved!");
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
        alert.showAndWait();
    }

    private void loadRecordings() {
        try {
            List<Recording> list = recordingService.getAllRecordings();
            recordingsList.getItems().setAll(list);
        } catch (Exception e) {
            // keep silent in UI; optional toast could be added
        }
    }

    private void playRecording(Recording rec) {
        try {
            stopPlayback();
            File wav = new File(rec.filePath());
            AudioInputStream ais = AudioSystem.getAudioInputStream(wav);
            currentClip = AudioSystem.getClip();
            currentClip.open(ais);
            applyVolume();
            currentClip.start();
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
}
