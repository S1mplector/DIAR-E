package dev.diar.app.service;

import dev.diar.app.port.CategoryRepository;
import dev.diar.app.port.LogRepository;
import dev.diar.app.port.TowerRepository;
import dev.diar.app.port.ClockPort;
import dev.diar.core.model.Category;
import dev.diar.core.model.LogEntry;
import dev.diar.core.model.Tower;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class TowerViewService {
    private final CategoryRepository categoryRepository;
    private final LogRepository logRepository;
    private final TowerRepository towerRepository;
    private final ClockPort clock;

    public TowerViewService(CategoryRepository categoryRepository, LogRepository logRepository, TowerRepository towerRepository, ClockPort clock) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository);
        this.logRepository = Objects.requireNonNull(logRepository);
        this.towerRepository = Objects.requireNonNull(towerRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public Optional<Category> getCategory(String categoryId) {
        return categoryRepository.findAll().stream().filter(c -> c.id().equals(categoryId)).findFirst();
    }

    public List<Tower> towersForCategory(String categoryId) {
        List<Tower> towers = new ArrayList<>(towerRepository.findByCategory(categoryId));
        // Sort: completed ones by completedOn desc, active (completedOn == null) last
        towers.sort((a,b) -> {
            if (a.completedOn() == null && b.completedOn() == null) return 0;
            if (a.completedOn() == null) return 1;
            if (b.completedOn() == null) return -1;
            return b.completedOn().compareTo(a.completedOn());
        });
        return towers;
    }

    public List<LogEntry> logsForTower(String categoryId, Tower targetTower) {
        // Fetch logs for category in a wide range and order ascending
        ZonedDateTime now = clock.now();
        List<LogEntry> logs = logRepository.findByCategory(categoryId, now.minusYears(100), now.plusYears(1))
            .stream().sorted(Comparator.comparing(LogEntry::createdAt)).collect(Collectors.toList());

        // Order towers by completion time asc, active last
        List<Tower> towersAsc = new ArrayList<>(towerRepository.findByCategory(categoryId));
        towersAsc.sort((a,b) -> {
            if (a.completedOn() == null && b.completedOn() == null) return 0;
            if (a.completedOn() == null) return 1;
            if (b.completedOn() == null) return -1;
            return a.completedOn().compareTo(b.completedOn());
        });

        // Slice logs assignment by blocksCompleted per tower
        int cursor = 0;
        for (Tower t : towersAsc) {
            int count = Math.max(0, t.blocksCompleted());
            int end = Math.min(cursor + count, logs.size());
            List<LogEntry> slice = logs.subList(cursor, end);
            if (t.id().equals(targetTower.id())) {
                return new ArrayList<>(slice);
            }
            cursor = end;
        }
        return List.of();
    }

    public int stageForTower(Tower tower) {
        int target = Math.max(1, tower.blockTarget());
        double ratio = Math.max(0.0, Math.min(1.0, (double) tower.blocksCompleted() / (double) target));
        int stage = (int) Math.ceil(ratio * 10.0);
        return Math.max(1, Math.min(10, stage));
    }
}
