package dev.diar.app.service;

import dev.diar.app.service.fakes.FakeClock;
import dev.diar.app.service.fakes.InMemorySettingsRepository;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class MorningRoutineCoordinatorTest {

    @Test
    void runsWhenNoLastReset() {
        var settings = new InMemorySettingsRepository();
        var clock = new FakeClock(ZonedDateTime.parse("2025-01-01T07:00:00Z"));
        var energy = new EnergyService(settings, clock);
        var routine = new MorningRoutineCoordinator(settings, new MorningRoutineService(clock), energy, clock);
        boolean ran = routine.runIfDue();
        assertTrue(ran);
        assertEquals(100, energy.getLevel());
        assertFalse(energy.isExhausted());
    }

    @Test
    void doesNotRunTwiceSameDay() {
        var settings = new InMemorySettingsRepository();
        var clock = new FakeClock(ZonedDateTime.parse("2025-01-01T07:00:00Z"));
        var energy = new EnergyService(settings, clock);
        var routine = new MorningRoutineCoordinator(settings, new MorningRoutineService(clock), energy, clock);
        assertTrue(routine.runIfDue());
        assertFalse(routine.runIfDue());
    }

    @Test
    void runsNextDay() {
        var settings = new InMemorySettingsRepository();
        var clock = new FakeClock(ZonedDateTime.parse("2025-01-01T07:00:00Z"));
        var energy = new EnergyService(settings, clock);
        var routine = new MorningRoutineCoordinator(settings, new MorningRoutineService(clock), energy, clock);
        assertTrue(routine.runIfDue());
        clock.setNow(ZonedDateTime.parse("2025-01-02T07:00:00Z"));
        assertTrue(routine.runIfDue());
    }
}
