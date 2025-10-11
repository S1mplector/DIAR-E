package dev.diar.app.port;

import dev.diar.core.model.Tower;
import java.util.List;

public interface TowerRepository {
    void save(Tower tower);
    List<Tower> findByCategory(String categoryId);
}
