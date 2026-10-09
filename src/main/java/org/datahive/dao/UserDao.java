package org.datahive.dao;

import org.datahive.config.Database;
import org.datahive.model.Role;
import org.datahive.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class UserDao {
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
