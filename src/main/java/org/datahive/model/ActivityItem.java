package org.datahive.model;

public final class ActivityItem {
    private final String type;
    private final String title;
    private final String detail;
    private final String createdAt;
    public ActivityItem(String type, String title, String detail, String createdAt) {
        this.type = type; this.title = title; this.detail = detail; this.createdAt = createdAt;
    }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getDetail() { return detail; }
    public String getCreatedAt() { return createdAt; }
}
