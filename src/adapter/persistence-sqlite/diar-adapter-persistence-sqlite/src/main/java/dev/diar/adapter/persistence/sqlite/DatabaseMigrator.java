package dev.diar.adapter.persistence.sqlite;

import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.util.Objects;

public final class DatabaseMigrator {
    private DatabaseMigrator() {}

    public static void migrate(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource");
        Flyway.configure()
                .baselineOnMigrate(true)
                .locations("classpath:db/migration")
                .dataSource(dataSource)
                .load()
                .migrate();
    }
}
