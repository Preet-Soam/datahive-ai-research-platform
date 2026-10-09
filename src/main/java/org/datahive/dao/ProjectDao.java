package org.datahive.dao;

import org.datahive.config.Database;
import org.datahive.model.Project;
import org.datahive.model.ProjectMember;
import org.datahive.model.Role;
import org.datahive.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ProjectDao {
    private static final String PROJECT_COLUMNS = "p.id, p.owner_id, p.title, p.description, p.status, " +
            "u.full_name AS owner_name, " +
            "(SELECT COUNT(*) FROM datasets d WHERE d.project_id = p.id) AS dataset_count, " +
            "(SELECT COUNT(*) FROM experiments e WHERE e.project_id = p.id) AS experiment_count ";

    public List<Project> findVisibleTo(User user) throws SQLException {
        String sql = "SELECT " + PROJECT_COLUMNS + "FROM projects p JOIN users u ON u.id = p.owner_id " +
                (user.getRole() == Role.ADMIN ? "ORDER BY p.updated_at DESC, p.id DESC" :
                        "JOIN project_members pm ON pm.project_id = p.id WHERE pm.user_id = ? " +
                                "ORDER BY p.updated_at DESC, p.id DESC");
        List<Project> projects = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (user.getRole() != Role.ADMIN) {
                statement.setLong(1, user.getId());
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    projects.add(map(result));
                }
            }
        }
        return projects;
    }

    public Optional<Project> findVisibleById(long id, User user) throws SQLException {
        String sql = "SELECT " + PROJECT_COLUMNS + "FROM projects p JOIN users u ON u.id = p.owner_id " +
                (user.getRole() == Role.ADMIN ? "WHERE p.id = ?" :
                        "JOIN project_members pm ON pm.project_id = p.id WHERE p.id = ? AND pm.user_id = ?");
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            if (user.getRole() != Role.ADMIN) {
                statement.setLong(2, user.getId());
            }
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    public List<User> listActiveResearchers() throws SQLException {
        String sql = "SELECT id, full_name, email, role, active FROM users " +
                "WHERE role = 'RESEARCHER' AND active = TRUE ORDER BY full_name";
        List<User> users = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                users.add(new User(result.getLong("id"), result.getString("full_name"),
                        result.getString("email"), Role.RESEARCHER, result.getBoolean("active")));
            }
        }
        return users;
    }

    public List<ProjectMember> listMembers(long projectId) throws SQLException {
        List<ProjectMember> members = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT u.id, u.full_name, u.email, pm.member_role FROM project_members pm " +
                             "JOIN users u ON u.id = pm.user_id WHERE pm.project_id = ? " +
                             "ORDER BY CASE pm.member_role WHEN 'OWNER' THEN 0 ELSE 1 END, u.full_name")) {
            statement.setLong(1, projectId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) members.add(new ProjectMember(result.getLong("id"), result.getString("full_name"),
                        result.getString("email"), result.getString("member_role")));
            }
        }
        return members;
    }

    public List<User> listAvailableResearchers(long projectId) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.id, u.full_name, u.email FROM users u WHERE u.active = TRUE " +
                "AND u.role = 'RESEARCHER' AND NOT EXISTS (SELECT 1 FROM project_members pm " +
                "WHERE pm.project_id = ? AND pm.user_id = u.id) ORDER BY u.full_name";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) users.add(new User(result.getLong("id"), result.getString("full_name"),
                        result.getString("email"), Role.RESEARCHER, true));
            }
        }
        return users;
    }

    public boolean addResearcher(long projectId, long userId) throws SQLException {
        String sql = "INSERT INTO project_members (project_id, user_id, member_role) " +
                "SELECT p.id, u.id, 'RESEARCHER' FROM projects p, users u WHERE p.id = ? AND p.status = 'ACTIVE' " +
                "AND p.owner_id <> u.id AND u.id = ? AND u.active = TRUE AND u.role = 'RESEARCHER' " +
                "AND NOT EXISTS (SELECT 1 FROM project_members pm WHERE pm.project_id = p.id AND pm.user_id = u.id)";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId); statement.setLong(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean removeResearcher(long projectId, long userId) throws SQLException {
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM project_members WHERE project_id = ? AND user_id = ? AND member_role = 'RESEARCHER' " +
                        "AND EXISTS (SELECT 1 FROM projects p WHERE p.id = ? AND p.status = 'ACTIVE')")) {
            statement.setLong(1, projectId); statement.setLong(2, userId); statement.setLong(3, projectId);
            return statement.executeUpdate() == 1;
        }
    }

    public long create(String title, String description, long ownerId) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long projectId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO projects (title, description, owner_id) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, title);
                    statement.setString(2, description);
                    statement.setLong(3, ownerId);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Project ID was not returned");
                        }
                        projectId = keys.getLong(1);
                    }
                }
                addOwnerMembership(connection, projectId, ownerId);
                connection.commit();
                return projectId;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean update(long projectId, String title, String description, long ownerId, User actor)
            throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String sql = actor.getRole() == Role.ADMIN
                        ? "UPDATE projects SET title = ?, description = ?, owner_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
                        : "UPDATE projects SET title = ?, description = ?, updated_at = CURRENT_TIMESTAMP " +
                                "WHERE id = ? AND owner_id = ?";
                int changed;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, title);
                    statement.setString(2, description);
                    if (actor.getRole() == Role.ADMIN) {
                        statement.setLong(3, ownerId);
                        statement.setLong(4, projectId);
                    } else {
                        statement.setLong(3, projectId);
                        statement.setLong(4, actor.getId());
                    }
                    changed = statement.executeUpdate();
                }
                if (changed == 1 && actor.getRole() == Role.ADMIN) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE project_members SET member_role = 'RESEARCHER' WHERE project_id = ? " +
                                    "AND member_role = 'OWNER' AND user_id <> ?")) {
                        statement.setLong(1, projectId);
                        statement.setLong(2, ownerId);
                        statement.executeUpdate();
                    }
                    addOwnerMembership(connection, projectId, ownerId);
                }
                connection.commit();
                return changed == 1;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean archive(long projectId, User actor) throws SQLException {
        String sql = actor.getRole() == Role.ADMIN
                ? "UPDATE projects SET status = 'ARCHIVED', updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'ACTIVE'"
                : "UPDATE projects SET status = 'ARCHIVED', updated_at = CURRENT_TIMESTAMP WHERE id = ? " +
                        "AND owner_id = ? AND status = 'ACTIVE'";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            if (actor.getRole() != Role.ADMIN) {
                statement.setLong(2, actor.getId());
            }
            return statement.executeUpdate() == 1;
        }
    }

    public boolean isActiveResearcher(long userId) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM users WHERE id = ? AND role = 'RESEARCHER' AND active = TRUE")) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static void addOwnerMembership(Connection connection, long projectId, long ownerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "MERGE INTO project_members (project_id, user_id, member_role) KEY (project_id, user_id) " +
                        "VALUES (?, ?, 'OWNER')")) {
            statement.setLong(1, projectId);
            statement.setLong(2, ownerId);
            statement.executeUpdate();
        }
    }

    private static Project map(ResultSet result) throws SQLException {
        return new Project(result.getLong("id"), result.getLong("owner_id"), result.getString("title"),
                result.getString("description"), result.getString("status"), result.getString("owner_name"),
                result.getLong("dataset_count"), result.getLong("experiment_count"));
    }
}
