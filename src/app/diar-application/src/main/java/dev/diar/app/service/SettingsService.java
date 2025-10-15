package dev.diar.app.service;

import dev.diar.app.port.SettingsRepository;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public class SettingsService {
    public static final String KEY_ENERGY_LEVEL = "energy.level";
    public static final String KEY_ENERGY_EXHAUSTED = "energy.exhausted";
    public static final String KEY_ENERGY_DATE = "energy.date";
    public static final String KEY_LAST_RESET_DATE = "last_reset_date";

    private final SettingsRepository repo;

    public SettingsService(SettingsRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    public Optional<String> getRaw(String key) { return repo.get(key); }
    public void putRaw(String key, String value) { repo.put(key, value); }

    public int getInt(String key, int defaultValue) {
        return repo.get(key).map(Integer::parseInt).orElse(defaultValue);
    }

    public void putInt(String key, int value) { repo.put(key, Integer.toString(value)); }

    public boolean getBool(String key, boolean defaultValue) {
        return repo.get(key).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    public void putBool(String key, boolean value) { repo.put(key, Boolean.toString(value)); }

    public LocalDate getDate(String key, LocalDate defaultValue) {
        return repo.get(key).map(LocalDate::parse).orElse(defaultValue);
    }

    public void putDate(String key, LocalDate value) { repo.put(key, value.toString()); }
}
