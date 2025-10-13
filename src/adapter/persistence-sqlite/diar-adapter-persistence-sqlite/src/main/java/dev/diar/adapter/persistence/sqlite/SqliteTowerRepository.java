package dev.diar.adapter.persistence.sqlite;

import dev.diar.app.port.TowerRepository;
import dev.diar.core.model.Tower;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SqliteTowerRepository implements TowerRepository {
    private final DataSource dataSource;

    public SqliteTowerRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public void save(Tower tower) {
        String sql = "INSERT INTO towers(id, category_id, block_target, blocks_completed, completed_on) VALUES(?, ?, ?, ?, ?) " +
                     "ON CONFLICT(id) DO UPDATE SET blocks_completed=excluded.blocks_completed, completed_on=excluded.completed_on";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tower.id());
            ps.setString(2, tower.categoryId());
            ps.setInt(3, tower.blockTarget());
            ps.setInt(4, tower.blocksCompleted());
            if (tower.completedOn() != null) {
                ps.setString(5, tower.completedOn().toString());
            } else {
                ps.setNull(5, Types.VARCHAR);
            }
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save tower", e);
        }
    }

    @Override
    public List<Tower> findByCategory(String categoryId) {
        String sql = "SELECT id, category_id, block_target, blocks_completed, completed_on " +
                     "FROM towers WHERE category_id = ? ORDER BY completed_on DESC NULLS FIRST";
        List<Tower> result = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to find towers by category", e);
        }
    }

    private Tower mapRow(ResultSet rs) throws Exception {
        String completedOnStr = rs.getString("completed_on");
        LocalDate completedOn = completedOnStr != null ? LocalDate.parse(completedOnStr) : null;
        return new Tower(
            rs.getString("id"),
            rs.getString("category_id"),
            rs.getInt("block_target"),
            rs.getInt("blocks_completed"),
            completedOn
        );
    }
}
