package dev.diar.app.service;

import dev.diar.app.service.fakes.*;
import dev.diar.core.model.Category;
import dev.diar.core.model.Tower;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BlockServiceTest {

    @Test
    void addBlockCreatesLogAndStartsTower() {
        var categories = new InMemoryCategoryRepository();
        var logs = new InMemoryLogRepository();
        var towers = new InMemoryTowerRepository();
        var clock = new FakeClock(ZonedDateTime.parse("2025-01-01T10:00:00Z"));

        // Seed category
        var cat = new Category("c1", "Coding", 3);
        categories.save(cat);

        var svc = new BlockService(categories, logs, towers, clock);
        String logId = svc.addBlock("c1", "Did a kata");
        assertNotNull(logId);

        // tower should be created with 1 block
        List<Tower> byCat = towers.findByCategory("c1");
        assertEquals(1, byCat.size());
        Tower t = byCat.get(0);
        assertEquals(1, t.blocksCompleted());
        assertNull(t.completedOn());
    }

    @Test
    void completingTowerCreatesNewActiveTower() {
        var categories = new InMemoryCategoryRepository();
        var logs = new InMemoryLogRepository();
        var towers = new InMemoryTowerRepository();
        var clock = new FakeClock(ZonedDateTime.parse("2025-01-01T10:00:00Z"));

        var cat = new Category("c1", "Exercise", 2);
        categories.save(cat);
        var svc = new BlockService(categories, logs, towers, clock);

        // Add two blocks to complete
        svc.addBlock("c1", null);
        svc.addBlock("c1", null);

        List<Tower> ts = towers.findByCategory("c1");
        assertEquals(2, ts.size());

        Tower completed = ts.stream().filter(Tower::isCompleted).findFirst().orElseThrow();
        assertEquals(2, completed.blocksCompleted());
        assertNotNull(completed.completedOn());

        Tower active = ts.stream().filter(t -> !t.isCompleted()).findFirst().orElse(null);
        assertNotNull(active);
        assertEquals(0, active.blocksCompleted());
    }
}
