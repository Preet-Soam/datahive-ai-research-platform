package org.datahive.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Small JDBC connection factory. Each request gets its own short-lived connection. */
public final class Database {
    private static final Path DATA_DIRECTORY = AppStorage.databaseDirectory();
    private static final String URL = setting(
            "DATAHIVE_JDBC_URL",
            "jdbc:h2:file:" + DATA_DIRECTORY.resolve("datahive").toString().replace('\\', '/')
                    + ";DB_CLOSE_ON_EXIT=FALSE"
    );
    private static final String USER = setting("DATAHIVE_DB_USER", "sa");
    private static final String PASSWORD = setting("DATAHIVE_DB_PASSWORD", "");

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        if (URL.startsWith("jdbc:h2:")) {
            try {
                Class.forName("org.h2.Driver", true, Database.class.getClassLoader());
            } catch (ClassNotFoundException exception) {
                throw new SQLException("The H2 JDBC driver is missing from the web application.", exception);
            }
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void prepareLocalDirectory() throws IOException {
        if (URL.startsWith("jdbc:h2:file:")) {
            Files.createDirectories(DATA_DIRECTORY);
        }
    }

    public static String jdbcUrl() {
        return URL;
    }

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
