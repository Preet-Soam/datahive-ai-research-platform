package org.datahive.model;

public final class MetricCard {
    private final String label;
    private final String value;
    private final String hint;
    private final String icon;

    public MetricCard(String label, String value, String hint, String icon) {
        this.label = label;
        this.value = value;
        this.hint = hint;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public String getValue() { return value; }
    public String getHint() { return hint; }
    public String getIcon() { return icon; }
}
