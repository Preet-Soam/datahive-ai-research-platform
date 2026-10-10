package org.datahive.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.datahive.dao.ProjectDao;
import org.datahive.model.Project;
import org.datahive.model.Role;
import org.datahive.model.User;
import org.datahive.service.ActivityLogger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

@WebServlet(name = "CollaborationServlet", urlPatterns = "/collaboration")
public final class CollaborationServlet extends HttpServlet {
    private final ProjectDao projectDao = new ProjectDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getAttribute("currentUser");
        if (user.getRole() == Role.ADMIN) {
            response.sendRedirect(request.getContextPath() + "/admin?tab=projects");
            return;
        }
        try {
            List<Project> projects = projectDao.findVisibleTo(user).stream()
                    .filter(project -> "ACTIVE".equals(project.getStatus())).toList();
            request.setAttribute("projects", projects);
            request.setAttribute("activePage", "collaboration");
            request.setAttribute("initials", initials(user.getFullName()));
            String requested = request.getParameter("project");
            Project selected = null;
            if (requested != null && !requested.isBlank()) {
                selected = projectDao.findVisibleById(positiveId(requested), user).orElse(null);
                if (selected == null || !"ACTIVE".equals(selected.getStatus())) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Active project not found"); return;
                }
            } else if (!projects.isEmpty()) selected = projects.getFirst();
            if (selected != null) {
                request.setAttribute("selectedProject", selected);
                request.setAttribute("members", projectDao.listMembers(selected.getId()));
                boolean canManage = user.getRole() == Role.ADMIN || selected.getOwnerId() == user.getId();
                request.setAttribute("canManageMembers", canManage);
                if (canManage) request.setAttribute("availableResearchers", projectDao.listAvailableResearchers(selected.getId()));
            }
            request.getRequestDispatcher("/WEB-INF/views/collaboration.jsp").forward(request, response);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid project identifier");
        } catch (SQLException exception) {
            throw new ServletException("Could not load project collaboration", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getAttribute("currentUser");
        if (actor.getRole() == Role.ADMIN) {
            response.sendRedirect(request.getContextPath() + "/admin?tab=projects");
            return;
        }
        if (!validCsrf(request)) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The request could not be verified"); return; }
        try {
            long projectId = positiveId(request.getParameter("projectId"));
            long memberId = positiveId(request.getParameter("userId"));
            Project project = projectDao.findVisibleById(projectId, actor).orElse(null);
            if (project == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND, "Project not found"); return; }
            if (!"ACTIVE".equals(project.getStatus()) ||
                    (actor.getRole() != Role.ADMIN && project.getOwnerId() != actor.getId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only the project owner can manage its team"); return;
            }
            String action = request.getParameter("action");
            boolean changed;
            if ("add".equals(action)) changed = projectDao.addResearcher(projectId, memberId);
            else if ("remove".equals(action)) changed = projectDao.removeResearcher(projectId, memberId);
            else { response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown collaboration action"); return; }
            if (changed) ActivityLogger.record(actor,
                    "add".equals(action) ? "MEMBER_ADDED" : "MEMBER_REMOVED", "Project", projectId,
                    ("add".equals(action) ? "Added researcher" : "Removed researcher") + " account #" + memberId +
                            " from project '" + project.getTitle() + "'.");
            response.sendRedirect(request.getContextPath() + "/collaboration?project=" + projectId +
                    (changed ? "&notice=updated" : "&error=member"));
        } catch (NumberFormatException exception) {
            response.sendRedirect(request.getContextPath() + "/collaboration?error=member");
        } catch (SQLException exception) {
            throw new ServletException("Could not update project membership", exception);
        }
    }

    private static boolean validCsrf(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(AuthFilter.CSRF_SESSION_KEY);
        return expected != null && expected.equals(request.getParameter("csrfToken"));
    }
    private static long positiveId(String value) { long id = Long.parseLong(value); if (id < 1) throw new NumberFormatException(); return id; }
    private static String initials(String name) {
        if (name == null || name.isBlank()) return "DH";
        String[] parts = name.trim().split("\\s+");
        return (parts[0].substring(0, 1) + (parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : ""))
                .toUpperCase(Locale.ROOT);
    }
}
