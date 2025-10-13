package dev.diar.ui.view;

import dev.diar.app.service.RecordingService;
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

public class RecordingDialog extends Dialog<ButtonType> {
    private final RecordingService recordingService;
    private String currentRecordingId;
    private Button recordButton;
    private ProgressBar levelMeter;
    private Label statusLabel;
    private Label timerLabel;
    private Timeline timer;
    private int elapsedSeconds = 0;

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
        
        content.getChildren().addAll(statusLabel, timerLabel, levelMeter, recordButton);
        
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        setOnCloseRequest(e -> {
            if (recordingService.isRecording()) {
                stopRecording();
            }
            if (timer != null) {
                timer.stop();
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
}
