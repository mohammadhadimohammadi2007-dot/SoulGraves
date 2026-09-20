package ir.soulgraves.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private final File file;
    private Connection connection;

    public Database(File pluginFolder) {
        this.file = new File(pluginFolder, "graves.db");
    }

    public void open() throws SQLException {
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        try {
            Class.forName("ir.soulgraves.lib.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            try { Class.forName("org.sqlite.JDBC"); } catch (ClassNotFoundException ignored) {}
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS graves (
                    id                  TEXT PRIMARY KEY,
                    owner_uuid          TEXT NOT NULL,
                    world               TEXT NOT NULL,
                    x                   REAL NOT NULL,
                    y                   REAL NOT NULL,
                    z                   REAL NOT NULL,
                    death_time          INTEGER NOT NULL,
                    creation_playtime   INTEGER NOT NULL,
                    death_cause         TEXT,
                    killer              TEXT,
                    grave_order         INTEGER NOT NULL,
                    marker_type         TEXT NOT NULL,
                    marker_id           TEXT,
                    items_data          BLOB NOT NULL
                )
            """);
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_graves_owner ON graves(owner_uuid)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_graves_world ON graves(world)");
        }
    }

    public Connection getConnection() { return connection; }

    public void close() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
        }
    }
}
