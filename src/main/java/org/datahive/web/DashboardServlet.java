package org.datahive.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.datahive.dao.DashboardDao;
import org.datahive.dao.AdminDao;
import org.datahive.dao.DatasetDao;
import org.datahive.dao.ExperimentDao;
import org.datahive.dao.ProjectDao;
import org.datahive.model.DashboardSummary;
import org.datahive.model.Role;
import org.datahive.model.User;
import org.datahive.service.DashboardService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "DashboardServlet", urlPatterns = "/app")
public final class DashboardServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(DashboardServlet.class.getName());
    private final DashboardService dashboardService = new DashboardService();
    private final AdminDao adminDao = new AdminDao();
    private final DatasetDao datasetDao = new DatasetDao();
    private final ExperimentDao experimentDao = new ExperimentDao();
    private final ProjectDao projectDao = new ProjectDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getAttribute("currentUser");
        try {
            DashboardSummary summary = dashboardService.load(user.getRole(), user.getId());
            request.setAttribute("summary", summary);
            request.setAttribute("firstName", firstName(user.getFullName()));
            request.setAttribute("roleLabel", user.getRole().name().toLowerCase(Locale.ROOT));
            request.setAttribute("accountName", user.getFullName());
            request.setAttribute("accountEmail", user.getEmail());
            request.setAttribute("isAdmin", user.getRole() == Role.ADMIN);
            request.setAttribute("initials", initials(user.getFullName()));
            request.setAttribute("activePage", "overview");
            if (user.getRole() == Role.ADMIN) {
                request.setAttribute("adminUsers", adminDao.listUsers().stream().limit(5).toList());
                request.setAttribute("adminResources", adminDao.listResources().stream().limit(5).toList());
                request.setAttribute("dashboardProjects", projectDao.findVisibleTo(user).stream().limit(5).toList());
            } else {
                request.setAttribute("dashboardDatasets", datasetDao.findVisibleTo(user).stream().limit(5).toList());
                request.setAttribute("dashboardRuns", experimentDao.listVisible(user).stream().limit(5).toList());
                request.setAttribute("dashboardProjects", projectDao.findVisibleTo(user).stream().limit(5).toList());
            }
            request.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(request, response);
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Dashboard data could not be loaded", exception);
            throw new ServletException("Dashboard data could not be loaded", exception);
        }
    }

    private static String firstName(String name) {
        if (name == null || name.isBlank()) {
            return "there";
        }
        return name.trim().split("\\s+", 2)[0];
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "DH";
        }
        String[] parts = name.trim().split("\\s+");
        return parts.length == 1
                ? parts[0].substring(0, 1).toUpperCase(Locale.ROOT)
                : (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }
}
