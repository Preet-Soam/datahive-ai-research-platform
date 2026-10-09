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

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet(name = "ProjectServlet", urlPatterns = "/projects")
public final class ProjectServlet extends HttpServlet {
    private final ProjectDao projectDao = new ProjectDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getAttribute("currentUser");
        try {
            List<Project> projects = projectDao.findVisibleTo(user);
            request.setAttribute("projects", projects);
            request.setAttribute("activeProjectCount", projects.stream().filter(p -> "ACTIVE".equals(p.getStatus())).count());
            if (user.getRole() == Role.ADMIN) {
                request.setAttribute("researchers", projectDao.listActiveResearchers());
            }
            String editId = request.getParameter("edit");
            if (editId != null && !editId.isBlank()) {
                long id = positiveId(editId);
                Project project = projectDao.findVisibleById(id, user).orElse(null);
                if (project == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Project not found");
                    return;
                }
                if (user.getRole() != Role.ADMIN && project.getOwnerId() != user.getId()) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only the project owner can edit it");
                    return;
                }
                request.setAttribute("editingProject", project);
            }
            request.setAttribute("activePage", "projects");
            request.setAttribute("initials", initials(user.getFullName()));
            request.getRequestDispatcher("/WEB-INF/views/projects.jsp").forward(request, response);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid project identifier");
        } catch (SQLException exception) {
            throw new ServletException("Could not load projects", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User user = (User) request.getAttribute("currentUser");
        if (!validCsrf(request)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The request could not be verified");
            return;
        }

        String action = request.getParameter("action");
        try {
            if ("archive".equals(action)) {
                long id = positiveId(request.getParameter("projectId"));
                if (!projectDao.archive(id, user)) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Project not found or already archived");
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/projects?notice=archived");
                return;
            }

            String title = clean(request.getParameter("title"));
            String description = clean(request.getParameter("description"));
            if (title.length() < 3 || title.length() > 160 || description.length() > 1200) {
                response.sendRedirect(request.getContextPath() + "/projects?error=validation");
                return;
            }
            long ownerId = user.getId();
            if (user.getRole() == Role.ADMIN) {
                ownerId = positiveId(request.getParameter("ownerId"));
                if (!projectDao.isActiveResearcher(ownerId)) {
                    response.sendRedirect(request.getContextPath() + "/projects?error=owner");
                    return;
                }
            }

            if ("create".equals(action)) {
                projectDao.create(title, description, ownerId);
                response.sendRedirect(request.getContextPath() + "/projects?notice=created");
            } else if ("update".equals(action)) {
                long id = positiveId(request.getParameter("projectId"));
                if (!projectDao.update(id, title, description, ownerId, user)) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Project not found or cannot be edited");
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/projects?notice=updated");
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown project action");
            }
        } catch (NumberFormatException exception) {
            response.sendRedirect(request.getContextPath() + "/projects?error=validation");
        } catch (SQLException exception) {
            throw new ServletException("Could not save the project", exception);
        }
    }

    private static boolean validCsrf(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(AuthFilter.CSRF_SESSION_KEY);
        String submitted = request.getParameter("csrfToken");
        return expected != null && expected.equals(submitted);
    }

    private static long positiveId(String value) {
        long id = Long.parseLong(value);
        if (id < 1) {
            throw new NumberFormatException("Identifier must be positive");
        }
        return id;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) return "DH";
        String[] parts = name.trim().split("\\s+");
        return (parts[0].substring(0, 1) + (parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : ""))
                .toUpperCase(java.util.Locale.ROOT);
    }
}
