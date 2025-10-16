package dev.diar.app.port;

import java.nio.file.Path;
import java.util.function.Consumer;

public interface AudioCapturePort {
    void startRecording(Path targetFile, Consumer<Double> levelMeterCallback) throws Exception;
    default void startRecordingWithSpectrum(Path targetFile, Consumer<Double> levelMeterCallback, Consumer<float[]> spectrumCallback) throws Exception {
        startRecording(targetFile, levelMeterCallback);
    }
    Path stopRecording() throws Exception;
    boolean isRecording();
}
