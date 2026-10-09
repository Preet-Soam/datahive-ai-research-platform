package org.datahive.dao;

import org.datahive.config.Database;
import org.datahive.security.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class ProfileDao {
    public boolean update(long userId, String name, String email, String currentPassword, String newPassword)
            throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String passwordHash;
                try (PreparedStatement statement = connection.prepareStatement("SELECT password_hash FROM users WHERE id = ? AND active = TRUE")) {
                    statement.setLong(1, userId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) { connection.rollback(); return false; }
                        passwordHash = result.getString(1);
                    }
                }
                if (newPassword != null && !newPassword.isBlank()) {
                    if (!PasswordHasher.verify(currentPassword, passwordHash)) {
                        connection.rollback(); return false;
                    }
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE users SET full_name = ?, email = ?, password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                        statement.setString(1, name); statement.setString(2, email.toLowerCase(java.util.Locale.ROOT));
                        statement.setString(3, PasswordHasher.hash(newPassword)); statement.setLong(4, userId);
                        statement.executeUpdate();
                    }
                } else {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE users SET full_name = ?, email = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                        statement.setString(1, name); statement.setString(2, email.toLowerCase(java.util.Locale.ROOT));
                        statement.setLong(3, userId); statement.executeUpdate();
                    }
                }
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback(); throw exception;
            } finally { connection.setAutoCommit(true); }
        }
    }
}
