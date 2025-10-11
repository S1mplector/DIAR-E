package dev.diar.adapter.audio;

import dev.diar.app.port.AudioCapturePort;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class JavaSoundAudioCapturePort implements AudioCapturePort {
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private Path currentTarget;

    @Override
    public void startRecording(Path targetFile, Consumer<Double> levelMeterCallback) {
        this.currentTarget = targetFile;
        recording.set(true);
        // TODO: Implement Java Sound capture in background thread and call levelMeterCallback.accept(level)
    }

    @Override
    public Path stopRecording() {
        recording.set(false);
        // TODO: Close line and persist WAV file
        return currentTarget;
    }

    @Override
    public boolean isRecording() {
        return recording.get();
    }
}
