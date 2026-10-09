package org.datahive.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.datahive.security.PasswordHasher;
import org.datahive.dao.DatasetDao;
import org.datahive.service.CsvProfiler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Creates the local schema and a small, clearly identified demo workspace on first launch. */
public final class DatabaseInitializer implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            Database.prepareLocalDirectory();
            initializeSchema(event);
            if (demoModeEnabled()) {
                seedDemoWorkspace();
                seedDemoDataset();
            } else {
                LOGGER.info("Demo account and dataset seeding is disabled by DATAHIVE_DEMO_MODE");
            }
            LOGGER.info(() -> "DataHive database ready: " + Database.jdbcUrl());
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "DataHive could not initialize its database", exception);
            throw new IllegalStateException("DataHive database initialization failed", exception);
        }
    }

    private static boolean demoModeEnabled() {
        String value = System.getenv("DATAHIVE_DEMO_MODE");
        return value == null || !"false".equalsIgnoreCase(value.trim());
    }

    private void initializeSchema(ServletContextEvent event) throws IOException, SQLException {
        try (InputStream input = DatabaseInitializer.class.getClassLoader().getResourceAsStream("db/schema.sql")) {
            if (input == null) {
                throw new IOException("Database schema resource db/schema.sql was not packaged");
            }
            String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection connection = Database.getConnection(); Statement statement = connection.createStatement()) {
                for (String sql : schema.split(";")) {
                    if (!sql.isBlank()) {
                        statement.execute(sql.trim());
                    }
                }
            }
        }
    }

    private void seedDemoWorkspace() throws SQLException {
        try (Connection connection = Database.getConnection()) {
            if (count(connection, "users") > 0) {
                return;
            }
            connection.setAutoCommit(false);
            try {
                long adminId = insertUser(connection, "DataHive Admin", "admin@datahive.local",
                        "AdminPass123!", "ADMIN");
                long researcherId = insertUser(connection, "Researcher Demo", "researcher@datahive.local",
                        "ResearcherPass123!", "RESEARCHER");

                long projectId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO projects (title, description, owner_id) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, "Research workspace");
                    statement.setString(2, "A starter project for exploring datasets and recording experiments.");
                    statement.setLong(3, researcherId);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("The demo project ID was not returned");
                        }
                        projectId = keys.getLong(1);
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO project_members (project_id, user_id, member_role) VALUES (?, ?, ?)");) {
                    statement.setLong(1, projectId);
                    statement.setLong(2, researcherId);
                    statement.setString(3, "OWNER");
                    statement.executeUpdate();
                }

                insertResource(connection, "CPU training pool", "COMPUTE", 4, "vCPU", "AVAILABLE",
                        "Demo resource record for local review.");
                insertResource(connection, "Research file storage", "STORAGE", 25, "GB", "AVAILABLE",
                        "Demo resource record for local review.");
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void seedDemoDataset() throws SQLException, IOException {
        try (Connection connection = Database.getConnection()) {
            String seeded = setting(connection, "demo_binary_dataset_seeded");
            if ("true".equals(seeded)) return;
            long projectId = 0;
            long researcherId = 0;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT p.id, u.id FROM projects p JOIN users u ON u.id = p.owner_id " +
                            "WHERE LOWER(u.email) = 'researcher@datahive.local' " +
                            "AND p.title = 'Research workspace' ORDER BY p.id FETCH FIRST 1 ROW ONLY");
                 ResultSet result = statement.executeQuery()) {
                if (result.next()) { projectId = result.getLong(1); researcherId = result.getLong(2); }
            }
            if (projectId == 0) {
                markSetting(connection, "demo_binary_dataset_seeded", "true");
                return;
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM datasets WHERE project_id = ? AND name = 'Synthetic churn baseline'")) {
                statement.setLong(1, projectId);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        markSetting(connection, "demo_binary_dataset_seeded", "true");
                        return;
                    }
                }
            }

            Path root = AppStorage.uploadDirectory();
            Files.createDirectories(root);
            String storedName = "demo-" + java.util.UUID.randomUUID().toString().replace("-", "") + ".csv";
            Path file = root.resolve(storedName).normalize();
            if (!file.startsWith(root)) throw new IOException("Invalid demo dataset path");
            try {
                try (java.io.BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    writer.write("age,tenure_months,monthly_spend,support_tickets,churned\n");
                    for (int index = 0; index < 160; index++) {
                        int age = 20 + (index * 7) % 48;
                        int tenure = (index * 13) % 60;
                        double spend = 24 + ((index * 29) % 230) / 10.0;
                        int tickets = (index * 5 + index / 6) % 8;
                        String churned = tickets >= 4 || (tenure < 8 && spend > 31) ? "yes" : "no";
                        writer.write(age + "," + tenure + "," + String.format(java.util.Locale.ROOT, "%.1f", spend) +"," + tickets + "," + churned + "\n");
                    }
                }
                CsvProfiler.Profile profile = new CsvProfiler().inspect(file);
                new DatasetDao().create(projectId, "Synthetic churn baseline",
                        "Deterministically generated example data for demonstrating profiling and binary classification.",
                        "synthetic_churn.csv", storedName, Files.size(file), researcherId, profile);
                markSetting(connection, "demo_binary_dataset_seeded", "true");
            } catch (SQLException | IOException | RuntimeException exception) {
                Files.deleteIfExists(file);
                throw exception;
            }
        }
    }

    private static String setting(Connection connection, String key) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT setting_value FROM application_settings WHERE setting_key = ?")) {
            statement.setString(1, key);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? result.getString(1) : null; }
        }
    }

    private static void markSetting(Connection connection, String key, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "MERGE INTO application_settings (setting_key, setting_value, updated_at) KEY (setting_key) " +
                        "VALUES (?, ?, CURRENT_TIMESTAMP)")) {
            statement.setString(1, key); statement.setString(2, value); statement.executeUpdate();
        }
    }

    private static long insertUser(Connection connection, String name, String email, String password, String role)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users (full_name, email, password_hash, role) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, PasswordHasher.hash(password));
            statement.setString(4, role);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The demo account ID was not returned");
                }
                return keys.getLong(1);
            }
        }
    }

    private static void insertResource(Connection connection, String name, String type, double capacity,
                                       String unit, String status, String description) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO resources (name, resource_type, capacity, capacity_unit, status, description) " +
                        "VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, name);
            statement.setString(2, type);
            statement.setDouble(3, capacity);
            statement.setString(4, unit);
            statement.setString(5, status);
            statement.setString(6, description);
            statement.executeUpdate();
        }
    }

    private static long count(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return result.next() ? result.getLong(1) : 0;
        }
    }
}
