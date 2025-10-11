package dev.diar.core.model;

import java.time.ZonedDateTime;
import java.util.Objects;

public final class Block {
    private final String id;
    private final String categoryId;
    private final String note;
    private final ZonedDateTime createdAt;

    public Block(String id, String categoryId, String note, ZonedDateTime createdAt) {
        this.id = Objects.requireNonNull(id);
        this.categoryId = Objects.requireNonNull(categoryId);
        this.note = note;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public String id() { return id; }
    public String categoryId() { return categoryId; }
    public String note() { return note; }
    public ZonedDateTime createdAt() { return createdAt; }
}
