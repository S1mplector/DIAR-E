package dev.diar.app.port;

import java.nio.file.Path;
import java.util.function.Consumer;

public interface AudioCapturePort {
    void startRecording(Path targetFile, Consumer<Double> levelMeterCallback) throws Exception;
    Path stopRecording() throws Exception;
    boolean isRecording();
}
