package dev.diar.adapter.persistence.sqlite;

import dev.diar.app.port.RecordingRepository;
import dev.diar.core.model.Recording;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SqliteRecordingRepository implements RecordingRepository {
    private final DataSource dataSource;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public SqliteRecordingRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public void save(Recording recording) {
        String sql = "INSERT INTO recordings(id, file_path, created_at, duration_seconds) VALUES(?, ?, ?, ?) " +
                     "ON CONFLICT(id) DO UPDATE SET file_path=excluded.file_path, created_at=excluded.created_at, duration_seconds=excluded.duration_seconds";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, recording.id());
            ps.setString(2, recording.filePath());
            ps.setString(3, recording.createdAt().format(FORMATTER));
            if (recording.durationSeconds() != null) {
                ps.setInt(4, recording.durationSeconds());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save recording", e);
        }
    }

    @Override
    public Optional<Recording> findById(String id) {
        String sql = "SELECT id, file_path, created_at, duration_seconds FROM recordings WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find recording", e);
        }
    }

    @Override
    public List<Recording> findAll() {
        String sql = "SELECT id, file_path, created_at, duration_seconds FROM recordings ORDER BY created_at DESC";
        List<Recording> result = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to find all recordings", e);
        }
    }

    @Override
    public void delete(String id) {
        String sql = "DELETE FROM recordings WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete recording", e);
        }
    }

    private Recording mapRow(ResultSet rs) throws Exception {
        Integer durationSeconds = rs.getInt("duration_seconds");
        if (rs.wasNull()) {
            durationSeconds = null;
        }
        return new Recording(
            rs.getString("id"),
            rs.getString("file_path"),
            ZonedDateTime.parse(rs.getString("created_at"), FORMATTER),
            durationSeconds
        );
    }
}
