package dev.diar.app.port;

import dev.diar.core.model.Recording;
import java.util.List;
import java.util.Optional;

public interface RecordingRepository {
    void save(Recording recording);
    Optional<Recording> findById(String id);
    List<Recording> findAll();
    void delete(String id);
}
