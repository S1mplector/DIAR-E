package dev.diar.core.model;

import java.time.LocalDate;
import java.util.Objects;

public final class EnergyStatus {
    private final int level; // 0..100
    private final boolean exhausted;
    private final LocalDate lastResetDate; // date the morning routine last ran

    public EnergyStatus(int level, boolean exhausted, LocalDate lastResetDate) {
        if (level < 0 || level > 100) throw new IllegalArgumentException("level must be 0..100");
        this.level = level;
        this.exhausted = exhausted;
        this.lastResetDate = Objects.requireNonNull(lastResetDate);
    }

    public int level() { return level; }
    public boolean exhausted() { return exhausted; }
    public LocalDate lastResetDate() { return lastResetDate; }
}
