package org.datahive.dao;

import org.datahive.config.Database;
import org.datahive.model.Resource;
import org.datahive.model.Role;
import org.datahive.model.UsageBucket;
import org.datahive.model.User;
import org.datahive.security.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class AdminDao {
    public List<User> listUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, full_name, email, role, active FROM users ORDER BY created_at DESC");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                users.add(new User(result.getLong("id"), result.getString("full_name"), result.getString("email"),
                        Role.fromDatabase(result.getString("role")), result.getBoolean("active")));
            }
        }
        return users;
    }

    public long countActiveUsers() throws SQLException {
        return scalar("SELECT COUNT(*) FROM users WHERE active = TRUE");
    }

    public long countActiveResources() throws SQLException {
        return scalar("SELECT COUNT(*) FROM resources WHERE status <> 'OFFLINE'");
    }

    public void createUser(String name, String email, String password, Role role) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, password_hash, role) VALUES (?, ?, ?, ?)";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, email.toLowerCase(java.util.Locale.ROOT));
            statement.setString(3, PasswordHasher.hash(password));
            statement.setString(4, role.name());
            statement.executeUpdate();
        }
    }

    public boolean updateUser(long id, String name, String email, Role role, boolean active) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET full_name = ?, email = ?, role = ?, active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            statement.setString(1, name);
            statement.setString(2, email.toLowerCase(java.util.Locale.ROOT));
            statement.setString(3, role.name());
            statement.setBoolean(4, active);
            statement.setLong(5, id);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean setUserActive(long id, boolean active) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            statement.setBoolean(1, active);
            statement.setLong(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    public List<Resource> listResources() throws SQLException {
        List<Resource> resources = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, name, resource_type, capacity, capacity_unit, status, description FROM resources ORDER BY resource_type, name");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                resources.add(new Resource(result.getLong("id"), result.getString("name"),
                        result.getString("resource_type"), result.getDouble("capacity"),
                        result.getString("capacity_unit"), result.getString("status"), result.getString("description")));
            }
        }
        return resources;
    }

    public void createResource(String name, String type, double capacity, String unit, String status, String description)
            throws SQLException {
        String sql = "INSERT INTO resources (name, resource_type, capacity, capacity_unit, status, description) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, type);
            statement.setDouble(3, capacity);
            statement.setString(4, unit);
            statement.setString(5, status);
            statement.setString(6, description);
            statement.executeUpdate();
        }
    }

    public boolean updateResource(long id, String name, String type, double capacity, String unit, String status,
                                  String description) throws SQLException {
        String sql = "UPDATE resources SET name = ?, resource_type = ?, capacity = ?, capacity_unit = ?, " +
                "status = ?, description = ? WHERE id = ?";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, type);
            statement.setDouble(3, capacity);
            statement.setString(4, unit);
            statement.setString(5, status);
            statement.setString(6, description);
            statement.setLong(7, id);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean deleteResource(long id) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM resources WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    public UsageTotals usageTotals() throws SQLException {
        String sql = "SELECT " +
                "COALESCE(SUM(CASE WHEN usage_type = 'COMPUTE_SECONDS' THEN amount ELSE 0 END), 0) AS compute_seconds, " +
                "COALESCE(SUM(CASE WHEN usage_type = 'STORAGE_BYTES' THEN amount ELSE 0 END), 0) AS storage_bytes " +
                "FROM usage_records";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return new UsageTotals(result.getDouble("compute_seconds"), result.getDouble("storage_bytes"));
        }
    }

    private static long scalar(String sql) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getLong(1) : 0;
        }
    }

    public List<UsageBucket> usageByMonth(int months) throws SQLException {
        String sql = "SELECT YEAR(recorded_at) AS usage_year, MONTH(recorded_at) AS usage_month, " +
                "SUM(CASE WHEN usage_type = 'COMPUTE_SECONDS' THEN amount ELSE 0 END) AS compute_seconds, " +
                "SUM(CASE WHEN usage_type = 'STORAGE_BYTES' THEN amount ELSE 0 END) AS storage_bytes " +
                "FROM usage_records WHERE recorded_at >= DATEADD('MONTH', ?, CURRENT_DATE) " +
                "GROUP BY YEAR(recorded_at), MONTH(recorded_at) ORDER BY usage_year, usage_month";
        List<UsageBucket> buckets = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, -months);
            try (ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                String label = result.getInt("usage_year") + "-" + String.format("%02d", result.getInt("usage_month"));
                buckets.add(new UsageBucket(label, result.getDouble("compute_seconds"), result.getDouble("storage_bytes")));
            }
            }
        }
        return buckets;
    }

    public static final class UsageTotals {
        private final double computeSeconds;
        private final double storageBytes;
        public UsageTotals(double computeSeconds, double storageBytes) {
            this.computeSeconds = computeSeconds;
            this.storageBytes = storageBytes;
        }
        public double getComputeSeconds() { return computeSeconds; }
        public double getStorageBytes() { return storageBytes; }
        public String getComputeLabel() { return new java.text.DecimalFormat("#,##0.0").format(computeSeconds / 60.0) + " min"; }
        public String getStorageLabel() {
            double size = storageBytes;
            String[] units = {"B", "KB", "MB", "GB"};
            int unit = 0;
            while (size >= 1024 && unit < units.length - 1) { size /= 1024.0; unit++; }
            return new java.text.DecimalFormat("#,##0.0").format(size) + " " + units[unit];
        }
    }
}
