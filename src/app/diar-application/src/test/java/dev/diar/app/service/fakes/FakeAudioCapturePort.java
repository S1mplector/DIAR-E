package dev.diar.app.service.fakes;

import dev.diar.app.port.AudioCapturePort;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class FakeAudioCapturePort implements AudioCapturePort {
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private Path target;

    @Override
    public void startRecording(Path targetFile, Consumer<Double> levelMeterCallback) throws Exception {
        this.target = targetFile;
        recording.set(true);
        // Optionally tick the meter
        if (levelMeterCallback != null) levelMeterCallback.accept(0.1);
    }

    @Override
    public Path stopRecording() throws Exception {
        if (target != null && recording.get()) {
            // Write a 0.1s silent WAV at 16kHz, 16-bit, mono
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            int durationMs = 100;
            int frames = (int) (format.getFrameRate() * durationMs / 1000.0);
            byte[] pcm = new byte[frames * format.getFrameSize()]; // silence
            try (AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(pcm), format, frames)) {
                Files.createDirectories(target.getParent());
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, target.toFile());
            }
        }
        recording.set(false);
        return target;
    }

    @Override
    public boolean isRecording() {
        return recording.get();
    }
}
