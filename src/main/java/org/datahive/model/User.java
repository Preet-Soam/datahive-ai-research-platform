package org.datahive.model;

public final class User {
    private final long id;
    private final String fullName;
    private final String email;
    private final Role role;
    private final boolean active;

    public User(long id, String fullName, String email, Role role, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    public long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
}
