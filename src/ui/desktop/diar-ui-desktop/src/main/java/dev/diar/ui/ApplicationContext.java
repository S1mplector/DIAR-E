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
    private final SettingsRepository settingsRepository;
    private final AudioCapturePort audioCapturePort;
    private final ClockPort clockPort;
    private final Path recordingsDir;
    
    private CategoryService categoryService;
    private BlockService blockService;
    private RecordingService recordingService;
    private EnergyService energyService;
    private MorningRoutineCoordinator morningRoutineCoordinator;
    private LogQueryService logQueryService;
    private StatisticsService statisticsService;
    private ExportImportService exportImportService;
    private TowerViewService towerViewService;

    public ApplicationContext(
        CategoryRepository categoryRepository,
        LogRepository logRepository,
        TowerRepository towerRepository,
        RecordingRepository recordingRepository,
        SettingsRepository settingsRepository,
        AudioCapturePort audioCapturePort,
        ClockPort clockPort,
        Path recordingsDir
    ) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository);
        this.logRepository = Objects.requireNonNull(logRepository);
        this.towerRepository = Objects.requireNonNull(towerRepository);
        this.recordingRepository = Objects.requireNonNull(recordingRepository);
        this.settingsRepository = Objects.requireNonNull(settingsRepository);
        this.audioCapturePort = Objects.requireNonNull(audioCapturePort);
        this.clockPort = Objects.requireNonNull(clockPort);
        this.recordingsDir = Objects.requireNonNull(recordingsDir);
        
        initializeServices();
    }

    private void initializeServices() {
        this.categoryService = new CategoryService(categoryRepository);
        this.blockService = new BlockService(categoryRepository, logRepository, towerRepository, clockPort);
        this.recordingService = new RecordingService(recordingRepository, audioCapturePort, clockPort, recordingsDir);
        SettingsService settingsService = new SettingsService(settingsRepository);
        this.energyService = new EnergyService(settingsService, clockPort);
        this.morningRoutineCoordinator = new MorningRoutineCoordinator(
            settingsRepository,
            new MorningRoutineService(clockPort),
            energyService,
            clockPort
        );
        this.logQueryService = new LogQueryService(logRepository, categoryRepository, clockPort);
        this.statisticsService = new StatisticsService(categoryRepository, logRepository, towerRepository, clockPort);
        this.exportImportService = new ExportImportService(
            categoryRepository,
            logRepository,
            towerRepository,
            recordingRepository,
            settingsRepository,
            clockPort
        );
        this.towerViewService = new TowerViewService(categoryRepository, logRepository, towerRepository, clockPort);
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

    public EnergyService getEnergyService() {
        return energyService;
    }

    public MorningRoutineCoordinator getMorningRoutineCoordinator() {
        return morningRoutineCoordinator;
    }

    public LogQueryService getLogQueryService() {
        return logQueryService;
    }

    public StatisticsService getStatisticsService() {
        return statisticsService;
    }

    public ExportImportService getExportImportService() {
        return exportImportService;
    }

    public TowerViewService getTowerViewService() {
        return towerViewService;
    }
}
