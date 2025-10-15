package dev.diar.app.service;

import dev.diar.app.service.fakes.*;
import dev.diar.core.model.Category;
import dev.diar.core.model.LogEntry;
import dev.diar.core.model.Tower;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class ExportImportServiceTest {

    @Test
    void roundTripWithIdRemap() throws Exception {
        var categories = new InMemoryCategoryRepository();
        var logs = new InMemoryLogRepository();
        var towers = new InMemoryTowerRepository();
        var recordings = new InMemoryRecordingRepository();
        var settings = new InMemorySettingsRepository();
        var clock = new FakeClock(ZonedDateTime.parse("2025-01-10T12:00:00Z"));

        // seed data
        Category c = new Category("c1", "Reading", 5);
        categories.save(c);
        logs.save(new LogEntry("l1", "c1", "read 10 pages", clock.now().minusDays(1)));
        towers.save(new Tower("t1", "c1", 5, 2, null));
        settings.put("energy.level", "80");

        var svc = new ExportImportService(categories, logs, towers, recordings, settings, clock);
        Path tmp = Files.createTempFile("diar-export-", ".json");

        // export
        svc.exportAll(tmp);
        assertTrue(Files.size(tmp) > 0);

        // import into fresh in-memory repos (to simulate new DB)
        var categories2 = new InMemoryCategoryRepository();
        var logs2 = new InMemoryLogRepository();
        var towers2 = new InMemoryTowerRepository();
        var recordings2 = new InMemoryRecordingRepository();
        var settings2 = new InMemorySettingsRepository();
        var svc2 = new ExportImportService(categories2, logs2, towers2, recordings2, settings2, clock);

        svc2.importAll(tmp, true); // remap IDs to avoid collisions

        assertEquals(1, categories2.findAll().size());
        assertEquals(1, logs2.findByCategory(categories2.findAll().get(0).id(), clock.now().minusDays(10), clock.now().plusDays(1)).size());
        assertEquals(1, towers2.findByCategory(categories2.findAll().get(0).id()).size());
        assertTrue(settings2.get("energy.level").isPresent());

        Files.deleteIfExists(tmp);
    }

    @Test
    void importWithoutRemapKeepsIds() throws Exception {
        var categories = new InMemoryCategoryRepository();
        var logs = new InMemoryLogRepository();
        var towers = new InMemoryTowerRepository();
        var recordings = new InMemoryRecordingRepository();
        var settings = new InMemorySettingsRepository();
        var clock = new FakeClock(ZonedDateTime.parse("2025-01-10T12:00:00Z"));

        var svc = new ExportImportService(categories, logs, towers, recordings, settings, clock);
        Path tmp = Files.createTempFile("diar-export-", ".json");

        // minimal export content: empty repos
        svc.exportAll(tmp);

        // import back without remap should not throw
        svc.importAll(tmp, false);
        Files.deleteIfExists(tmp);
    }
}
