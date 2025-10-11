package dev.diar.core.model;

import java.time.LocalDate;
import java.util.Objects;

public final class Tower {
    private final String id;
    private final String categoryId;
    private final int blockTarget;
    private final int blocksCompleted;
    private final LocalDate completedOn; // null if not completed

    public Tower(String id, String categoryId, int blockTarget, int blocksCompleted, LocalDate completedOn) {
        if (blockTarget <= 0) throw new IllegalArgumentException("blockTarget must be > 0");
        if (blocksCompleted < 0 || blocksCompleted > blockTarget) throw new IllegalArgumentException("invalid progress");
        this.id = Objects.requireNonNull(id);
        this.categoryId = Objects.requireNonNull(categoryId);
        this.blockTarget = blockTarget;
        this.blocksCompleted = blocksCompleted;
        this.completedOn = completedOn;
    }

    public String id() { return id; }
    public String categoryId() { return categoryId; }
    public int blockTarget() { return blockTarget; }
    public int blocksCompleted() { return blocksCompleted; }
    public LocalDate completedOn() { return completedOn; }
    public boolean isCompleted() { return completedOn != null; }
}
