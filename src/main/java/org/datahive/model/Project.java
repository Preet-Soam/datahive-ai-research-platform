package org.datahive.model;

public final class Project {
    private final long id;
    private final long ownerId;
    private final String title;
    private final String description;
    private final String status;
    private final String ownerName;
    private final long datasetCount;
    private final long experimentCount;
    private final long memberCount;

    public Project(long id, long ownerId, String title, String description, String status,
                   String ownerName, long datasetCount, long experimentCount, long memberCount) {
        this.id = id;
        this.ownerId = ownerId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.ownerName = ownerName;
        this.datasetCount = datasetCount;
        this.experimentCount = experimentCount;
        this.memberCount = memberCount;
    }

    public long getId() { return id; }
    public long getOwnerId() { return ownerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getOwnerName() { return ownerName; }
    public long getDatasetCount() { return datasetCount; }
    public long getExperimentCount() { return experimentCount; }
    public long getMemberCount() { return memberCount; }
    public String getInitial() { return title == null || title.isBlank() ? "P" : title.substring(0, 1).toUpperCase(java.util.Locale.ROOT); }
    public String getStatusLabel() { return status == null ? "unknown" : status.toLowerCase(java.util.Locale.ROOT); }
}
