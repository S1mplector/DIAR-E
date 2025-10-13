package dev.diar.ui;

import dev.diar.app.port.*;
import dev.diar.app.service.*;

import java.nio.file.Path;
import java.util.Objects;

public class ApplicationContext {
    private final CategoryRepository categoryRepository;
    private final LogRepository logRepository;
    private final TowerRepository towerRepository;
    private final RecordingRepository recordingRepository;
    private final AudioCapturePort audioCapturePort;
    private final ClockPort clockPort;
    private final Path recordingsDir;
    
    private CategoryService categoryService;
    private BlockService blockService;
    private RecordingService recordingService;

    public ApplicationContext(
        CategoryRepository categoryRepository,
        LogRepository logRepository,
        TowerRepository towerRepository,
        RecordingRepository recordingRepository,
        AudioCapturePort audioCapturePort,
        ClockPort clockPort,
        Path recordingsDir
    ) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository);
        this.logRepository = Objects.requireNonNull(logRepository);
        this.towerRepository = Objects.requireNonNull(towerRepository);
        this.recordingRepository = Objects.requireNonNull(recordingRepository);
        this.audioCapturePort = Objects.requireNonNull(audioCapturePort);
        this.clockPort = Objects.requireNonNull(clockPort);
        this.recordingsDir = Objects.requireNonNull(recordingsDir);
        
        initializeServices();
    }

    private void initializeServices() {
        this.categoryService = new CategoryService(categoryRepository);
        this.blockService = new BlockService(categoryRepository, logRepository, towerRepository, clockPort);
        this.recordingService = new RecordingService(recordingRepository, audioCapturePort, clockPort, recordingsDir);
    }

    public CategoryService getCategoryService() {
        return categoryService;
    }

    public BlockService getBlockService() {
        return blockService;
    }

    public RecordingService getRecordingService() {
        return recordingService;
    }
}
