package org.datahive.model;

import java.util.List;

public final class Dataset {
    private final long id;
    private final long projectId;
    private final String projectTitle;
    private final String name;
    private final String description;
    private final String originalFilename;
    private final String storedFilename;
    private final long fileSizeBytes;
    private final long rowCount;
    private final int columnCount;
    private final String uploadedByName;
    private final long uploadedById;
    private final List<ColumnProfile> columns;

    public Dataset(long id, long projectId, String projectTitle, String name, String description,
                   String originalFilename, String storedFilename, long fileSizeBytes, long rowCount,
                   int columnCount, String uploadedByName, long uploadedById, List<ColumnProfile> columns) {
        this.id = id;
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.name = name;
        this.description = description;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.fileSizeBytes = fileSizeBytes;
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.uploadedByName = uploadedByName;
        this.uploadedById = uploadedById;
        this.columns = columns == null ? List.of() : List.copyOf(columns);
    }

    public long getId() { return id; }
    public long getProjectId() { return projectId; }
    public String getProjectTitle() { return projectTitle; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStoredFilename() { return storedFilename; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public long getRowCount() { return rowCount; }
    public int getColumnCount() { return columnCount; }
    public String getUploadedByName() { return uploadedByName; }
    public long getUploadedById() { return uploadedById; }
    public List<ColumnProfile> getColumns() { return columns; }
    public String getFormattedSize() {
        if (fileSizeBytes < 1024) return fileSizeBytes + " B";
        double size = fileSizeBytes;
        String[] units = {"KB", "MB", "GB"};
        int unit = -1;
        do { size /= 1024.0; unit++; } while (size >= 1024 && unit < units.length - 1);
        return new java.text.DecimalFormat("#,##0.0").format(size) + " " + units[unit];
    }
}
