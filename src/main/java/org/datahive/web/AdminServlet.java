package org.datahive.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.datahive.dao.AdminDao;
import org.datahive.dao.ProjectDao;
import org.datahive.model.Role;
import org.datahive.model.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@WebServlet(name = "AdminServlet", urlPatterns = "/admin")
public final class AdminServlet extends HttpServlet {
    private final AdminDao adminDao = new AdminDao();
    private final ProjectDao projectDao = new ProjectDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getAttribute("currentUser");
        if (!requireAdmin(user, response)) return;

        String tab = request.getParameter("tab");
        if (!ListTabs.isKnown(tab)) tab = "overview";
        int usageMonthsToShow = parseUsageMonths(request.getParameter("months"));

        try {
            if ("usage".equals(tab) && "csv".equals(request.getParameter("export"))) {
                writeUsageCsv(response, usageMonthsToShow);
                return;
            }

            request.setAttribute("activePage", "admin");
            request.setAttribute("activeTab", tab);
            request.setAttribute("initials", initials(user.getFullName()));
            request.setAttribute("users", adminDao.listUsers());
            request.setAttribute("resources", adminDao.listResources());
            request.setAttribute("projects", projectDao.findVisibleTo(user));
            request.setAttribute("researchers", projectDao.listActiveResearchers());
            request.setAttribute("usageTotals", adminDao.usageTotals());

            List<org.datahive.model.UsageBucket> rawUsage =
                    adminDao.usageByMonth(usageMonthsToShow);
            double maxCompute = rawUsage.stream()
                    .mapToDouble(org.datahive.model.UsageBucket::getComputeSeconds)
                    .max().orElse(0);
            double maxStorage = rawUsage.stream()
                    .mapToDouble(org.datahive.model.UsageBucket::getStorageBytes)
                    .max().orElse(0);

            List<org.datahive.model.UsageBucket> usageMonths = new ArrayList<>();
            for (org.datahive.model.UsageBucket bucket : rawUsage) {
                int computeBar = maxCompute == 0 ? 0
                        : (int) Math.max(3, Math.round(
                                bucket.getComputeSeconds() / maxCompute * 100));
                int storageBar = maxStorage == 0 ? 0
                        : (int) Math.max(3, Math.round(
                                bucket.getStorageBytes() / maxStorage * 100));
                usageMonths.add(bucket.withBars(computeBar, storageBar));
            }

            request.setAttribute("usageMonths", usageMonths);
            request.setAttribute("usageMonthWindow", usageMonthsToShow);
            request.setAttribute("activeUserCount", adminDao.countActiveUsers());
            request.setAttribute("activeResourceCount", adminDao.countActiveResources());
            request.getRequestDispatcher("/WEB-INF/views/admin.jsp")
                    .forward(request, response);
        } catch (SQLException exception) {
            throw new ServletException("Could not load administration data", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getAttribute("currentUser");
        if (!requireAdmin(actor, response)) return;

        if (!validCsrf(request)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "The request could not be verified");
            return;
        }

        String action = request.getParameter("action");
        try {
            switch (action == null ? "" : action) {
                case "createUser" -> createUser(request, response);
                case "updateUser" -> updateUser(request, response, actor);
                case "setUserActive" -> setUserActive(request, response, actor);
                case "createResource" -> createResource(request, response);
                case "updateResource" -> updateResource(request, response);
                case "deleteResource" -> deleteResource(request, response);
                default -> response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Unknown administration action");
            }
        } catch (IllegalArgumentException exception) {
            response.sendRedirect(request.getContextPath() + "/admin?tab="
                    + safeTab(request) + "&error=validation");
        } catch (SQLException exception) {
            throw new ServletException("Could not save administration changes",
                    exception);
        }
    }

    private void createUser(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        String name = clean(request.getParameter("fullName"));
        String email = clean(request.getParameter("email"));
        String password = request.getParameter("password");
        Role role = parseRole(request.getParameter("role"));

        if (name.length() < 2 || name.length() > 120 || email.length() > 190
                || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || password == null || password.length() < 10
                || password.length() > 200) {
            throw new IllegalArgumentException("Invalid account details");
        }

        adminDao.createUser(name, email, password, role);
        response.sendRedirect(request.getContextPath()
                + "/admin?tab=users&notice=user-created");
    }

    private void updateUser(HttpServletRequest request, HttpServletResponse response,
                            User actor) throws SQLException, IOException {
        long id = positiveId(request.getParameter("userId"));
        String name = clean(request.getParameter("fullName"));
        String email = clean(request.getParameter("email"));
        Role role = parseRole(request.getParameter("role"));
        boolean active = "true".equals(request.getParameter("active"));

        if (name.length() < 2 || name.length() > 120 || email.length() > 190
                || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || (id == actor.getId() && (!active || role != Role.ADMIN))) {
            throw new IllegalArgumentException("Invalid account update");
        }

        if (!adminDao.updateUser(id, name, email, role, active)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found");
            return;
        }

        response.sendRedirect(request.getContextPath()
                + "/admin?tab=users&notice=user-updated");
    }

