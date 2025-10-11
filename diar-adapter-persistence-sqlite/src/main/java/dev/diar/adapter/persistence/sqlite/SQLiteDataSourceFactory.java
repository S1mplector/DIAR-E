package dev.diar.adapter.persistence.sqlite;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.nio.file.Path;

public final class SQLiteDataSourceFactory {
    private SQLiteDataSourceFactory() {}

    public static DataSource create(Path dbPath) {
        HikariConfig config = new HikariConfig();
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        config.setJdbcUrl(url);
        config.setMaximumPoolSize(5);
        config.setPoolName("diar-sqlite-pool");
        return new HikariDataSource(config);
    }
}
