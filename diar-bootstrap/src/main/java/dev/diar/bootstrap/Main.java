package dev.diar.bootstrap;

import dev.diar.ui.MainApp;
import javafx.application.Application;
import dev.diar.adapter.persistence.sqlite.SQLiteDataSourceFactory;
import dev.diar.adapter.persistence.sqlite.DatabaseMigrator;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        // Prepare app data directory and SQLite DB path
        Path dataDir = Path.of(System.getProperty("user.home"), ".diar-e", "data");
        try {
            Files.createDirectories(dataDir);
            Path dbPath = dataDir.resolve("diar.db");
            DataSource ds = SQLiteDataSourceFactory.create(dbPath);
            DatabaseMigrator.migrate(ds);
        } catch (Exception e) {
            e.printStackTrace();
            // Proceed to UI; in a real app we would show an error dialog
        }
        Application.launch(MainApp.class, args);
    }
}
