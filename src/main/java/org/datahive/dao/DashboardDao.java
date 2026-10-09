package org.datahive.dao;

import org.datahive.config.Database;
import org.datahive.model.DashboardSummary;
import org.datahive.model.ActivityItem;
import org.datahive.model.MetricCard;
import org.datahive.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;

public final class DashboardDao {
    public DashboardSummary load(Role role, long userId) throws SQLException {
        return role == Role.ADMIN ? loadAdmin() : loadResearcher(userId);
    }

    private DashboardSummary loadAdmin() throws SQLException {
        try (Connection connection = Database.getConnection()) {
            long activeUsers = scalar(connection, "SELECT COUNT(*) FROM users WHERE active = TRUE");
            long projects = scalar(connection, "SELECT COUNT(*) FROM projects WHERE status = 'ACTIVE'");
            long resources = scalar(connection, "SELECT COUNT(*) FROM resources WHERE status <> 'OFFLINE'");
            long storageBytes = scalar(connection, "SELECT COALESCE(SUM(file_size_bytes), 0) FROM datasets");
            List<MetricCard> cards = List.of(
                    new MetricCard("Active users", Long.toString(activeUsers), "Accounts with access", "U"),
                    new MetricCard("Active projects", Long.toString(projects), "Research workspaces", "P"),
                    new MetricCard("Available resources", Long.toString(resources), "Compute and storage", "R"),
                    new MetricCard("Dataset files", formatBytes(storageBytes), "Current CSV file sizes", "D")
            );
            return new DashboardSummary(cards, projects, recentActivity(true, 0));
        }
    }

    private DashboardSummary loadResearcher(long userId) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            long projects = scalar(connection,
                    "SELECT COUNT(*) FROM project_members pm JOIN projects p ON p.id = pm.project_id " +
                            "WHERE pm.user_id = ? AND p.status = 'ACTIVE'", userId);
            long datasets = scalar(connection,
                    "SELECT COUNT(*) FROM datasets d JOIN project_members pm ON pm.project_id = d.project_id " +
                            "WHERE pm.user_id = ?", userId);
            long activeRuns = scalar(connection,
                    "SELECT COUNT(*) FROM training_runs tr JOIN experiments e ON e.id = tr.experiment_id " +
                            "JOIN project_members pm ON pm.project_id = e.project_id " +
                            "WHERE pm.user_id = ? AND tr.status IN ('QUEUED', 'RUNNING')", userId);
            long experiments = scalar(connection,
                    "SELECT COUNT(*) FROM experiments e JOIN project_members pm ON pm.project_id = e.project_id " +
                            "WHERE pm.user_id = ?", userId);
            List<MetricCard> cards = List.of(
                    new MetricCard("My projects", Long.toString(projects), "Active workspaces", "P"),
                    new MetricCard("Datasets", Long.toString(datasets), "Available to your projects", "D"),
                    new MetricCard("Training jobs", Long.toString(activeRuns), "Queued or running", "T"),
                    new MetricCard("Experiments", Long.toString(experiments), "Saved research runs", "E")
            );
            return new DashboardSummary(cards, projects, recentActivity(false, userId));
        }
    }

    private static long scalar(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getLong(1) : 0;
        }
    }

    private static long scalar(Connection connection, String sql, long parameter) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, parameter);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : 0;
            }
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KB", "MB", "GB", "TB"};
        int unit = -1;
        do {
            value /= 1024.0;
            unit++;
        } while (value >= 1024 && unit < units.length - 1);
        return new DecimalFormat("#,##0.0").format(value) + " " + units[unit];
    }

    private static List<ActivityItem> recentActivity(boolean admin, long userId) throws SQLException {
        String projectMembership = admin ? "" : " JOIN project_members pm ON pm.project_id = p.id ";
        String datasetMembership = admin ? "" : " JOIN project_members pm ON pm.project_id = p.id ";
        String runMembership = admin ? "" : " JOIN project_members pm ON pm.project_id = e.project_id ";
        String experimentMembership = admin ? "" : " JOIN project_members pm ON pm.project_id = e.project_id ";
        String projectFilter = admin ? "" : " AND pm.user_id = ? ";
        String datasetFilter = admin ? "" : " AND pm.user_id = ? ";
        String runFilter = admin ? "" : " AND pm.user_id = ? ";
        String experimentFilter = admin ? "" : " AND pm.user_id = ? ";
        String sql = "SELECT activity_type, title, detail, activity_at FROM (" +
                "SELECT 'Project created' AS activity_type, p.title AS title, 'Research workspace' AS detail, p.created_at AS activity_at " +
                "FROM projects p " + projectMembership + "WHERE p.status = 'ACTIVE' " + projectFilter + "UNION ALL " +
                "SELECT 'Dataset uploaded', d.name, p.title, d.created_at FROM datasets d JOIN projects p ON p.id = d.project_id" +
                " " + datasetMembership + "WHERE 1 = 1 " + datasetFilter + "UNION ALL " +
                "SELECT CASE tr.status WHEN 'COMPLETED' THEN 'Training completed' WHEN 'FAILED' THEN 'Training failed' " +
                "WHEN 'RUNNING' THEN 'Training running' ELSE 'Training queued' END, e.name, p.title, tr.created_at " +
                "FROM training_runs tr JOIN experiments e ON e.id = tr.experiment_id JOIN projects p ON p.id = e.project_id" +
                " " + runMembership + "WHERE 1 = 1 " + runFilter + "UNION ALL " +
                "SELECT 'Experiment created', e.name, p.title, e.created_at FROM experiments e JOIN projects p ON p.id = e.project_id" +
                " " + experimentMembership + "WHERE 1 = 1 " + experimentFilter +
                ") events ORDER BY activity_at DESC FETCH FIRST 5 ROWS ONLY";
        List<ActivityItem> activities = new java.util.ArrayList<>();
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (!admin) for (int index = 1; index <= 4; index++) statement.setLong(index, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) activities.add(new ActivityItem(result.getString("activity_type"),
                        result.getString("title"), result.getString("detail"), result.getString("activity_at")));
            }
        }
        return activities;
    }
}
