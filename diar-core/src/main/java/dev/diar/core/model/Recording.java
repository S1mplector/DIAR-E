package dev.diar.core.model;

import java.time.ZonedDateTime;
import java.util.Objects;

public final class Recording {
    private final String id;
    private final String filePath;
    private final ZonedDateTime createdAt;
    private final Integer durationSeconds; // nullable until known

    public Recording(String id, String filePath, ZonedDateTime createdAt, Integer durationSeconds) {
        this.id = Objects.requireNonNull(id);
        this.filePath = Objects.requireNonNull(filePath);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.durationSeconds = durationSeconds;
    }

    public String id() { return id; }
    public String filePath() { return filePath; }
    public ZonedDateTime createdAt() { return createdAt; }
    public Integer durationSeconds() { return durationSeconds; }
}
