package dev.diar.app.service;

import dev.diar.app.port.CategoryRepository;
import dev.diar.core.model.Category;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository);
    }

    public String createCategory(String name, int towerBlockTarget) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be blank");
        }
        if (towerBlockTarget <= 0) {
            throw new IllegalArgumentException("Tower block target must be positive");
        }

        String id = UUID.randomUUID().toString();
        Category category = new Category(id, name, towerBlockTarget);
        categoryRepository.save(category);
        return id;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategory(String id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
    }
}
