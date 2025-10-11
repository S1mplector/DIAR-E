package dev.diar.app.port;

import dev.diar.core.model.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    void save(Category category);
    Optional<Category> findById(String id);
    List<Category> findAll();
}
