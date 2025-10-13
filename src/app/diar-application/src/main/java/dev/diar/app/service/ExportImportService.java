package dev.diar.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.diar.app.port.*;
import dev.diar.core.model.Category;
import dev.diar.core.model.LogEntry;
import dev.diar.core.model.Recording;
import dev.diar.core.model.Tower;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;

public class ExportImportService {
    private final CategoryRepository categoryRepository;
    private final LogRepository logRepository;
    private final TowerRepository towerRepository;
    private final RecordingRepository recordingRepository;
    private final SettingsRepository settingsRepository;
    private final ClockPort clock;
    private final ObjectMapper mapper;

    public ExportImportService(
        CategoryRepository categoryRepository,
        LogRepository logRepository,
        TowerRepository towerRepository,
        RecordingRepository recordingRepository,
        SettingsRepository settingsRepository,
        ClockPort clock
    ) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository);
        this.logRepository = Objects.requireNonNull(logRepository);
        this.towerRepository = Objects.requireNonNull(towerRepository);
        this.recordingRepository = Objects.requireNonNull(recordingRepository);
        this.settingsRepository = Objects.requireNonNull(settingsRepository);
        this.clock = Objects.requireNonNull(clock);
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void exportAll(Path file) {
        try {
            Files.createDirectories(file.getParent());
            ExportBundle bundle = new ExportBundle();

            // Categories
            List<Category> categories = categoryRepository.findAll();
            bundle.categories = categories;

            // Towers and Logs per category
            ZonedDateTime now = clock.now();
            ZonedDateTime from = now.minusYears(100);
            List<Tower> towers = new ArrayList<>();
            List<LogEntry> logs = new ArrayList<>();
            for (Category c : categories) {
                towers.addAll(towerRepository.findByCategory(c.id()));
                logs.addAll(logRepository.findByCategory(c.id(), from, now.plusYears(1)));
            }
            bundle.towers = towers;
            bundle.logs = logs;

            // Recordings metadata
            bundle.recordings = recordingRepository.findAll();

            // Selected settings keys
            Map<String,String> settings = new LinkedHashMap<>();
            for (String key : List.of("energy.level", "energy.exhausted", "energy.date", "last_reset_date")) {
                settingsRepository.get(key).ifPresent(val -> settings.put(key, val));
            }
            bundle.settings = settings;

            mapper.writeValue(file.toFile(), bundle);
        } catch (IOException e) {
            throw new RuntimeException("Export failed", e);
        }
    }

    public void importAll(Path file, boolean remapIds) {
        try {
            ExportBundle bundle = mapper.readValue(file.toFile(), ExportBundle.class);
            if (bundle == null) return;

            Map<String,String> catIdMap = new HashMap<>();

            // Categories first
            for (Category c : nonNull(bundle.categories)) {
                String id = c.id();
                String newId = remapIds ? UUID.randomUUID().toString() : id;
                catIdMap.put(id, newId);
                Category toSave = new Category(newId, c.name(), c.towerBlockTarget());
                categoryRepository.save(toSave);
            }

            // Towers next
            for (Tower t : nonNull(bundle.towers)) {
                String newId = remapIds ? UUID.randomUUID().toString() : t.id();
                String newCatId = catIdMap.getOrDefault(t.categoryId(), t.categoryId());
                Tower toSave = new Tower(newId, newCatId, t.blockTarget(), t.blocksCompleted(), t.completedOn());
                towerRepository.save(toSave);
            }

            // Logs next
            for (LogEntry le : nonNull(bundle.logs)) {
                String newId = remapIds ? UUID.randomUUID().toString() : le.id();
                String newCatId = catIdMap.getOrDefault(le.categoryId(), le.categoryId());
                LogEntry toSave = new LogEntry(newId, newCatId, le.note(), le.createdAt());
                logRepository.save(toSave);
            }

            // Recordings metadata (do not move audio files; just import metadata)
            for (Recording rec : nonNull(bundle.recordings)) {
                String newId = remapIds ? UUID.randomUUID().toString() : rec.id();
                Recording toSave = new Recording(newId, rec.filePath(), rec.createdAt(), rec.durationSeconds());
                recordingRepository.save(toSave);
            }

            // Settings
            if (bundle.settings != null) {
                bundle.settings.forEach(settingsRepository::put);
            }
        } catch (IOException e) {
            throw new RuntimeException("Import failed", e);
        }
    }

    private static <T> List<T> nonNull(List<T> list) {
        return list == null ? List.of() : list;
    }

    public static class ExportBundle {
        public List<Category> categories;
        public List<Tower> towers;
        public List<LogEntry> logs;
        public List<Recording> recordings;
        public Map<String,String> settings;
    }
}
