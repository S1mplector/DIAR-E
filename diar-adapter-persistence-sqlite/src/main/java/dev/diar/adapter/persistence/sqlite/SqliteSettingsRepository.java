package dev.diar.adapter.persistence.sqlite;

import dev.diar.app.port.SettingsRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;
import java.util.Optional;

public class SqliteSettingsRepository implements SettingsRepository {
    private final DataSource dataSource;

    public SqliteSettingsRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public void put(String key, String value) {
        try (Connection c = dataSource.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO settings(key, value) VALUES(?, ?) ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
                ps.setString(1, key);
                ps.setString(2, value);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save setting", e);
        }
    }

    @Override
    public Optional<String> get(String key) {
        try (Connection c = dataSource.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT value FROM settings WHERE key = ?")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.ofNullable(rs.getString(1));
                    }
                    return Optional.empty();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read setting", e);
        }
    }
}
