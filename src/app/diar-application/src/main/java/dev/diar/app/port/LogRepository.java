package dev.diar.app.port;

import dev.diar.core.model.LogEntry;
import java.time.ZonedDateTime;
import java.util.List;

public interface LogRepository {
    void save(LogEntry logEntry);
    List<LogEntry> findByCategory(String categoryId, ZonedDateTime fromInclusive, ZonedDateTime toExclusive);
}
