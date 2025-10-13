package dev.diar.app.service;

import dev.diar.app.service.fakes.InMemoryCategoryRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CategoryServiceTest {

    @Test
    void createsCategory() {
        var repo = new InMemoryCategoryRepository();
        var svc = new CategoryService(repo);
        String id = svc.createCategory("Reading", 10);
        assertNotNull(id);
        assertEquals(1, repo.findAll().size());
        assertEquals("Reading", repo.findAll().get(0).name());
    }

    @Test
    void rejectsDuplicateNameCaseInsensitive() {
        var repo = new InMemoryCategoryRepository();
        var svc = new CategoryService(repo);
        svc.createCategory("Exercise", 10);
        var ex = assertThrows(IllegalArgumentException.class,
            () -> svc.createCategory("exercise", 10));
        assertTrue(ex.getMessage().toLowerCase().contains("already exists"));
    }

    @Test
    void rejectsInvalidTarget() {
        var repo = new InMemoryCategoryRepository();
        var svc = new CategoryService(repo);
        assertThrows(IllegalArgumentException.class, () -> svc.createCategory("X", 0));
    }
}
