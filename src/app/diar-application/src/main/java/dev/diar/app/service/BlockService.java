package dev.diar.app.service;

import dev.diar.app.port.CategoryRepository;
import dev.diar.app.port.ClockPort;
import dev.diar.app.port.LogRepository;
import dev.diar.app.port.TowerRepository;
import dev.diar.core.model.Category;
import dev.diar.core.model.LogEntry;
import dev.diar.core.model.Tower;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BlockService {
    private final CategoryRepository categoryRepository;
    private final LogRepository logRepository;
    private final TowerRepository towerRepository;
    private final ClockPort clock;

    public BlockService(
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

    public String addBlock(String categoryId, String note) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));

        String logId = UUID.randomUUID().toString();
        LogEntry logEntry = new LogEntry(logId, categoryId, note, clock.now());
        logRepository.save(logEntry);

        // Update or create tower
        List<Tower> towers = towerRepository.findByCategory(categoryId);
        Tower activeTower = towers.stream()
            .filter(t -> !t.isCompleted())
            .findFirst()
            .orElse(null);

        if (activeTower == null) {
            // Create new tower
            String towerId = UUID.randomUUID().toString();
            activeTower = new Tower(towerId, categoryId, category.towerBlockTarget(), 1, null);
            towerRepository.save(activeTower);
        } else {
            // Update existing tower
            int newCount = activeTower.blocksCompleted() + 1;
            if (newCount >= activeTower.blockTarget()) {
                // Tower completed
                Tower completedTower = new Tower(
                    activeTower.id(),
                    activeTower.categoryId(),
                    activeTower.blockTarget(),
                    newCount,
                    clock.today()
                );
                towerRepository.save(completedTower);
            } else {
                Tower updatedTower = new Tower(
                    activeTower.id(),
                    activeTower.categoryId(),
                    activeTower.blockTarget(),
                    newCount,
                    null
                );
                towerRepository.save(updatedTower);
            }
        }

        return logId;
    }
}
