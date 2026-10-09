package org.datahive.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import org.datahive.dao.DatasetDao;
import org.datahive.dao.ProjectDao;
import org.datahive.model.Dataset;
import org.datahive.model.Project;
import org.datahive.model.Role;
import org.datahive.model.User;
import org.datahive.service.CsvProfiler;
import org.datahive.service.ActivityLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@WebServlet(name = "DatasetServlet", urlPatterns = "/datasets")
@MultipartConfig(maxFileSize = DatasetServlet.MAX_UPLOAD_BYTES,
        maxRequestSize = DatasetServlet.MAX_REQUEST_BYTES,
        fileSizeThreshold = 1_048_576)
public final class DatasetServlet extends HttpServlet {
    static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;
    static final long MAX_REQUEST_BYTES = 11L * 1024 * 1024;

    private final DatasetDao datasetDao = new DatasetDao();
    private final ProjectDao projectDao = new ProjectDao();
    private final CsvProfiler csvProfiler = new CsvProfiler();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getAttribute("currentUser");
        try {
            List<Project> projects = projectDao.findVisibleTo(user).stream()
                    .filter(project -> "ACTIVE".equals(project.getStatus())).toList();
            request.setAttribute("projects", projects);
            request.setAttribute("datasets", datasetDao.findVisibleTo(user));
            request.setAttribute("activeProjectCount", projects.size());
            request.setAttribute("activePage", "datasets");
            request.setAttribute("initials", initials(user.getFullName()));

            String view = request.getParameter("view");
            if (view != null && !view.isBlank()) {
                Dataset dataset = datasetDao.findVisibleById(positiveId(view), user).orElse(null);
                if (dataset == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Dataset not found");
                    return;
                }
                request.setAttribute("selectedDataset", dataset);
            }
            request.getRequestDispatcher("/WEB-INF/views/datasets.jsp").forward(request, response);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid dataset identifier");
        } catch (SQLException exception) {
            throw new ServletException("Could not load datasets", exception);
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
        if ("delete".equals(action)) {
            delete(request, response, user);
            return;
        }
        if ("update".equals(action)) {
            updateMetadata(request, response, user);
            return;
        }
        if (!"upload".equals(action)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown dataset action");
            return;
        }
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.RESEARCHER) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Path storedPath = null;
        try {
            long projectId = positiveId(request.getParameter("projectId"));
            Project project = projectDao.findVisibleById(projectId, user).orElse(null);
            if (project == null || !"ACTIVE".equals(project.getStatus())) {
                response.sendRedirect(request.getContextPath() + "/datasets?error=project");
                return;
            }

            Part file = request.getPart("datasetFile");
            if (file == null || file.getSize() < 1 || file.getSize() > MAX_UPLOAD_BYTES) {
                response.sendRedirect(request.getContextPath() + "/datasets?error=size");
                return;
            }
            String originalFilename = safeOriginalFilename(file.getSubmittedFileName());
            if (originalFilename.isBlank() || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
                response.sendRedirect(request.getContextPath() + "/datasets?error=format");
                return;
            }

            String storedFilename = UUID.randomUUID().toString().replace("-", "") + ".csv";
            Path uploadRoot = uploadRoot();
            Files.createDirectories(uploadRoot);
            storedPath = uploadRoot.resolve(storedFilename).normalize();
            if (!storedPath.startsWith(uploadRoot)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid upload path");
                return;
            }
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, storedPath, StandardCopyOption.REPLACE_EXISTING);
            }

            CsvProfiler.Profile profile = csvProfiler.inspect(storedPath);
            String name = clean(request.getParameter("name"));
            if (name.isBlank()) {
                name = withoutExtension(originalFilename);
            }
            String description = clean(request.getParameter("description"));
            if (name.length() < 2 || name.length() > 160 || description.length() > 1000) {
                Files.deleteIfExists(storedPath);
                response.sendRedirect(request.getContextPath() + "/datasets?error=validation");
                return;
            }
            long datasetId = datasetDao.create(projectId, name, description, originalFilename, storedFilename,
                    file.getSize(), user.getId(), profile);
            ActivityLogger.record(user, "DATASET_UPLOADED", "Dataset", datasetId,
                    "Uploaded dataset '" + name + "' to project '" + project.getTitle() + "' (" + profile.rowCount() + " rows, " + profile.columnCount() + " columns).");
            response.sendRedirect(request.getContextPath() + "/datasets?notice=uploaded");
        } catch (NumberFormatException exception) {
            deleteStored(storedPath);
            response.sendRedirect(request.getContextPath() + "/datasets?error=project");
        } catch (IOException exception) {
            deleteStored(storedPath);
            response.sendRedirect(request.getContextPath() + "/datasets?error=csv");
        } catch (SQLException exception) {
            deleteStored(storedPath);
            throw new ServletException("Could not store the uploaded dataset", exception);
        }
    }

    private void updateMetadata(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        try {
            long datasetId = positiveId(request.getParameter("datasetId"));
            String name = clean(request.getParameter("name"));
            String description = clean(request.getParameter("description"));
            if (name.length() < 2 || name.length() > 160 || description.length() > 1000) {
                response.sendRedirect(request.getContextPath() + "/datasets?view=" + datasetId + "&error=metadata");
                return;
            }
            if (!datasetDao.updateMetadata(datasetId, name, description, user)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Dataset not found or cannot be edited");
                return;
            }
            ActivityLogger.record(user, "DATASET_UPDATED", "Dataset", datasetId,
                    "Updated dataset details for '" + name + "'.");
            response.sendRedirect(request.getContextPath() + "/datasets?view=" + datasetId + "&notice=updated");
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid dataset identifier");
        } catch (SQLException exception) {
            throw new ServletException("Could not update dataset details", exception);
        }
    }

    private void delete(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        try {
            long datasetId = positiveId(request.getParameter("datasetId"));
            Dataset dataset = datasetDao.findVisibleById(datasetId, user).orElse(null);
            Optional<String> stored = datasetDao.delete(datasetId, user);
            if (stored.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Dataset not found or cannot be deleted");
                return;
            }
            ActivityLogger.record(user, "DATASET_DELETED", "Dataset", datasetId,
                    "Deleted dataset '" + (dataset == null ? "Dataset #" + datasetId : dataset.getName()) + "'.");
            Path path = uploadRoot().resolve(stored.get()).normalize();
            if (path.startsWith(uploadRoot())) Files.deleteIfExists(path);
            response.sendRedirect(request.getContextPath() + "/datasets?notice=deleted");
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid dataset identifier");
        } catch (SQLException exception) {
            throw new ServletException("Could not delete the dataset", exception);
        }
    }

    private static boolean validCsrf(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(AuthFilter.CSRF_SESSION_KEY);
        return expected != null && expected.equals(request.getParameter("csrfToken"));
    }

    private static Path uploadRoot() {
        String configured = System.getenv("DATAHIVE_UPLOAD_DIR");
        return Path.of(configured == null || configured.isBlank() ? "uploads" : configured).toAbsolutePath().normalize();
    }

    private static String safeOriginalFilename(String submitted) {
        if (submitted == null) return "";
        String basename = submitted.replace('\\', '/');
        basename = basename.substring(basename.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        return basename.length() > 255 ? basename.substring(basename.length() - 255) : basename;
    }

    private static String withoutExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }

    private static long positiveId(String value) {
        long id = Long.parseLong(value);
        if (id < 1) throw new NumberFormatException("Identifier must be positive");
        return id;
    }

    private static void deleteStored(Path path) {
        if (path == null) return;
        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) return "DH";
        String[] parts = name.trim().split("\\s+");
        return (parts[0].substring(0, 1) + (parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : ""))
                .toUpperCase(Locale.ROOT);
    }
}
