package org.datahive.model;

public final class ColumnProfile {
    private final String name;
    private final String inferredType;
    private final long missingCount;
    private final long distinctCount;
    private final String sampleValues;

    public ColumnProfile(String name, String inferredType, long missingCount, long distinctCount, String sampleValues) {
        this.name = name;
        this.inferredType = inferredType;
        this.missingCount = missingCount;
        this.distinctCount = distinctCount;
        this.sampleValues = sampleValues;
    }

    public String getName() { return name; }
    public String getInferredType() { return inferredType; }
    public long getMissingCount() { return missingCount; }
    public long getDistinctCount() { return distinctCount; }
    public String getSampleValues() { return sampleValues; }
}
