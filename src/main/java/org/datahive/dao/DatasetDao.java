package org.datahive.dao;

import org.datahive.config.Database;
import org.datahive.model.ColumnProfile;
import org.datahive.model.Dataset;
import org.datahive.model.Role;
import org.datahive.model.User;
import org.datahive.service.CsvProfiler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DatasetDao {
    private static final String DATASET_COLUMNS = "d.id, d.project_id, p.title AS project_title, d.name, " +
            "d.description, d.original_filename, d.stored_filename, d.file_size_bytes, d.row_count, " +
            "d.column_count, u.full_name AS uploaded_by_name, d.uploaded_by AS uploaded_by_id ";

    public List<Dataset> findVisibleTo(User user) throws SQLException {
        String sql = "SELECT " + DATASET_COLUMNS + "FROM datasets d " +
                "JOIN projects p ON p.id = d.project_id JOIN users u ON u.id = d.uploaded_by " +
                (user.getRole() == Role.ADMIN ? "ORDER BY d.created_at DESC" :
                        "JOIN project_members pm ON pm.project_id = d.project_id WHERE pm.user_id = ? " +
                                "ORDER BY d.created_at DESC");
        List<Dataset> datasets = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (user.getRole() != Role.ADMIN) statement.setLong(1, user.getId());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    long datasetId = result.getLong("id");
                    datasets.add(map(result, loadColumns(connection, datasetId)));
                }
            }
        }
        return datasets;
    }

    public Optional<Dataset> findVisibleById(long datasetId, User user) throws SQLException {
        String sql = "SELECT " + DATASET_COLUMNS + "FROM datasets d " +
                "JOIN projects p ON p.id = d.project_id JOIN users u ON u.id = d.uploaded_by " +
                (user.getRole() == Role.ADMIN ? "WHERE d.id = ?" :
                        "JOIN project_members pm ON pm.project_id = d.project_id WHERE d.id = ? AND pm.user_id = ?");
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, datasetId);
            if (user.getRole() != Role.ADMIN) statement.setLong(2, user.getId());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                Dataset base = map(result, loadColumns(connection, datasetId));
                return Optional.of(base);
            }
        }
    }

    public long create(long projectId, String name, String description, String originalFilename,
                       String storedFilename, long fileSize, long uploadedBy, CsvProfiler.Profile profile)
            throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long datasetId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO datasets (project_id, name, description, original_filename, stored_filename, " +
                                "file_size_bytes, row_count, column_count, uploaded_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    statement.setLong(1, projectId);
                    statement.setString(2, name);
                    statement.setString(3, description);
                    statement.setString(4, originalFilename);
                    statement.setString(5, storedFilename);
                    statement.setLong(6, fileSize);
                    statement.setLong(7, profile.rowCount());
                    statement.setInt(8, profile.columnCount());
                    statement.setLong(9, uploadedBy);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Dataset ID was not returned");
                        datasetId = keys.getLong(1);
                    }
                }

                String insertColumn = "INSERT INTO dataset_columns (dataset_id, column_name, inferred_type, " +
                        "missing_count, distinct_count, sample_values) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(insertColumn)) {
                    for (ColumnProfile column : profile.columns()) {
                        statement.setLong(1, datasetId);
                        statement.setString(2, column.getName());
                        statement.setString(3, column.getInferredType());
                        statement.setLong(4, column.getMissingCount());
                        statement.setLong(5, column.getDistinctCount());
                        statement.setString(6, column.getSampleValues());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO usage_records (resource_id, project_id, user_id, usage_type, amount) " +
                                "VALUES ((SELECT MIN(id) FROM resources WHERE resource_type = 'STORAGE' " +
                                "AND status <> 'OFFLINE'), ?, ?, 'STORAGE_BYTES', ?)")) {
                    statement.setLong(1, projectId);
                    statement.setLong(2, uploadedBy);
                    statement.setLong(3, fileSize);
                    statement.executeUpdate();
                }
                connection.commit();
                return datasetId;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public Optional<String> delete(long datasetId, User user) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String select = "SELECT d.stored_filename FROM datasets d " +
                        (user.getRole() == Role.ADMIN ? "WHERE d.id = ?" :
                                "JOIN project_members pm ON pm.project_id = d.project_id " +
                                        "WHERE d.id = ? AND pm.user_id = ? AND d.uploaded_by = ?");
                String storedFilename;
                try (PreparedStatement statement = connection.prepareStatement(select)) {
                    statement.setLong(1, datasetId);
                    if (user.getRole() != Role.ADMIN) {
                        statement.setLong(2, user.getId());
                        statement.setLong(3, user.getId());
                    }
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            connection.rollback();
                            return Optional.empty();
                        }
                        storedFilename = result.getString(1);
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM datasets WHERE id = ?")) {
                    statement.setLong(1, datasetId);
                    statement.executeUpdate();
                }
                connection.commit();
                return Optional.of(storedFilename);
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean updateMetadata(long datasetId, String name, String description, User user)
            throws SQLException {
        String sql = "UPDATE datasets SET name = ?, description = ? WHERE id = ?" +
                (user.getRole() == Role.ADMIN ? "" :
                        " AND uploaded_by = ? AND EXISTS (SELECT 1 FROM project_members pm " +
                                "WHERE pm.project_id = datasets.project_id AND pm.user_id = ?)");
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, description);
            statement.setLong(3, datasetId);
            if (user.getRole() != Role.ADMIN) {
                statement.setLong(4, user.getId());
                statement.setLong(5, user.getId());
            }
            return statement.executeUpdate() == 1;
        }
    }

    private static List<ColumnProfile> loadColumns(Connection connection, long datasetId) throws SQLException {
        List<ColumnProfile> columns = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT column_name, inferred_type, missing_count, distinct_count, sample_values " +
                        "FROM dataset_columns WHERE dataset_id = ? ORDER BY id")) {
            statement.setLong(1, datasetId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    columns.add(new ColumnProfile(result.getString(1), result.getString(2),
                            result.getLong(3), result.getLong(4), result.getString(5)));
                }
            }
        }
        return columns;
    }

    private static Dataset map(ResultSet result, List<ColumnProfile> columns) throws SQLException {
        return new Dataset(result.getLong("id"), result.getLong("project_id"), result.getString("project_title"),
                result.getString("name"), result.getString("description"), result.getString("original_filename"),
                result.getString("stored_filename"), result.getLong("file_size_bytes"), result.getLong("row_count"),
                result.getInt("column_count"), result.getString("uploaded_by_name"),
                result.getLong("uploaded_by_id"), columns);
    }
}
