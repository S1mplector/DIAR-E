package dev.diar.adapter.audio;

import dev.diar.app.port.AudioCapturePort;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class JavaSoundAudioCapturePort implements AudioCapturePort {
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private Path currentTarget;
    private TargetDataLine line;
    private Thread recordingThread;
    private ByteArrayOutputStream audioData;
    private volatile double inputGain = 1.0; // 1.0 = unity
    private volatile String inputDeviceId = "default";
    private volatile Mixer.Info selectedMixerInfo = null;

    @Override
    public void startRecording(Path targetFile, Consumer<Double> levelMeterCallback) {
        if (recording.get()) {
            throw new IllegalStateException("Already recording");
        }
        
        this.currentTarget = targetFile;
        this.audioData = new ByteArrayOutputStream();
        recording.set(true);
        
        // Configure and open line synchronously to fail fast (e.g., missing mic permission)
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                throw new RuntimeException("Audio line not supported for 16kHz/16-bit mono");
            }
            if (selectedMixerInfo != null) {
                Mixer m = AudioSystem.getMixer(selectedMixerInfo);
                line = (TargetDataLine) m.getLine(info);
            } else {
                line = (TargetDataLine) AudioSystem.getLine(info);
            }
            line.open(format);
            line.start();
        } catch (Exception openEx) {
            recording.set(false);
            throw new RuntimeException("Failed to access microphone. Check system permissions.", openEx);
        }

        recordingThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[4096];
                while (recording.get()) {
                    int bytesRead = line.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        // Apply input gain to little-endian 16-bit samples in-place
                        applyGainInPlace(buffer, bytesRead);
                        audioData.write(buffer, 0, bytesRead);

                        // Calculate audio level for meter callback
                        double level = calculateRMSLevel(buffer, bytesRead);
                        if (levelMeterCallback != null) {
                            levelMeterCallback.accept(level);
                        }
                    }
                }
            } catch (Exception e) {
                // Surface errors for caller visibility
                throw new RuntimeException("Audio capture failed", e);
            }
        });
        recordingThread.start();
    }

    @Override
    public void startRecordingWithSpectrum(Path targetFile, Consumer<Double> levelMeterCallback, Consumer<float[]> spectrumCallback) {
        if (recording.get()) {
            throw new IllegalStateException("Already recording");
        }

        this.currentTarget = targetFile;
        this.audioData = new ByteArrayOutputStream();
        recording.set(true);

        // Configure and open line
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                throw new RuntimeException("Audio line not supported for 16kHz/16-bit mono");
            }
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
        } catch (Exception openEx) {
            recording.set(false);
            throw new RuntimeException("Failed to access microphone. Check system permissions.", openEx);
        }

        recordingThread = new Thread(() -> {
            try {
                final int windowSamples = 1024; // ~64ms @16kHz
                final int binCount = 48;
                short[] window = new short[windowSamples];
                int wpos = 0;
                byte[] buffer = new byte[4096];
                while (recording.get()) {
                    int bytesRead = line.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        // Apply input gain to samples before saving/analysis
                        applyGainInPlace(buffer, bytesRead);
                        audioData.write(buffer, 0, bytesRead);

                        double level = calculateRMSLevel(buffer, bytesRead);
                        if (levelMeterCallback != null) levelMeterCallback.accept(level);

                        // Append to window
                        for (int i = 0; i < bytesRead - 1; i += 2) {
                            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                            window[wpos++] = sample;
                            if (wpos >= windowSamples) {
                                // Compute spectrum bins using naive DFT at equally spaced frequencies up to Nyquist
                                float[] bins = new float[binCount];
                                double twoPiOverN = 2 * Math.PI / windowSamples;
                                // Hanning window and DFT accumulation
                                for (int b = 0; b < binCount; b++) {
                                    int k = (int) Math.round((b + 1) * (windowSamples / 2.0) / (binCount + 1));
                                    double sumRe = 0, sumIm = 0;
                                    for (int n = 0; n < windowSamples; n++) {
                                        double w = 0.5 - 0.5 * Math.cos(2 * Math.PI * n / (windowSamples - 1));
                                        double x = window[n] * w;
                                        double ang = twoPiOverN * k * n;
                                        sumRe += x * Math.cos(ang);
                                        sumIm -= x * Math.sin(ang);
                                    }
                                    double mag = Math.sqrt(sumRe * sumRe + sumIm * sumIm) / (windowSamples * 2048.0);
                                    // Apply gain and soft compression for perceived loudness
                                    double boosted = Math.pow(mag * 8.0, 0.6); // gain x8, gamma 0.6
                                    bins[b] = (float) Math.max(0.0, Math.min(1.0, boosted));
                                }
                                if (spectrumCallback != null) spectrumCallback.accept(bins);
                                wpos = 0; // reset window
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Audio capture failed", e);
            }
        });
        recordingThread.start();
    }

    @Override
    public Path stopRecording() {
        if (!recording.get()) {
            return currentTarget;
        }
        
        recording.set(false);
        
        try {
            if (recordingThread != null) {
                recordingThread.join(2000);
            }
            
            if (line != null) {
                line.stop();
                line.close();
            }
            
            // Write WAV file
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            byte[] data;
            if (audioData != null && audioData.size() > 0) {
                data = audioData.toByteArray();
            } else {
                // Ensure a minimal file exists to avoid playback errors; write 0.2s of silence
                int frames = (int) (format.getFrameRate() * 0.2);
                data = new byte[frames * format.getFrameSize()];
            }
            writeWavFile(currentTarget.toFile(), data, format);
        } catch (Exception e) {
            throw new RuntimeException("Failed to finalize recording", e);
        }
        
        return currentTarget;
    }

    @Override
    public boolean isRecording() {
        return recording.get();
    }
    
    @Override
    public void setInputGain(double gain) {
        // Clamp to a safe range [0.5, 8.0]
        if (Double.isNaN(gain) || Double.isInfinite(gain)) return;
        double g = Math.max(0.5, Math.min(8.0, gain));
        this.inputGain = g;
    }

    @Override
    public double getInputGain() {
        return inputGain;
    }
    
    @Override
    public List<AudioDevice> listInputDevices() {
        List<AudioDevice> res = new ArrayList<>();
        res.add(new AudioDevice("default", "System Default"));
        Mixer.Info[] infos = AudioSystem.getMixerInfo();
        for (int i = 0; i < infos.length; i++) {
            Mixer.Info mi = infos[i];
            try {
                Mixer m = AudioSystem.getMixer(mi);
                Line.Info[] targets = m.getTargetLineInfo(new DataLine.Info(TargetDataLine.class, new AudioFormat(16000,16,1,true,false)));
                if (targets != null && targets.length > 0) {
                    String id = "mixer:" + i;
                    String name = mi.getName();
                    String vend = mi.getVendor();
                    if (vend != null && !vend.isBlank()) name = name + " (" + vend + ")";
                    res.add(new AudioDevice(id, name));
                }
            } catch (Exception ignored) {}
        }
        return res;
    }

    @Override
    public void setInputDevice(String deviceId) {
        if (deviceId == null || deviceId.isBlank() || deviceId.equals("default")) {
            this.inputDeviceId = "default";
            this.selectedMixerInfo = null;
            return;
        }
        if (deviceId.startsWith("mixer:")) {
            try {
                int idx = Integer.parseInt(deviceId.substring("mixer:".length()));
                Mixer.Info[] infos = AudioSystem.getMixerInfo();
                if (idx >= 0 && idx < infos.length) {
                    this.selectedMixerInfo = infos[idx];
                    this.inputDeviceId = deviceId;
                    return;
                }
            } catch (Exception ignored) {}
        }
        // fallback
        this.inputDeviceId = "default";
        this.selectedMixerInfo = null;
    }

    @Override
    public String getInputDevice() {
        return inputDeviceId;
    }
    
    private double calculateRMSLevel(byte[] buffer, int length) {
        long sum = 0;
        for (int i = 0; i < length - 1; i += 2) {
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            sum += sample * sample;
        }
        double rms = Math.sqrt((double) sum / (length / 2));
        // Calibrate to 0.0 - 1.0 with higher sensitivity
        return Math.min(1.0, rms / 6000.0);
    }
    
    private void applyGainInPlace(byte[] buffer, int length) {
        double g = this.inputGain;
        if (Math.abs(g - 1.0) < 1e-6) return; // fast path
        for (int i = 0; i < length - 1; i += 2) {
            short s = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            int v = (int) Math.round(s * g);
            if (v > Short.MAX_VALUE) v = Short.MAX_VALUE;
            else if (v < Short.MIN_VALUE) v = Short.MIN_VALUE;
            buffer[i] = (byte) (v & 0xFF);
            buffer[i + 1] = (byte) ((v >>> 8) & 0xFF);
        }
    }
    
    private void writeWavFile(File file, byte[] audioData, AudioFormat format) throws Exception {
        AudioInputStream ais = new AudioInputStream(
            new java.io.ByteArrayInputStream(audioData),
            format,
            audioData.length / format.getFrameSize()
        );
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file);
        ais.close();
    }
}
