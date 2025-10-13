package dev.diar.app.service;

import dev.diar.app.port.ClockPort;
import dev.diar.app.port.SettingsRepository;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Core service for daily energy tracking. Uses SettingsRepository for persistence
 * to avoid a dedicated table until needed.
 */
public class EnergyService {
    private static final String KEY_LEVEL = "energy.level";
    private static final String KEY_EXHAUSTED = "energy.exhausted";
    private static final String KEY_DATE = "energy.date";

    private final SettingsRepository settings;
    private final ClockPort clock;

    public EnergyService(SettingsRepository settings, ClockPort clock) {
        this.settings = Objects.requireNonNull(settings);
        this.clock = Objects.requireNonNull(clock);
    }

    public int getLevel() {
        return settings.get(KEY_LEVEL).map(Integer::parseInt).orElse(100);
    }

    public boolean isExhausted() {
        return settings.get(KEY_EXHAUSTED).map(Boolean::parseBoolean).orElse(false);
    }

    public LocalDate getDate() {
        return settings.get(KEY_DATE).map(LocalDate::parse).orElse(clock.today());
    }

    public void setLevel(int level) {
        int clamped = Math.max(0, Math.min(100, level));
        settings.put(KEY_LEVEL, Integer.toString(clamped));
        settings.put(KEY_DATE, clock.today().toString());
    }

    public void setExhausted(boolean exhausted) {
        settings.put(KEY_EXHAUSTED, Boolean.toString(exhausted));
        settings.put(KEY_DATE, clock.today().toString());
    }

    public void resetForNewDay() {
        settings.put(KEY_LEVEL, Integer.toString(100));
        settings.put(KEY_EXHAUSTED, Boolean.toString(false));
        settings.put(KEY_DATE, clock.today().toString());
    }
}