    private void setUserActive(HttpServletRequest request,
                               HttpServletResponse response, User actor)
            throws SQLException, IOException {
        long id = positiveId(request.getParameter("userId"));
        boolean active = "true".equals(request.getParameter("active"));

        if (id == actor.getId() && !active) {
            throw new IllegalArgumentException("Cannot deactivate current admin");
        }

        if (!adminDao.setUserActive(id, active)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found");
            return;
        }

        response.sendRedirect(request.getContextPath()
                + "/admin?tab=users&notice=user-updated");
    }

    private void createResource(HttpServletRequest request,
                                HttpServletResponse response)
            throws SQLException, IOException {
        ResourceValues values = readResource(request);
        adminDao.createResource(values.name, values.type, values.capacity,
                values.unit, values.status, values.description);
        response.sendRedirect(request.getContextPath()
                + "/admin?tab=resources&notice=resource-created");
    }

    private void updateResource(HttpServletRequest request,
                                HttpServletResponse response)
            throws SQLException, IOException {
        long id = positiveId(request.getParameter("resourceId"));
        ResourceValues values = readResource(request);

        if (!adminDao.updateResource(id, values.name, values.type,
                values.capacity, values.unit, values.status,
                values.description)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Resource not found");
            return;
        }

        response.sendRedirect(request.getContextPath()
                + "/admin?tab=resources&notice=resource-updated");
    }

    private void deleteResource(HttpServletRequest request,
                                HttpServletResponse response)
            throws SQLException, IOException {
        if (!adminDao.deleteResource(
                positiveId(request.getParameter("resourceId")))) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Resource not found");
            return;
        }

        response.sendRedirect(request.getContextPath()
                + "/admin?tab=resources&notice=resource-deleted");
    }

    private static ResourceValues readResource(HttpServletRequest request) {
        String name = clean(request.getParameter("name"));
        String type = clean(request.getParameter("type"))
                .toUpperCase(Locale.ROOT);
        String unit = clean(request.getParameter("unit"));
        String status = clean(request.getParameter("status"))
                .toUpperCase(Locale.ROOT);
        String description = clean(request.getParameter("description"));

        double capacity;
        try {
            capacity = Double.parseDouble(request.getParameter("capacity"));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid capacity");
        }

        if (name.length() < 2 || name.length() > 140
                || !ListTabs.isResourceType(type)
                || !ListTabs.isResourceStatus(status)
                || unit.isBlank() || unit.length() > 24
                || description.length() > 600
                || !Double.isFinite(capacity) || capacity < 0) {
            throw new IllegalArgumentException("Invalid resource details");
        }

        return new ResourceValues(name, type, capacity, unit, status,
                description);
    }

    private static boolean requireAdmin(User user,
                                        HttpServletResponse response)
            throws IOException {
        if (user.getRole() == Role.ADMIN) return true;
        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                "Administrator access is required");
        return false;
    }

    private static boolean validCsrf(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String expected = session == null ? null
                : (String) session.getAttribute(AuthFilter.CSRF_SESSION_KEY);
        return expected != null
                && expected.equals(request.getParameter("csrfToken"));
    }

    private static Role parseRole(String value) {
        try {
            return Role.valueOf(value == null ? "" : value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid role");
        }
    }

    private static long positiveId(String value) {
        long id = Long.parseLong(value);
        if (id < 1) throw new IllegalArgumentException("Invalid identifier");
        return id;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeTab(HttpServletRequest request) {
        String tab = request.getParameter("tab");
        return ListTabs.isKnown(tab) ? tab : "overview";
    }

    private static int parseUsageMonths(String value) {
        try {
            int months = Integer.parseInt(value);
            return months == 3 || months == 6 || months == 12 ? months : 6;
        } catch (RuntimeException exception) {
            return 6;
        }
    }

    private void writeUsageCsv(HttpServletResponse response, int months)
            throws SQLException, IOException {
        List<org.datahive.model.UsageBucket> buckets =
                adminDao.usageByMonth(months);

        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"datahive-usage-" + months
                        + "-months.csv\"");

        var writer = response.getWriter();
        writer.println("month,compute_seconds,storage_bytes");
        for (org.datahive.model.UsageBucket bucket : buckets) {
            writer.printf(Locale.ROOT, "%s,%.3f,%.0f%n", bucket.getLabel(),
                    bucket.getComputeSeconds(), bucket.getStorageBytes());
        }
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) return "DH";
        String[] parts = name.trim().split("\\s+");
        return (parts[0].substring(0, 1)
                + (parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : ""))
                .toUpperCase(Locale.ROOT);
    }

    private record ResourceValues(String name, String type, double capacity,
                                  String unit, String status, String description) { }

    private static final class ListTabs {
        private static boolean isKnown(String tab) {
            return "overview".equals(tab) || "users".equals(tab)
                    || "resources".equals(tab) || "usage".equals(tab)
                    || "projects".equals(tab);
        }

        private static boolean isResourceType(String type) {
            return "COMPUTE".equals(type) || "STORAGE".equals(type);
        }

        private static boolean isResourceStatus(String status) {
            return "AVAILABLE".equals(status) || "IN_USE".equals(status)
                    || "MAINTENANCE".equals(status) || "OFFLINE".equals(status);
        }
    }
}
