package dev.diar.adapter.persistence.sqlite;

import dev.diar.app.port.LogRepository;
import dev.diar.core.model.LogEntry;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SqliteLogRepository implements LogRepository {
    private final DataSource dataSource;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public SqliteLogRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public void save(LogEntry logEntry) {
        String sql = "INSERT INTO logs(id, category_id, note, created_at) VALUES(?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, logEntry.id());
            ps.setString(2, logEntry.categoryId());
            ps.setString(3, logEntry.note());
            ps.setString(4, logEntry.createdAt().format(FORMATTER));
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save log entry", e);
        }
    }

    @Override
    public List<LogEntry> findByCategory(String categoryId, ZonedDateTime fromInclusive, ZonedDateTime toExclusive) {
        String sql = "SELECT id, category_id, note, created_at FROM logs " +
                     "WHERE category_id = ? AND created_at >= ? AND created_at < ? " +
                     "ORDER BY created_at DESC";
        List<LogEntry> result = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, categoryId);
            ps.setString(2, fromInclusive.format(FORMATTER));
            ps.setString(3, toExclusive.format(FORMATTER));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to find logs by category", e);
        }
    }

    private LogEntry mapRow(ResultSet rs) throws Exception {
        return new LogEntry(
            rs.getString("id"),
            rs.getString("category_id"),
            rs.getString("note"),
            ZonedDateTime.parse(rs.getString("created_at"), FORMATTER)
        );
    }
}
