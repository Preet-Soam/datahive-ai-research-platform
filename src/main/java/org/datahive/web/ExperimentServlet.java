package org.datahive.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.datahive.dao.DatasetDao;
import org.datahive.dao.ExperimentDao;
import org.datahive.dao.ProjectDao;
import org.datahive.model.ColumnProfile;
import org.datahive.model.Dataset;
import org.datahive.model.Project;
import org.datahive.model.TrainingExperiment;
import org.datahive.model.User;
import org.datahive.service.TrainingService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

@WebServlet(name = "ExperimentServlet", urlPatterns = "/experiments")
public final class ExperimentServlet extends HttpServlet {
    private final DatasetDao datasetDao = new DatasetDao();
    private final ProjectDao projectDao = new ProjectDao();
    private final ExperimentDao experimentDao = new ExperimentDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getAttribute("currentUser");
        try {
            request.setAttribute("datasets", datasetDao.findVisibleTo(user));
            request.setAttribute("experiments", experimentDao.listVisible(user));
            request.setAttribute("activePage", "experiments");
            request.setAttribute("initials", initials(user.getFullName()));
            String view = request.getParameter("view");
            if (view != null && !view.isBlank()) {
                TrainingExperiment selected = experimentDao.findVisible(positiveId(view), user);
                if (selected == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND, "Experiment not found"); return; }
                request.setAttribute("selectedExperiment", selected);
            }
            request.getRequestDispatcher("/WEB-INF/views/experiments.jsp").forward(request, response);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid experiment identifier");
        } catch (SQLException exception) {
            throw new ServletException("Could not load experiments", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User user = (User) request.getAttribute("currentUser");
        if (!validCsrf(request)) { response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The request could not be verified"); return; }
        try {
            String name = clean(request.getParameter("name"));
            String target = clean(request.getParameter("targetColumn"));
            int epochs = Integer.parseInt(request.getParameter("epochs"));
            double learningRate = Double.parseDouble(request.getParameter("learningRate"));
            Dataset dataset = datasetDao.findVisibleById(positiveId(request.getParameter("datasetId")), user).orElse(null);
            if (dataset == null || name.length() < 3 || name.length() > 160 || target.isBlank() || target.length() > 190 ||
                    (epochs != 60 && epochs != 120 && epochs != 180) ||
                    !Double.isFinite(learningRate) || (learningRate != 0.05 && learningRate != 0.12 && learningRate != 0.2)) {
                response.sendRedirect(request.getContextPath() + "/experiments?error=validation"); return;
            }
            Project project = projectDao.findVisibleById(dataset.getProjectId(), user).orElse(null);
            if (project == null || !"ACTIVE".equals(project.getStatus())) {
                response.sendRedirect(request.getContextPath() + "/experiments?error=project"); return;
            }
            boolean targetExists = dataset.getColumns().stream().anyMatch(column -> column.getName().equals(target));
            List<String> features = dataset.getColumns().stream()
                    .filter(column -> !column.getName().equals(target))
                    .filter(ExperimentServlet::isNumeric)
                    .map(ColumnProfile::getName).toList();
            if (!targetExists || features.isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/experiments?error=features"); return;
            }
            ExperimentDao.TrainingTicket ticket = experimentDao.createAndQueue(project.getId(), dataset.getId(),
                    name, target, features, epochs, learningRate, user.getId());
            TrainingService service = TrainingService.from(getServletContext());
            if (service == null) {
                experimentDao.fail(ticket.runId(), "The local training queue is unavailable.");
                throw new ServletException("Training service was not initialized");
            }
            try { service.enqueue(ticket.runId(), project.getId(), dataset.getId(), user.getId(), dataset, target,
                    features, epochs, learningRate); }
            catch (java.util.concurrent.RejectedExecutionException | IllegalArgumentException exception) {
                experimentDao.fail(ticket.runId(), "The training queue is full. Please try again shortly.");
                response.sendRedirect(request.getContextPath() + "/experiments?error=queue"); return;
            }
            response.sendRedirect(request.getContextPath() + "/experiments?view=" + ticket.experimentId() + "&notice=queued");
        } catch (NumberFormatException exception) {
            response.sendRedirect(request.getContextPath() + "/experiments?error=validation");
        } catch (SQLException exception) {
            throw new ServletException("Could not start the experiment", exception);
        }
    }

    private static boolean isNumeric(ColumnProfile column) {
        return "INTEGER".equals(column.getInferredType()) || "DECIMAL".equals(column.getInferredType());
    }
    private static boolean validCsrf(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(AuthFilter.CSRF_SESSION_KEY);
        return expected != null && expected.equals(request.getParameter("csrfToken"));
    }
    private static long positiveId(String value) { long id = Long.parseLong(value); if (id < 1) throw new NumberFormatException(); return id; }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String initials(String name) {
        if (name == null || name.isBlank()) return "DH";
        String[] parts = name.trim().split("\\s+");
        return (parts[0].substring(0, 1) + (parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : ""))
                .toUpperCase(Locale.ROOT);
    }
}
