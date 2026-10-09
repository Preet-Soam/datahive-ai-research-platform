package org.datahive.model;

public final class UsageBucket {
    private final String label;
    private final double computeSeconds;
    private final double storageBytes;
    private final int computeBar;
    private final int storageBar;

    public UsageBucket(String label, double computeSeconds, double storageBytes) {
        this(label, computeSeconds, storageBytes, 0, 0);
    }

    private UsageBucket(String label, double computeSeconds, double storageBytes, int computeBar, int storageBar) {
        this.label = label;
        this.computeSeconds = computeSeconds;
        this.storageBytes = storageBytes;
        this.computeBar = computeBar;
        this.storageBar = storageBar;
    }

    public String getLabel() { return label; }
    public double getComputeSeconds() { return computeSeconds; }
    public double getStorageBytes() { return storageBytes; }
    public int getComputeBar() { return computeBar; }
    public int getStorageBar() { return storageBar; }
    public String getComputeLabel() { return new java.text.DecimalFormat("#,##0.0").format(computeSeconds) + " sec"; }
    public String getStorageLabel() {
        double size = storageBytes;
        String[] units = {"B", "KB", "MB", "GB"};
        int unit = 0;
        while (size >= 1024 && unit < units.length - 1) { size /= 1024.0; unit++; }
        return new java.text.DecimalFormat("#,##0.0").format(size) + " " + units[unit];
    }
    public UsageBucket withBars(int computeBar, int storageBar) {
        return new UsageBucket(label, computeSeconds, storageBytes, computeBar, storageBar);
    }
}
