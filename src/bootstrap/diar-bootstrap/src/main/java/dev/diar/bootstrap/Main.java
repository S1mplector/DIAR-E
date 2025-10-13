package dev.diar.bootstrap;

import dev.diar.adapter.audio.JavaSoundAudioCapturePort;
import dev.diar.adapter.persistence.sqlite.*;
import dev.diar.app.port.*;
import dev.diar.app.service.SystemClock;
import dev.diar.ui.ApplicationContext;
import dev.diar.ui.MainApp;
import javafx.application.Application;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        try {
            // Setup data directories
            Path appDir = Path.of(System.getProperty("user.home"), ".diar-e");
            Path dataDir = appDir.resolve("data");
            Path recordingsDir = appDir.resolve("recordings");
            Files.createDirectories(dataDir);
            Files.createDirectories(recordingsDir);
            
            // Initialize database
            Path dbPath = dataDir.resolve("diar.db");
            DataSource dataSource = SQLiteDataSourceFactory.create(dbPath);
            DatabaseMigrator.migrate(dataSource);
            
            // Create repositories
            CategoryRepository categoryRepository = new SqliteCategoryRepository(dataSource);
            LogRepository logRepository = new SqliteLogRepository(dataSource);
            TowerRepository towerRepository = new SqliteTowerRepository(dataSource);
            RecordingRepository recordingRepository = new SqliteRecordingRepository(dataSource);
            SettingsRepository settingsRepository = new SqliteSettingsRepository(dataSource);
            
            // Create adapters
            AudioCapturePort audioCapturePort = new JavaSoundAudioCapturePort();
            ClockPort clockPort = new SystemClock();
            
            // Create application context
            ApplicationContext applicationContext = new ApplicationContext(
                categoryRepository,
                logRepository,
                towerRepository,
                recordingRepository,
                audioCapturePort,
                clockPort,
                recordingsDir
            );
            
            // Set context and launch UI
            MainApp.setApplicationContext(applicationContext);
            Application.launch(MainApp.class, args);
            
        } catch (Exception e) {
            System.err.println("Failed to start DIAR-E: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
