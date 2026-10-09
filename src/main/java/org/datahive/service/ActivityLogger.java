package org.datahive.service;

import org.datahive.dao.ActivityLogDao;
import org.datahive.model.User;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Best-effort audit recording; a logging outage must not undo a user action. */
public final class ActivityLogger {
    private static final Logger LOGGER = Logger.getLogger(ActivityLogger.class.getName());
    private static final ActivityLogDao DAO = new ActivityLogDao();

    private ActivityLogger() { }

    public static void record(User actor, String action, String targetType,
                              Long targetId, String summary) {
        record(actor == null ? null : actor.getId(), action, targetType, targetId, summary);
    }

    public static void record(Long actorId, String action, String targetType,
                              Long targetId, String summary) {
        try {
            DAO.record(actorId, action, targetType, targetId, summary);
        } catch (SQLException exception) {
            LOGGER.log(Level.WARNING, "Could not record activity: " + action, exception);
        }
    }
}
