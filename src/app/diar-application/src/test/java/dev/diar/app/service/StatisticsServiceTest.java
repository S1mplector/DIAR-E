package dev.diar.app.service;

import dev.diar.app.service.fakes.*;
import dev.diar.core.model.Category;
import dev.diar.core.model.LogEntry;
import dev.diar.core.model.Tower;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StatisticsServiceTest {

    @Test
    void blocksPerDayAndTowersCompletedAndCategoryProgress() {
        var categories = new InMemoryCategoryRepository();
        var logs = new InMemoryLogRepository();
        var towers = new InMemoryTowerRepository();
        var clock = new FakeClock(ZonedDateTime.parse("2025-01-10T12:00:00Z"));

        var c1 = new Category("c1", "Yoga", 3);
        var c2 = new Category("c2", "Reading", 2);
        categories.save(c1);
        categories.save(c2);

        // Logs across last 3 days
        logs.save(new LogEntry("l1", "c1", null, clock.now().minusDays(2)));
        logs.save(new LogEntry("l2", "c1", null, clock.now().minusDays(1)));
        logs.save(new LogEntry("l3", "c2", null, clock.now().minusHours(2)));

        // Towers (one completed today, one active)
        towers.save(new Tower("t1", "c2", 2, 2, clock.today()));
        towers.save(new Tower("t2", "c1", 3, 1, null));

        var stats = new StatisticsService(categories, logs, towers, clock);

        Map<LocalDate, Long> bpd = stats.blocksPerDay(3);
        assertEquals(3, bpd.size());
        long sum = bpd.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(3L, sum);

        Map<LocalDate, Long> tpd = stats.towersCompletedPerDay(3);
        long completedSum = tpd.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(1L, completedSum);

        var cp1 = stats.categoryProgress("c1");
        assertEquals("c1", cp1.categoryId);
        assertEquals(1, cp1.completedBlocks);
        assertEquals(3, cp1.targetBlocks);
        assertTrue(cp1.progressRatio > 0.0);

        List<StatisticsService.CategoryProgress> all = stats.allCategoryProgress();
        assertEquals(2, all.size());
    }
}
