package org.datahive.model;

public enum Role {
    ADMIN,
    RESEARCHER;

    public static Role fromDatabase(String value) {
        return Role.valueOf(value.toUpperCase());
    }
}
