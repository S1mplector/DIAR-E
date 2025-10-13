package dev.diar.app.service;

import dev.diar.app.port.ClockPort;
import dev.diar.app.port.SettingsRepository;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Orchestrates daily morning routine using SettingsRepository and services.
 * Stores/reads last reset date from settings under key "last_reset_date".
 */
public class MorningRoutineCoordinator {
    private static final String KEY_LAST_RESET = "last_reset_date";

    private final SettingsRepository settingsRepository;
    private final MorningRoutineService morningRoutineService;
    private final EnergyService energyService;
    private final ClockPort clock;

    public MorningRoutineCoordinator(
        SettingsRepository settingsRepository,
        MorningRoutineService morningRoutineService,
        EnergyService energyService,
        ClockPort clock
    ) {
        this.settingsRepository = Objects.requireNonNull(settingsRepository);
        this.morningRoutineService = Objects.requireNonNull(morningRoutineService);
        this.energyService = Objects.requireNonNull(energyService);
        this.clock = Objects.requireNonNull(clock);
    }

    public boolean runIfDue() {
        LocalDate lastReset = settingsRepository.get(KEY_LAST_RESET)
            .map(LocalDate::parse)
            .orElse(null);
        if (morningRoutineService.shouldRunMorningRoutine(lastReset)) {
            // Perform routine actions
            energyService.resetForNewDay();
            // Persist last reset
            settingsRepository.put(KEY_LAST_RESET, clock.today().toString());
            return true;
        }
        return false;
    }
}
