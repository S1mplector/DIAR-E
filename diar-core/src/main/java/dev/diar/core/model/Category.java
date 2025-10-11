package dev.diar.core.model;

import java.util.Objects;

public final class Category {
    private final String id;
    private final String name;
    private final int towerBlockTarget;

    public Category(String id, String name, int towerBlockTarget) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        if (towerBlockTarget <= 0) throw new IllegalArgumentException("towerBlockTarget must be > 0");
        this.towerBlockTarget = towerBlockTarget;
    }

    public String id() { return id; }
    public String name() { return name; }
    public int towerBlockTarget() { return towerBlockTarget; }
}
