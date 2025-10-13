package dev.diar.adapter.persistence.sqlite;

import dev.diar.app.port.CategoryRepository;
import dev.diar.core.model.Category;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SqliteCategoryRepository implements CategoryRepository {
    private final DataSource dataSource;

    public SqliteCategoryRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public void save(Category category) {
        String sql = "INSERT INTO categories(id, name, tower_block_target) VALUES(?, ?, ?) " +
                     "ON CONFLICT(id) DO UPDATE SET name=excluded.name, tower_block_target=excluded.tower_block_target";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, category.id());
            ps.setString(2, category.name());
            ps.setInt(3, category.towerBlockTarget());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save category", e);
        }
    }

    @Override
    public Optional<Category> findById(String id) {
        String sql = "SELECT id, name, tower_block_target FROM categories WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find category", e);
        }
    }

    @Override
    public List<Category> findAll() {
        String sql = "SELECT id, name, tower_block_target FROM categories ORDER BY name";
        List<Category> result = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to find all categories", e);
        }
    }

    private Category mapRow(ResultSet rs) throws Exception {
        return new Category(
            rs.getString("id"),
            rs.getString("name"),
            rs.getInt("tower_block_target")
        );
    }
}
