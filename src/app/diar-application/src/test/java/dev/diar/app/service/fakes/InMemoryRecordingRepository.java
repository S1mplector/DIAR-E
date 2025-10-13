package dev.diar.app.service.fakes;

import dev.diar.app.port.RecordingRepository;
import dev.diar.core.model.Recording;

import java.util.*;

public class InMemoryRecordingRepository implements RecordingRepository {
    private final Map<String, Recording> storage = new LinkedHashMap<>();

    @Override
    public void save(Recording recording) {
        storage.put(recording.id(), recording);
    }

    @Override
    public Optional<Recording> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Recording> findAll() {
        List<Recording> list = new ArrayList<>(storage.values());
        list.sort((a,b) -> b.createdAt().compareTo(a.createdAt()));
        return list;
    }
}
