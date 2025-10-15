package dev.diar.app.service;

import dev.diar.app.port.ClockPort;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Core service for daily energy tracking. Uses SettingsRepository for persistence
 * to avoid a dedicated table until needed.
 */
public class EnergyService {
    private final SettingsService settings;
    private final ClockPort clock;

    public EnergyService(SettingsService settings, ClockPort clock) {
        this.settings = Objects.requireNonNull(settings);
        this.clock = Objects.requireNonNull(clock);
    }

    public int getLevel() {
        return settings.getInt(SettingsService.KEY_ENERGY_LEVEL, 100);
    }

    public boolean isExhausted() {
        return settings.getBool(SettingsService.KEY_ENERGY_EXHAUSTED, false);
    }

    public LocalDate getDate() {
        return settings.getDate(SettingsService.KEY_ENERGY_DATE, clock.today());
    }

    public void setLevel(int level) {
        int clamped = Math.max(0, Math.min(100, level));
        settings.putInt(SettingsService.KEY_ENERGY_LEVEL, clamped);
        settings.putDate(SettingsService.KEY_ENERGY_DATE, clock.today());
    }

    public void setExhausted(boolean exhausted) {
        settings.putBool(SettingsService.KEY_ENERGY_EXHAUSTED, exhausted);
        settings.putDate(SettingsService.KEY_ENERGY_DATE, clock.today());
    }

    public void resetForNewDay() {
        settings.putInt(SettingsService.KEY_ENERGY_LEVEL, 100);
        settings.putBool(SettingsService.KEY_ENERGY_EXHAUSTED, false);
        settings.putDate(SettingsService.KEY_ENERGY_DATE, clock.today());
    }
}
