package dev.diar.adapter.audio;

import dev.diar.app.port.AudioCapturePort;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class JavaSoundAudioCapturePort implements AudioCapturePort {
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private Path currentTarget;
    private TargetDataLine line;
    private Thread recordingThread;
    private ByteArrayOutputStream audioData;

    @Override
    public void startRecording(Path targetFile, Consumer<Double> levelMeterCallback) {
        if (recording.get()) {
            throw new IllegalStateException("Already recording");
        }
        
        this.currentTarget = targetFile;
        this.audioData = new ByteArrayOutputStream();
        recording.set(true);
        
        recordingThread = new Thread(() -> {
            try {
                // Configure audio format: 16kHz, 16 bit, mono
                AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                
                if (!AudioSystem.isLineSupported(info)) {
                    throw new RuntimeException("Audio line not supported");
                }
                
                line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();
                
                byte[] buffer = new byte[4096];
                while (recording.get()) {
                    int bytesRead = line.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        audioData.write(buffer, 0, bytesRead);
                        
                        // Calculate audio level for meter callback
                        double level = calculateRMSLevel(buffer, bytesRead);
                        if (levelMeterCallback != null) {
                            levelMeterCallback.accept(level);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
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
            if (audioData != null && audioData.size() > 0) {
                byte[] data = audioData.toByteArray();
                AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
                writeWavFile(currentTarget.toFile(), data, format);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return currentTarget;
    }

    @Override
    public boolean isRecording() {
        return recording.get();
    }
    
    private double calculateRMSLevel(byte[] buffer, int length) {
        long sum = 0;
        for (int i = 0; i < length - 1; i += 2) {
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            sum += sample * sample;
        }
        double rms = Math.sqrt((double) sum / (length / 2));
        // Normalize to 0.0 - 1.0 range
        return Math.min(1.0, rms / 32768.0);
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
