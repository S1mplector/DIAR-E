package dev.diar.app.port;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public interface AudioCapturePort {
    // Simple descriptor for input devices
    public static record AudioDevice(String id, String name) {}

    void startRecording(Path targetFile, Consumer<Double> levelMeterCallback) throws Exception;
    default void startRecordingWithSpectrum(Path targetFile, Consumer<Double> levelMeterCallback, Consumer<float[]> spectrumCallback) throws Exception {
        startRecording(targetFile, levelMeterCallback);
    }
    Path stopRecording() throws Exception;
    boolean isRecording();
    // Optional input gain control (1.0 = unity). Implementations may clamp to a safe range.
    default void setInputGain(double gain) {}
    default double getInputGain() { return 1.0; }

    // Input device selection APIs
    default List<AudioDevice> listInputDevices() { return java.util.List.of(new AudioDevice("default", "System Default")); }
    default void setInputDevice(String deviceId) {}
    default String getInputDevice() { return "default"; }
}
