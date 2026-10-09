package org.datahive.dao;

import org.datahive.config.Database;
import org.datahive.model.Role;
import org.datahive.model.RunLog;
import org.datahive.model.TrainingExperiment;
import org.datahive.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class ExperimentDao {
    public void failInterruptedRuns() throws SQLException {
        List<Long> interrupted = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id FROM training_runs WHERE status IN ('QUEUED', 'RUNNING')");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) interrupted.add(result.getLong(1));
        }
        for (long runId : interrupted) fail(runId,
                "The web application restarted before this local run finished. Start a new run to continue.");
    }

    public TrainingTicket createAndQueue(long projectId, long datasetId, String name, String targetColumn,
                                         List<String> features, int epochs, double learningRate, long userId) throws SQLException {
        String sql = "INSERT INTO experiments (project_id, dataset_id, name, model_name, target_column, epochs, " +
                "learning_rate, parameters_json, created_by) VALUES (?, ?, ?, 'Binary logistic regression', ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long experimentId;
                try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setLong(1, projectId);
                    statement.setLong(2, datasetId);
                    statement.setString(3, name);
                    statement.setString(4, targetColumn);
                    statement.setInt(5, epochs);
                    statement.setDouble(6, learningRate);
                    statement.setString(7, parametersJson(features, epochs, learningRate));
                    statement.setLong(8, userId);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Experiment ID was not returned");
                        experimentId = keys.getLong(1);
                    }
                }
                long runId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO training_runs (experiment_id, random_seed) VALUES (?, 42)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    statement.setLong(1, experimentId);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Training run ID was not returned");
                        runId = keys.getLong(1);
                    }
                }
                addLog(connection, runId, "INFO", "Queued binary logistic regression with an 80/20 stratified holdout and seed 42.");
                connection.commit();
                return new TrainingTicket(experimentId, runId);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<TrainingExperiment> listVisible(User user) throws SQLException {
        String sql = selectFrom(user.getRole() == Role.ADMIN) + visibleWhere(user.getRole() == Role.ADMIN) +
                groupBy() + " ORDER BY tr.created_at DESC";
        List<TrainingExperiment> experiments = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (user.getRole() != Role.ADMIN) statement.setLong(1, user.getId());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) experiments.add(map(result, List.of()));
            }
        }
        return experiments;
    }

    public TrainingExperiment findVisible(long id, User user) throws SQLException {
        String sql = selectFrom(user.getRole() == Role.ADMIN) + visibleWhere(user.getRole() == Role.ADMIN) +
                (user.getRole() == Role.ADMIN ? " WHERE e.id = ? " : " AND e.id = ? ") + groupBy();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (user.getRole() == Role.ADMIN) statement.setLong(1, id);
            else { statement.setLong(1, user.getId()); statement.setLong(2, id); }
            long runId;
            TrainingExperiment base;
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                runId = result.getLong("run_id");
                base = map(result, List.of());
            }
            return new TrainingExperiment(base.getId(), base.getRunId(), base.getProjectId(), base.getProjectTitle(),
                    base.getDatasetName(), base.getName(), base.getModelName(), base.getTargetColumn(),
                    base.getEpochs(), base.getLearningRate(),
                    base.getStatus(), base.getProgress(), base.getCreatedBy(), base.getAccuracy(),
                    base.getPrecision(), base.getRecall(), base.getF1(), base.getCreatedAt(), loadLogs(connection, runId));
        }
    }

    public boolean startRun(long runId) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int changed;
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE training_runs SET status = 'RUNNING', started_at = CURRENT_TIMESTAMP " +
                                "WHERE id = ? AND status = 'QUEUED'")) {
                    statement.setLong(1, runId);
                    changed = statement.executeUpdate();
                }
                if (changed == 1) addLog(connection, runId, "INFO", "Training started. Preparing numeric features and holdout data.");
                connection.commit();
                return changed == 1;
            } catch (SQLException exception) {
                connection.rollback(); throw exception;
            } finally { connection.setAutoCommit(true); }
        }
    }

    public void updateProgress(long runId, int progress, String message) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE training_runs SET progress_percent = ? WHERE id = ? AND status = 'RUNNING'")) {
                    statement.setInt(1, Math.max(0, Math.min(99, progress)));
                    statement.setLong(2, runId);
                    statement.executeUpdate();
                }
                addLog(connection, runId, "INFO", message);
                connection.commit();
            } catch (SQLException exception) { connection.rollback(); throw exception; }
            finally { connection.setAutoCommit(true); }
        }
    }

    public void complete(long runId, long projectId, long userId, double elapsedSeconds,
                         double accuracy, double precision, double recall, double f1,
                         String positiveClass, int trainRows, int testRows) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE training_runs SET status = 'COMPLETED', progress_percent = 100, " +
                                "finished_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'RUNNING'")) {
                    statement.setLong(1, runId);
                    if (statement.executeUpdate() != 1) throw new SQLException("Training run is no longer active");
                }
                String metricSql = "INSERT INTO run_metrics (training_run_id, metric_name, metric_value) VALUES (?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(metricSql)) {
                    addMetric(statement, runId, "accuracy", accuracy);
                    addMetric(statement, runId, "precision", precision);
                    addMetric(statement, runId, "recall", recall);
                    addMetric(statement, runId, "f1", f1);
                    statement.executeBatch();
                }
                addLog(connection, runId, "INFO", "Holdout results: accuracy " + percent(accuracy) + ", F1 " + percent(f1) +
                        ". Positive class: '" + safeLog(positiveClass) + "'. Train rows: " + trainRows + "; test rows: " + testRows + ".");
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO usage_records (resource_id, project_id, user_id, usage_type, amount) " +
                                "VALUES ((SELECT MIN(id) FROM resources WHERE resource_type = 'COMPUTE' " +
                                "AND status <> 'OFFLINE'), ?, ?, 'COMPUTE_SECONDS', ?)")) {
                    statement.setLong(1, projectId);
                    statement.setLong(2, userId);
                    statement.setDouble(3, Math.max(0.01, elapsedSeconds));
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException exception) { connection.rollback(); throw exception; }
            finally { connection.setAutoCommit(true); }
        }
    }

    public void fail(long runId, String message) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE training_runs SET status = 'FAILED', finished_at = CURRENT_TIMESTAMP, error_message = ? " +
                                "WHERE id = ? AND status IN ('QUEUED', 'RUNNING')")) {
                    String safe = safeLog(message);
                    statement.setString(1, safe.length() > 2000 ? safe.substring(0, 2000) : safe);
                    statement.setLong(2, runId);
                    statement.executeUpdate();
                }
                addLog(connection, runId, "ERROR", "Training failed: " + safeLog(message));
                connection.commit();
            } catch (SQLException exception) { connection.rollback(); throw exception; }
            finally { connection.setAutoCommit(true); }
        }
    }

    private static String selectFrom(boolean admin) {
        return "SELECT e.id, tr.id AS run_id, e.project_id, p.title AS project_title, " +
                "COALESCE(d.name, '(dataset removed)') AS dataset_name, e.name, e.model_name, e.target_column, " +
                "e.epochs, e.learning_rate, " +
                "tr.status, tr.progress_percent, u.full_name AS created_by, " +
                "MAX(CASE WHEN rm.metric_name = 'accuracy' THEN rm.metric_value END) AS accuracy, " +
                "MAX(CASE WHEN rm.metric_name = 'precision' THEN rm.metric_value END) AS metric_precision, " +
                "MAX(CASE WHEN rm.metric_name = 'recall' THEN rm.metric_value END) AS recall, " +
                "MAX(CASE WHEN rm.metric_name = 'f1' THEN rm.metric_value END) AS f1, tr.created_at " +
                "FROM training_runs tr JOIN experiments e ON e.id = tr.experiment_id " +
                "JOIN projects p ON p.id = e.project_id LEFT JOIN datasets d ON d.id = e.dataset_id " +
                "JOIN users u ON u.id = e.created_by LEFT JOIN run_metrics rm ON rm.training_run_id = tr.id " +
                (admin ? "" : "JOIN project_members pm ON pm.project_id = e.project_id ");
    }

    private static String visibleWhere(boolean admin) { return admin ? "" : "WHERE pm.user_id = ? "; }
    private static String groupBy() {
        return "GROUP BY e.id, tr.id, e.project_id, p.title, d.name, e.name, e.model_name, e.target_column, e.epochs, e.learning_rate, " +
                "tr.status, tr.progress_percent, u.full_name, tr.created_at ";
    }

    private static List<RunLog> loadLogs(Connection connection, long runId) throws SQLException {
        List<RunLog> logs = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT level, message, created_at FROM run_logs WHERE training_run_id = ? ORDER BY created_at, id")) {
            statement.setLong(1, runId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) logs.add(new RunLog(result.getString(1), result.getString(2), result.getString(3)));
            }
        }
        return logs;
    }

    private static void addLog(Connection connection, long runId, String level, String message) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO run_logs (training_run_id, level, message) VALUES (?, ?, ?)")) {
            statement.setLong(1, runId); statement.setString(2, level);
            statement.setString(3, message.length() > 2000 ? message.substring(0, 2000) : message);
            statement.executeUpdate();
        }
    }

    private static void addMetric(PreparedStatement statement, long runId, String name, double value) throws SQLException {
        statement.setLong(1, runId); statement.setString(2, name); statement.setDouble(3, value); statement.addBatch();
    }
    private static String parametersJson(List<String> features, int epochs, double learningRate) {
        StringBuilder json = new StringBuilder("{\"seed\":42,\"epochs\":" + epochs + ",\"learningRate\":" + learningRate + ",\"split\":\"80/20 stratified\",\"features\":[");
        for (int i = 0; i < features.size(); i++) {
            if (i > 0) json.append(',');
            json.append('"').append(features.get(i).replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        }
        return json.append("]}").toString();
    }
    private static TrainingExperiment map(ResultSet result, List<RunLog> logs) throws SQLException {
        double accuracy = result.getDouble("accuracy"); if (result.wasNull()) accuracy = Double.NaN;
        double precision = result.getDouble("metric_precision"); if (result.wasNull()) precision = Double.NaN;
        double recall = result.getDouble("recall"); if (result.wasNull()) recall = Double.NaN;
        double f1 = result.getDouble("f1"); if (result.wasNull()) f1 = Double.NaN;
        return new TrainingExperiment(result.getLong("id"), result.getLong("run_id"), result.getLong("project_id"),
                result.getString("project_title"), result.getString("dataset_name"), result.getString("name"),
                result.getString("model_name"), result.getString("target_column"), result.getInt("epochs"),
                result.getDouble("learning_rate"), result.getString("status"),
                result.getInt("progress_percent"), result.getString("created_by"), accuracy, precision, recall, f1,
                result.getString("created_at"), logs);
    }
    private static String percent(double value) { return new java.text.DecimalFormat("0.0%").format(value); }
    private static String safeLog(String value) {
        if (value == null || value.isBlank()) return "The CSV could not be used by the training baseline.";
        return value.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", " ").trim();
    }
    public record TrainingTicket(long experimentId, long runId) { }
}
