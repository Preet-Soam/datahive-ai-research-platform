package org.datahive.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.datahive.dao.DashboardDao;
import org.datahive.model.DashboardSummary;
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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getAttribute("currentUser");
        try {
            DashboardSummary summary = dashboardService.load(user.getRole(), user.getId());
            request.setAttribute("summary", summary);
            request.setAttribute("firstName", firstName(user.getFullName()));
            request.setAttribute("roleLabel", user.getRole().name().toLowerCase(Locale.ROOT));
            request.setAttribute("initials", initials(user.getFullName()));
            request.setAttribute("activePage", "overview");
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
