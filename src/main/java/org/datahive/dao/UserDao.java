package org.datahive.dao;

import org.datahive.config.Database;
import org.datahive.model.Role;
import org.datahive.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import org.datahive.security.PasswordHasher;

public final class UserDao {
    public User createAccount(String name, String email, String password, Role role) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?)")) {
                    statement.setString(1, email);
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) throw new SQLException("An account with this email already exists", "23505");
                    }
                }
                long userId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO users (full_name, email, password_hash, role) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, name);
                    statement.setString(2, email.toLowerCase(java.util.Locale.ROOT));
                    statement.setString(3, PasswordHasher.hash(password));
                    statement.setString(4, role.name());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Account ID was not returned");
                        userId = keys.getLong(1);
                    }
                }

                if (role == Role.RESEARCHER) {
                    long projectId;
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO projects (title, description, owner_id) VALUES (?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS)) {
                        statement.setString(1, name + "'s Research Lab");
                        statement.setString(2, "Personal workspace created with your DataHive account.");
                        statement.setLong(3, userId);
                        statement.executeUpdate();
                        try (ResultSet keys = statement.getGeneratedKeys()) {
                            if (!keys.next()) throw new SQLException("Workspace ID was not returned");
                            projectId = keys.getLong(1);
                        }
                    }
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO project_members (project_id, user_id, member_role) VALUES (?, ?, 'OWNER')")) {
                        statement.setLong(1, projectId);
                        statement.setLong(2, userId);
                        statement.executeUpdate();
                    }
                }
                connection.commit();
                return new User(userId, name, email.toLowerCase(java.util.Locale.ROOT), role, true);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public Optional<AuthenticatedUser> findForLogin(String email) throws SQLException {
        String sql = "SELECT id, full_name, email, password_hash, role, active FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                User user = new User(
                        result.getLong("id"),
                        result.getString("full_name"),
                        result.getString("email"),
                        Role.fromDatabase(result.getString("role")),
                        result.getBoolean("active")
                );
                return Optional.of(new AuthenticatedUser(user, result.getString("password_hash")));
            }
        }
    }

    public record AuthenticatedUser(User user, String passwordHash) {
    }
}
