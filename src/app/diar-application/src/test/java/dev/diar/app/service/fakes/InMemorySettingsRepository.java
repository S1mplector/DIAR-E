package dev.diar.app.service.fakes;

import dev.diar.app.port.SettingsRepository;

import java.util.*;

public class InMemorySettingsRepository implements SettingsRepository {
    private final Map<String, String> storage = new HashMap<>();

    @Override
    public void put(String key, String value) {
        storage.put(key, value);
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(storage.get(key));
    }
}
