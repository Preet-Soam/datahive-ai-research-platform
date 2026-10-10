package org.datahive.model;

import java.util.List;

public final class TrainingExperiment {
    private final long id;
    private final long runId;
    private final long projectId;
    private final String projectTitle;
    private final String datasetName;
    private final String name;
    private final String modelName;
    private final String targetColumn;
    private final int epochs;
    private final double learningRate;
    private final String status;
    private final int progress;
    private final String createdBy;
    private final long createdById;
    private final double accuracy;
    private final double precision;
    private final double recall;
    private final double f1;
    private final String createdAt;
    private final String remoteJobUrl;
    private final TrainingEvaluation evaluation;
    private final List<RunLog> logs;

    public TrainingExperiment(long id, long runId, long projectId, String projectTitle, String datasetName,
                              String name, String modelName, String targetColumn, int epochs, double learningRate,
                              String status, int progress,
                              String createdBy, long createdById, double accuracy, double precision, double recall, double f1,
                              String createdAt, String remoteJobUrl, TrainingEvaluation evaluation, List<RunLog> logs) {
        this.id = id; this.runId = runId; this.projectId = projectId; this.projectTitle = projectTitle;
        this.datasetName = datasetName; this.name = name; this.modelName = modelName;
        this.targetColumn = targetColumn; this.epochs = epochs; this.learningRate = learningRate;
        this.status = status; this.progress = progress;
        this.createdBy = createdBy; this.createdById = createdById; this.accuracy = accuracy; this.precision = precision;
        this.recall = recall; this.f1 = f1; this.createdAt = createdAt; this.remoteJobUrl = remoteJobUrl;
        this.evaluation = evaluation;
        this.logs = logs == null ? List.of() : List.copyOf(logs);
    }
    public long getId() { return id; }
    public long getRunId() { return runId; }
    public long getProjectId() { return projectId; }
    public String getProjectTitle() { return projectTitle; }
    public String getDatasetName() { return datasetName; }
    public String getName() { return name; }
    public String getModelName() { return modelName; }
    public String getTargetColumn() { return targetColumn; }
    public int getEpochs() { return epochs; }
    public double getLearningRate() { return learningRate; }
    public String getLearningRateLabel() { return new java.text.DecimalFormat("0.00").format(learningRate); }
    public String getStatus() { return status; }
    public String getStatusLabel() { return status == null ? "unknown" : status.toLowerCase(java.util.Locale.ROOT); }
    public int getProgress() { return progress; }
    public boolean isLive() { return "QUEUED".equals(status) || "RUNNING".equals(status); }
    public String getCreatedBy() { return createdBy; }
    public long getCreatedById() { return createdById; }
    public double getAccuracy() { return accuracy; }
    public double getPrecision() { return precision; }
    public double getRecall() { return recall; }
    public double getF1() { return f1; }
    public String getCreatedAt() { return createdAt; }
    public String getRemoteJobUrl() { return remoteJobUrl; }
    public TrainingEvaluation getEvaluation() { return evaluation; }
    public List<RunLog> getLogs() { return logs; }
    public String getAccuracyPercent() { return metric(accuracy); }
    public String getPrecisionPercent() { return metric(precision); }
    public String getRecallPercent() { return metric(recall); }
    public String getF1Percent() { return metric(f1); }
    private static String metric(double value) {
        return Double.isNaN(value) ? "—" : new java.text.DecimalFormat("0.0%").format(value);
    }
}
