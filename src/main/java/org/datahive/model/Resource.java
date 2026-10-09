package org.datahive.model;

public final class Resource {
    private final long id;
    private final String name;
    private final String type;
    private final double capacity;
    private final String unit;
    private final String status;
    private final String description;

    public Resource(long id, String name, String type, double capacity, String unit, String status, String description) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.capacity = capacity;
        this.unit = unit;
        this.status = status;
        this.description = description;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public double getCapacity() { return capacity; }
    public String getUnit() { return unit; }
    public String getStatus() { return status; }
    public String getStatusLabel() { return status == null ? "unknown" : status.toLowerCase(java.util.Locale.ROOT).replace('_', ' '); }
    public String getDescription() { return description; }
    public String getCapacityLabel() { return new java.text.DecimalFormat("#,##0.##").format(capacity) + " " + unit; }
}
