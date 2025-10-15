package dev.diar.app.service.fakes;

import dev.diar.app.port.CategoryRepository;
import dev.diar.core.model.Category;

import java.util.*;

public class InMemoryCategoryRepository implements CategoryRepository {
    private final Map<String, Category> storage = new LinkedHashMap<>();

    @Override
    public void save(Category category) {
        storage.put(category.id(), category);
    }

    @Override
    public Optional<Category> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Category> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void delete(String id) {
        storage.remove(id);
    }
}
