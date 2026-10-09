package org.datahive.model;

public final class AdminActivity {
    private final String actorName;
    private final String action;
    private final String targetType;
    private final Long targetId;
    private final String summary;
    private final String createdAt;

    public AdminActivity(String actorName, String action, String targetType,
                         Long targetId, String summary, String createdAt) {
        this.actorName = actorName;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.summary = summary;
        this.createdAt = createdAt;
    }

    public String getActorName() { return actorName; }
    public String getAction() { return action; }
    public String getActionLabel() {
        if (action == null || action.isBlank()) return "Activity";
        String label = action.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getSummary() { return summary; }
    public String getCreatedAt() { return createdAt; }
}
