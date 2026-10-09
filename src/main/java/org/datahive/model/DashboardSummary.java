package org.datahive.model;

import java.util.List;

public final class DashboardSummary {
    private final List<MetricCard> metrics;
    private final long projectCount;
    private final List<ActivityItem> recentActivity;

    public DashboardSummary(List<MetricCard> metrics, long projectCount, List<ActivityItem> recentActivity) {
        this.metrics = List.copyOf(metrics);
        this.projectCount = projectCount;
        this.recentActivity = recentActivity == null ? List.of() : List.copyOf(recentActivity);
    }

    public List<MetricCard> getMetrics() { return metrics; }
    public long getProjectCount() { return projectCount; }
    public List<ActivityItem> getRecentActivity() { return recentActivity; }
}
