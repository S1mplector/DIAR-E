package dev.diar.app.service;

import dev.diar.app.port.AudioCapturePort;
import dev.diar.app.port.ClockPort;
import dev.diar.app.port.RecordingRepository;
import dev.diar.core.model.Recording;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class RecordingService {
    private final RecordingRepository recordingRepository;
    private final AudioCapturePort audioCapturePort;
    private final ClockPort clock;
    private final Path recordingsDir;

    public RecordingService(
        RecordingRepository recordingRepository,
        AudioCapturePort audioCapturePort,
        ClockPort clock,
        Path recordingsDir
    ) {
        this.recordingRepository = Objects.requireNonNull(recordingRepository);
        this.audioCapturePort = Objects.requireNonNull(audioCapturePort);
        this.clock = Objects.requireNonNull(clock);
        this.recordingsDir = Objects.requireNonNull(recordingsDir);
    }

    public String startRecording(Consumer<Double> levelMeterCallback) {
        try {
            Files.createDirectories(recordingsDir);
            String id = UUID.randomUUID().toString();
            String fileName = id + ".wav";
            Path targetFile = recordingsDir.resolve(fileName);
            
            audioCapturePort.startRecording(targetFile, levelMeterCallback);
            
            return id;
        } catch (Exception e) {
            throw new RuntimeException("Failed to start recording", e);
        }
    }

    public Recording stopRecording(String recordingId) {
        Path filePath = audioCapturePort.stopRecording();
        
        Recording recording = new Recording(
            recordingId,
            filePath.toString(),
            clock.now(),
            null // Duration will be calculated later if needed
        );
        recordingRepository.save(recording);
        
        return recording;
    }

    public boolean isRecording() {
        return audioCapturePort.isRecording();
    }

    public List<Recording> getAllRecordings() {
        return recordingRepository.findAll();
    }
}
