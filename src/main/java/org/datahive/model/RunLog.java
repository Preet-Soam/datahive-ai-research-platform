package org.datahive.model;

public final class RunLog {
    private final String level;
    private final String message;
    private final String createdAt;
    public RunLog(String level, String message, String createdAt) {
        this.level = level; this.message = message; this.createdAt = createdAt;
    }
    public String getLevel() { return level; }
    public String getLevelLabel() { return level == null ? "info" : level.toLowerCase(java.util.Locale.ROOT); }
    public String getMessage() { return message; }
    public String getCreatedAt() { return createdAt; }
}
