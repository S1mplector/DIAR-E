package dev.diar.app.service.fakes;

import dev.diar.app.port.LogRepository;
import dev.diar.core.model.LogEntry;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class InMemoryLogRepository implements LogRepository {
    private final Map<String, LogEntry> storage = new LinkedHashMap<>();

    @Override
    public void save(LogEntry logEntry) {
        storage.put(logEntry.id(), logEntry);
    }

    @Override
    public List<LogEntry> findByCategory(String categoryId, ZonedDateTime fromInclusive, ZonedDateTime toExclusive) {
        return storage.values().stream()
            .filter(le -> le.categoryId().equals(categoryId))
            .filter(le -> (le.createdAt().isEqual(fromInclusive) || le.createdAt().isAfter(fromInclusive))
                && le.createdAt().isBefore(toExclusive))
            .sorted(Comparator.comparing(LogEntry::createdAt))
            .collect(Collectors.toList());
    }
}
