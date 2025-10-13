package dev.diar.app.service;

import dev.diar.app.port.CategoryRepository;
import dev.diar.app.port.ClockPort;
import dev.diar.app.port.LogRepository;
import dev.diar.core.model.Category;
import dev.diar.core.model.LogEntry;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class LogQueryService {
    private final LogRepository logRepository;
    private final CategoryRepository categoryRepository;
    private final ClockPort clock;

    public LogQueryService(LogRepository logRepository, CategoryRepository categoryRepository, ClockPort clock) {
        this.logRepository = Objects.requireNonNull(logRepository);
        this.categoryRepository = Objects.requireNonNull(categoryRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public List<LogEntry> recentLogs(int lastNDays) {
        if (lastNDays <= 0) return List.of();
        ZonedDateTime to = clock.now();
        ZonedDateTime from = to.minusDays(lastNDays);
        List<LogEntry> all = new ArrayList<>();
        for (Category c : categoryRepository.findAll()) {
            all.addAll(logRepository.findByCategory(c.id(), from, to));
        }
        // sort desc by createdAt
        return all.stream()
            .sorted((a,b) -> b.createdAt().compareTo(a.createdAt()))
            .collect(Collectors.toList());
    }

    public List<LogEntry> logsByCategory(String categoryId, ZonedDateTime fromInclusive, ZonedDateTime toExclusive) {
        return logRepository.findByCategory(categoryId, fromInclusive, toExclusive)
            .stream()
            .sorted((a,b) -> b.createdAt().compareTo(a.createdAt()))
            .collect(Collectors.toList());
    }

    public List<LogEntry> searchNotes(String query, int lastNDays) {
        if (query == null || query.isBlank()) return List.of();
        String q = query.toLowerCase(Locale.ROOT);
        return recentLogs(lastNDays).stream()
            .filter(le -> {
                String note = le.note();
                return note != null && note.toLowerCase(Locale.ROOT).contains(q);
            })
            .collect(Collectors.toList());
    }
}
