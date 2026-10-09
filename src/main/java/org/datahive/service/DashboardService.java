package org.datahive.service;

import org.datahive.dao.DashboardDao;
import org.datahive.model.DashboardSummary;
import org.datahive.model.Role;

import java.sql.SQLException;

public final class DashboardService {
    private final DashboardDao dashboardDao;

    public DashboardService() {
        this(new DashboardDao());
    }

    DashboardService(DashboardDao dashboardDao) {
        this.dashboardDao = dashboardDao;
    }

    public DashboardSummary load(Role role, long userId) throws SQLException {
        return dashboardDao.load(role, userId);
    }
}
