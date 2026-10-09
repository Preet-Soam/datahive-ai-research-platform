package org.datahive.config;

import java.nio.file.Path;

/** Resolves persistent app files to a writable per-user directory by default. */
public final class AppStorage {
    private static final Path DEFAULT_ROOT = Path.of(System.getProperty("user.home"), ".datahive");

    private AppStorage() {
    }

    public static Path databaseDirectory() {
        return configuredPath("DATAHIVE_DATA_DIR", DEFAULT_ROOT.resolve("data"));
    }

    public static Path uploadDirectory() {
        return configuredPath("DATAHIVE_UPLOAD_DIR", DEFAULT_ROOT.resolve("uploads"));
    }

    private static Path configuredPath(String name, Path fallback) {
        String configured = System.getenv(name);
        return Path.of(configured == null || configured.isBlank() ? fallback.toString() : configured)
                .toAbsolutePath()
                .normalize();
    }
}
