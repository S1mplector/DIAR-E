package dev.diar.app.service;

import dev.diar.app.port.ClockPort;
import java.time.LocalDate;
import java.util.Objects;

public class MorningRoutineService {
    private final ClockPort clock;

    public MorningRoutineService(ClockPort clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public boolean shouldRunMorningRoutine(LocalDate lastResetDate) {
        LocalDate today = clock.today();
        return lastResetDate == null || today.isAfter(lastResetDate);
    }
}
