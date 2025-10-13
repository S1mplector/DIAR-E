package dev.diar.app.service.fakes;

import dev.diar.app.port.TowerRepository;
import dev.diar.core.model.Tower;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryTowerRepository implements TowerRepository {
    private final Map<String, Tower> storage = new LinkedHashMap<>();

    @Override
    public void save(Tower tower) {
        storage.put(tower.id(), tower);
    }

    @Override
    public List<Tower> findByCategory(String categoryId) {
        return storage.values().stream()
            .filter(t -> t.categoryId().equals(categoryId))
            .collect(Collectors.toList());
    }
}
