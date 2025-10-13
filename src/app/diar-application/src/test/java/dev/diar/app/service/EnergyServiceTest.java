package dev.diar.app.service;

import dev.diar.app.service.fakes.FakeClock;
import dev.diar.app.service.fakes.InMemorySettingsRepository;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class EnergyServiceTest {

    @Test
    void defaultValuesAndSettersClampAndPersist() {
        var settings = new InMemorySettingsRepository();
        var clock = new FakeClock(ZonedDateTime.parse("2025-01-01T08:00:00Z"));
        var svc = new EnergyService(settings, clock);

        assertEquals(100, svc.getLevel());
        assertFalse(svc.isExhausted());

        svc.setLevel(120);
        assertEquals(100, svc.getLevel());
        svc.setLevel(-5);
        assertEquals(0, svc.getLevel());

        svc.setExhausted(true);
        assertTrue(svc.isExhausted());
    }

    @Test
    void resetForNewDayResetsState() {
        var settings = new InMemorySettingsRepository();
        var clock = new FakeClock(ZonedDateTime.parse("2025-01-01T08:00:00Z"));
        var svc = new EnergyService(settings, clock);

        svc.setLevel(40);
        svc.setExhausted(true);
        svc.resetForNewDay();

        assertEquals(100, svc.getLevel());
        assertFalse(svc.isExhausted());
        assertEquals(clock.today(), svc.getDate());
    }
}
