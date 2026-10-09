package org.datahive.dao;

import org.datahive.config.Database;
import org.datahive.model.AdminActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class ActivityLogDao {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    public void record(Long actorId, String action, String targetType,
                       Long targetId, String summary) throws SQLException {
        String sql = "INSERT INTO activity_logs (actor_id, action, target_type, target_id, summary) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (actorId == null) statement.setNull(1, java.sql.Types.BIGINT);
            else statement.setLong(1, actorId);
            statement.setString(2, action);
            statement.setString(3, targetType);
            if (targetId == null) statement.setNull(4, java.sql.Types.BIGINT);
            else statement.setLong(4, targetId);
            statement.setString(5, summary.length() <= 500 ? summary : summary.substring(0, 500));
            statement.executeUpdate();
        }
    }

    public List<AdminActivity> latest(int limit) throws SQLException {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String sql = "SELECT a.action, a.target_type, a.target_id, a.summary, " +
                "COALESCE(u.full_name, 'Former account') AS actor_name, a.created_at " +
                "FROM activity_logs a LEFT JOIN users u ON u.id = a.actor_id " +
                "ORDER BY a.created_at DESC, a.id DESC FETCH FIRST " + safeLimit + " ROWS ONLY";
        List<AdminActivity> activities = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    long id = result.getLong("target_id");
                    Long targetId = result.wasNull() ? null : id;
                    Timestamp createdAt = result.getTimestamp("created_at");
                    activities.add(new AdminActivity(result.getString("actor_name"),
                            result.getString("action"), result.getString("target_type"),
                            targetId, result.getString("summary"),
                            createdAt.toLocalDateTime().format(DISPLAY_TIME)));
                }
            }
        }
        return activities;
    }
}
