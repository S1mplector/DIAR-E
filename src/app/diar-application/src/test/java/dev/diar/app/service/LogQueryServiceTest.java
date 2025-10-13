package dev.diar.app.service;

import dev.diar.app.service.fakes.*;
import dev.diar.core.model.Category;
import dev.diar.core.model.LogEntry;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LogQueryServiceTest {

    @Test
    void recentLogsAcrossCategoriesSortedDesc() {
        var categories = new InMemoryCategoryRepository();
        var logs = new InMemoryLogRepository();
        var clock = new FakeClock(ZonedDateTime.parse("2025-01-10T12:00:00Z"));

        var c1 = new Category("c1", "A", 5);
        var c2 = new Category("c2", "B", 5);
        categories.save(c1);
        categories.save(c2);

        logs.save(new LogEntry("l1", "c1", null, clock.now().minusDays(1)));
        logs.save(new LogEntry("l2", "c2", null, clock.now().minusDays(2)));
        logs.save(new LogEntry("l3", "c1", null, clock.now().minusHours(1)));

        var svc = new LogQueryService(logs, categories, clock);
        List<LogEntry> recent = svc.recentLogs(3);
        assertEquals(3, recent.size());
        assertEquals("l3", recent.get(0).id());
    }

    @Test
    void logsByCategoryRangeAndSearchNotes() {
        var categories = new InMemoryCategoryRepository();
        var logs = new InMemoryLogRepository();
        var clock = new FakeClock(ZonedDateTime.parse("2025-01-10T12:00:00Z"));

        var c1 = new Category("c1", "A", 5);
        categories.save(c1);

        logs.save(new LogEntry("l1", "c1", "Did pushups", clock.now().minusDays(1)));
        logs.save(new LogEntry("l2", "c1", "Read a book", clock.now().minusDays(2)));
        logs.save(new LogEntry("l3", "c1", "coding kata", clock.now().minusHours(1)));

        var svc = new LogQueryService(logs, categories, clock);
        var from = clock.now().minusDays(3);
        var to = clock.now().plusHours(1);
        List<LogEntry> byCat = svc.logsByCategory("c1", from, to);
        assertEquals(3, byCat.size());

        List<LogEntry> search = svc.searchNotes("CoDiNg", 3);
        assertEquals(1, search.size());
        assertEquals("l3", search.get(0).id());
    }
}
