package org.datahive.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.datahive.dao.ExperimentDao;
import org.datahive.service.ActivityLogger;

import java.io.IOException;
import java.sql.SQLException;

/** Receives a one-time, random-token authenticated result from a Hugging Face Job. */
@WebServlet(name = "HuggingFaceCallbackServlet", urlPatterns = "/hf/callback")
public final class HuggingFaceCallbackServlet extends HttpServlet {
    private final ExperimentDao experiments = new ExperimentDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            long runId = positiveLong(request.getParameter("runId"));
            String token = request.getParameter("token");
            if (token == null || token.length() < 40 || token.length() > 100) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            String status = request.getParameter("status");
            boolean accepted;
            if ("FAILED".equals(status)) {
                String message = request.getParameter("message");
                accepted = experiments.failRemote(runId, token,
                        message == null || message.isBlank() ? "The Hugging Face training job failed." : message);
                if (accepted) ActivityLogger.record((Long) null, "REMOTE_TRAINING_FAILED", "Training run", runId,
                        "A private Hugging Face training job reported a failure.");
            } else if ("COMPLETED".equals(status)) {
                double accuracy = unitMetric("accuracy", request);
                double precision = unitMetric("precision", request);
                double recall = unitMetric("recall", request);
                double f1 = unitMetric("f1", request);
                int trainRows = nonNegativeInt(request.getParameter("trainRows"));
                int testRows = nonNegativeInt(request.getParameter("testRows"));
                double elapsed = boundedDouble(request.getParameter("elapsedSeconds"), 0, 7200);
                String summary = request.getParameter("classSummary");
                if (summary == null) summary = "Remote classifier";
                accepted = experiments.completeRemote(runId, token, accuracy, precision, recall, f1,
                        summary, trainRows, testRows, elapsed);
                if (accepted) ActivityLogger.record((Long) null, "REMOTE_TRAINING_COMPLETED", "Training run", runId,
                        String.format(java.util.Locale.ROOT, "Hugging Face training completed. Accuracy %.1f%%; macro F1 %.1f%%.",
                                accuracy * 100, f1 * 100));
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown remote run state");
                return;
            }
            if (!accepted) {
                response.sendError(HttpServletResponse.SC_CONFLICT, "Run is missing, already finished, or callback token is invalid");
                return;
            }
            response.getWriter().write("{\"accepted\":true}");
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid remote training result");
        } catch (SQLException exception) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Could not save the remote training result");
        }
    }

    private static long positiveLong(String value) {
        long result = Long.parseLong(value);
        if (result < 1) throw new IllegalArgumentException();
        return result;
    }

    private static int nonNegativeInt(String value) {
        int result = Integer.parseInt(value);
        if (result < 0 || result > 50_000) throw new IllegalArgumentException();
        return result;
    }

    private static double unitMetric(String name, HttpServletRequest request) {
        return boundedDouble(request.getParameter(name), 0, 1);
    }

    private static double boundedDouble(String value, double minimum, double maximum) {
        double result = Double.parseDouble(value);
        if (!Double.isFinite(result) || result < minimum || result > maximum) throw new IllegalArgumentException();
        return result;
    }
}
