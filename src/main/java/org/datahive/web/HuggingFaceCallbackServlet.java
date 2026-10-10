package org.datahive.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.datahive.dao.ExperimentDao;
import org.datahive.model.TrainingEvaluation;
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
                TrainingEvaluation evaluation = evaluation(request, trainRows, testRows);
                accepted = experiments.completeRemote(runId, token, accuracy, precision, recall, f1,
                        summary, trainRows, testRows, elapsed, evaluation);
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

    private static TrainingEvaluation evaluation(HttpServletRequest request, int trainRows, int testRows) {
        int count = nonNegativeInt(request.getParameter("classCount"));
        if (count < 2 || count > 20) throw new IllegalArgumentException();
        java.util.List<String> labels = new java.util.ArrayList<>();
        int[] trainingCounts = parseCounts(request.getParameter("trainingClassCounts"), count);
        long[] flatMatrix = parseCountsAsLong(request.getParameter("confusionCounts"), count * count);
        long[][] confusion = new long[count][count];
        long testSum = 0;
        long trainSum = 0;
        for (int value : trainingCounts) trainSum += value;
        if (trainSum != trainRows) throw new IllegalArgumentException();
        for (int actual = 0; actual < count; actual++) {
            String encoded = request.getParameter("classLabel" + actual);
            if (encoded == null || encoded.length() > 700) throw new IllegalArgumentException();
            String label;
            try {
                String padded = encoded + "=".repeat((4 - encoded.length() % 4) % 4);
                label = new String(java.util.Base64.getUrlDecoder().decode(padded), java.nio.charset.StandardCharsets.UTF_8);
            } catch (IllegalArgumentException exception) { throw new IllegalArgumentException(); }
            if (label.isBlank() || label.length() > 190) throw new IllegalArgumentException();
            labels.add(label);
            for (int predicted = 0; predicted < count; predicted++) {
                confusion[actual][predicted] = flatMatrix[actual * count + predicted];
                testSum += confusion[actual][predicted];
            }
        }
        if (testSum != testRows) throw new IllegalArgumentException();
        return TrainingEvaluation.from(labels, confusion, trainingCounts, trainRows, testRows);
    }

    private static int[] parseCounts(String value, int expected) {
        long[] parsed = parseCountsAsLong(value, expected);
        int[] counts = new int[expected];
        for (int index = 0; index < expected; index++) {
            if (parsed[index] > 50_000) throw new IllegalArgumentException();
            counts[index] = (int) parsed[index];
        }
        return counts;
    }

    private static long[] parseCountsAsLong(String value, int expected) {
        if (value == null || value.length() > 10_000) throw new IllegalArgumentException();
        String[] parts = value.split(",", -1);
        if (parts.length != expected) throw new IllegalArgumentException();
        long[] counts = new long[expected];
        for (int index = 0; index < expected; index++) {
            counts[index] = Long.parseLong(parts[index]);
            if (counts[index] < 0 || counts[index] > 50_000) throw new IllegalArgumentException();
        }
        return counts;
    }
}
