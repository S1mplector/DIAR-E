package dev.diar.app.service;

import dev.diar.app.port.CategoryRepository;
import dev.diar.app.port.ClockPort;
import dev.diar.app.port.LogRepository;
import dev.diar.app.port.TowerRepository;
import dev.diar.core.model.Category;
import dev.diar.core.model.LogEntry;
import dev.diar.core.model.Tower;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregated statistics computed from repositories.
 */
public class StatisticsService {
    private final CategoryRepository categoryRepository;
    private final LogRepository logRepository;
    private final TowerRepository towerRepository;
    private final ClockPort clock;

    public StatisticsService(
        CategoryRepository categoryRepository,
        LogRepository logRepository,
        TowerRepository towerRepository,
        ClockPort clock
    ) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository);
        this.logRepository = Objects.requireNonNull(logRepository);
        this.towerRepository = Objects.requireNonNull(towerRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public Map<LocalDate, Long> blocksPerDay(int lastNDays) {
        Map<LocalDate, Long> result = new LinkedHashMap<>();
        ZonedDateTime to = clock.now();
        ZonedDateTime from = to.minusDays(lastNDays);
        ZoneId zone = to.getZone();

        for (int i = lastNDays - 1; i >= 0; i--) {
            result.put(to.minusDays(i).toLocalDate(), 0L);
        }

        for (Category c : categoryRepository.findAll()) {
            List<LogEntry> logs = logRepository.findByCategory(c.id(), from, to);
            for (LogEntry le : logs) {
                LocalDate day = le.createdAt().withZoneSameInstant(zone).toLocalDate();
                if (result.containsKey(day)) {
                    result.put(day, result.get(day) + 1);
                }
            }
        }
        return result;
    }

    public Map<LocalDate, Long> towersCompletedPerDay(int lastNDays) {
        Map<LocalDate, Long> result = new LinkedHashMap<>();
        LocalDate today = clock.today();
        for (int i = lastNDays - 1; i >= 0; i--) {
            result.put(today.minusDays(i), 0L);
        }

        for (Category c : categoryRepository.findAll()) {
            List<Tower> towers = towerRepository.findByCategory(c.id());
            for (Tower t : towers) {
                if (t.completedOn() != null) {
                    LocalDate day = t.completedOn();
                    if (result.containsKey(day)) {
                        result.put(day, result.get(day) + 1);
                    }
                }
            }
        }
        return result;
    }

    public CategoryProgress categoryProgress(String categoryId) {
        Category c = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
        Optional<Tower> active = towerRepository.findByCategory(categoryId).stream()
            .filter(t -> !t.isCompleted())
            .findFirst();
        int completed = active.map(Tower::blocksCompleted).orElse(0);
        int target = c.towerBlockTarget();
        double ratio = target > 0 ? (double) completed / (double) target : 0.0;
        return new CategoryProgress(c.id(), c.name(), completed, target, ratio);
    }

    public List<CategoryProgress> allCategoryProgress() {
        return categoryRepository.findAll().stream()
            .map(c -> categoryProgress(c.id()))
            .collect(Collectors.toList());
    }

    public static final class CategoryProgress {
        public final String categoryId;
        public final String categoryName;
        public final int completedBlocks;
        public final int targetBlocks;
        public final double progressRatio;

        public CategoryProgress(String categoryId, String categoryName, int completedBlocks, int targetBlocks, double progressRatio) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.completedBlocks = completedBlocks;
            this.targetBlocks = targetBlocks;
            this.progressRatio = progressRatio;
        }
    }
}
