package org.datahive.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.datahive.dao.ProfileDao;
import org.datahive.model.User;
import org.datahive.service.ActivityLogger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

@WebServlet(name = "ProfileServlet", urlPatterns = "/profile")
public final class ProfileServlet extends HttpServlet {
    private final ProfileDao profileDao = new ProfileDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getAttribute("currentUser");
        request.setAttribute("activePage", "profile");
        request.setAttribute("initials", initials(user.getFullName()));
        request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User user = (User) request.getAttribute("currentUser");
        if (!validCsrf(request)) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The request could not be verified"); return; }
        String name = clean(request.getParameter("fullName"));
        String email = clean(request.getParameter("email"));
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");
        if (name.length() < 2 || name.length() > 120 || email.length() > 190 ||
                !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") ||
                (newPassword != null && !newPassword.isBlank() &&
                        (newPassword.length() < 10 || newPassword.length() > 200 || !newPassword.equals(confirmPassword)))) {
            response.sendRedirect(request.getContextPath() + "/profile?error=validation"); return;
        }
        try {
            if (!profileDao.update(user.getId(), name, email, currentPassword, newPassword)) {
                response.sendRedirect(request.getContextPath() + "/profile?error=password"); return;
            }
            User updated = new User(user.getId(), name, email, user.getRole(), true);
            HttpSession session = request.getSession(false);
            if (session != null) session.setAttribute(AuthFilter.USER_SESSION_KEY, updated);
            ActivityLogger.record(updated, "PROFILE_UPDATED", "User", updated.getId(),
                    "Updated profile details for " + name + ".");
            response.sendRedirect(request.getContextPath() + "/profile?notice=saved");
        } catch (SQLException exception) {
            if (exception.getSQLState() != null && exception.getSQLState().startsWith("23")) {
                response.sendRedirect(request.getContextPath() + "/profile?error=email"); return;
            }
            throw new ServletException("Could not update the profile", exception);
        }
    }

    private static boolean validCsrf(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(AuthFilter.CSRF_SESSION_KEY);
        return expected != null && expected.equals(request.getParameter("csrfToken"));
    }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String initials(String name) {
        if (name == null || name.isBlank()) return "DH";
        String[] parts = name.trim().split("\\s+");
        return (parts[0].substring(0, 1) + (parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : ""))
                .toUpperCase(Locale.ROOT);
    }
}
