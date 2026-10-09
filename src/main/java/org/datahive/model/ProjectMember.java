package org.datahive.model;

public final class ProjectMember {
    private final long id;
    private final String fullName;
    private final String email;
    private final String memberRole;
    public ProjectMember(long id, String fullName, String email, String memberRole) {
        this.id = id; this.fullName = fullName; this.email = email; this.memberRole = memberRole;
    }
    public long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getMemberRole() { return memberRole; }
    public String getRoleLabel() { return memberRole == null ? "researcher" : memberRole.toLowerCase(java.util.Locale.ROOT); }
    public String getInitials() {
        if (fullName == null || fullName.isBlank()) return "R";
        String[] parts = fullName.trim().split("\\s+");
        return (parts[0].substring(0, 1) + (parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : ""))
                .toUpperCase(java.util.Locale.ROOT);
    }
}
