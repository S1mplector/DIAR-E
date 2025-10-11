package dev.diar.app.port;

import java.util.Optional;

public interface SettingsRepository {
    void put(String key, String value);
    Optional<String> get(String key);
}
