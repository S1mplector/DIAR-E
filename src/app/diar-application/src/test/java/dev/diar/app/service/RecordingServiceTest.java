package dev.diar.app.service;

import dev.diar.app.service.fakes.*;
import dev.diar.core.model.Recording;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class RecordingServiceTest {

    @Test
    void startAndStopRecordingPersistsWithDuration() {
        var repo = new InMemoryRecordingRepository();
        var audio = new FakeAudioCapturePort();
        var clock = new FakeClock(ZonedDateTime.parse("2025-01-01T10:00:00Z"));
        Path dir = Path.of(System.getProperty("java.io.tmpdir"), "diar-e-tests", "recordings");

        var svc = new RecordingService(repo, audio, clock, dir);

        String id = svc.startRecording(null); // meter not needed in test
        assertNotNull(id);
        assertTrue(svc.isRecording());

        Recording rec = svc.stopRecording(id);
        assertNotNull(rec);
        assertEquals(id, rec.id());
        assertNotNull(rec.durationSeconds());
        assertTrue(rec.durationSeconds() >= 0);
        assertFalse(svc.isRecording());

        assertEquals(1, repo.findAll().size());
    }
}
