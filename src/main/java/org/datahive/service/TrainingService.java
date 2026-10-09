package org.datahive.service;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.datahive.config.AppStorage;
import org.datahive.dao.ExperimentDao;
import org.datahive.model.ColumnProfile;
import org.datahive.model.Dataset;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Owns a small bounded local training queue for demo-sized research runs. */
public final class TrainingService implements ServletContextListener {
    public static final String CONTEXT_KEY = TrainingService.class.getName();
    private static final Logger LOGGER = Logger.getLogger(TrainingService.class.getName());
    private ThreadPoolExecutor executor;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try { new ExperimentDao().failInterruptedRuns(); }
        catch (SQLException exception) { throw new IllegalStateException("Could not recover interrupted training runs", exception); }
        executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(12), runnable -> {
                    Thread thread = new Thread(runnable, "datahive-training");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
        event.getServletContext().setAttribute(CONTEXT_KEY, this);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (executor != null) executor.shutdownNow();
    }

    public void enqueue(long runId, long projectId, long datasetId, long userId, Dataset dataset,
                        String targetColumn, List<String> features, int epochs, double learningRate) {
        Path root = AppStorage.uploadDirectory();
        Path csv = root.resolve(dataset.getStoredFilename()).normalize();
        if (!csv.startsWith(root)) throw new IllegalArgumentException("Invalid dataset storage path");
        executor.execute(() -> run(runId, projectId, userId, csv, targetColumn, features, epochs, learningRate));
    }

    private void run(long runId, long projectId, long userId, Path csv,
                     String targetColumn, List<String> features, int epochs, double learningRate) {
        ExperimentDao dao = new ExperimentDao();
        long started = System.nanoTime();
        try {
            if (!dao.startRun(runId)) return;
            CsvProfiler.Table table = new CsvProfiler().readTable(csv);
            BinaryLogisticTrainer.Result result = new BinaryLogisticTrainer().train(table, targetColumn, features,
                    42, epochs, learningRate,
                    (progress, message) -> {
                        try { dao.updateProgress(runId, progress, message); }
                        catch (SQLException exception) { throw new IOException("Could not record training progress", exception); }
                    });
            double seconds = (System.nanoTime() - started) / 1_000_000_000.0;
            dao.complete(runId, projectId, userId, seconds, result.accuracy(), result.precision(),
                    result.recall(), result.f1(), result.positiveClass(), result.trainRows(), result.testRows());
            ActivityLogger.record(userId, "TRAINING_COMPLETED", "Training run", runId,
                    String.format(java.util.Locale.ROOT, "Training run #%d completed on local CPU; accuracy %.1f%%, precision %.1f%%, recall %.1f%%, F1 %.1f%% (%d train / %d test rows).",
                            runId, result.accuracy() * 100, result.precision() * 100, result.recall() * 100,
                            result.f1() * 100, result.trainRows(), result.testRows()));
        } catch (Exception exception) {
            String message = exception.getMessage();
            try { dao.fail(runId, message); }
            catch (SQLException persistenceException) {
                LOGGER.log(Level.SEVERE, "Training failed and its failure state could not be saved for run " + runId,
                        persistenceException);
            }
            ActivityLogger.record(userId, "TRAINING_FAILED", "Training run", runId,
                    "Training run #" + runId + " failed: " + (message == null ? "Unknown training error." : message));
            LOGGER.log(Level.INFO, "Training run {0} failed: {1}", new Object[]{runId, message});
        }
    }

    public static TrainingService from(ServletContext context) {
        return (TrainingService) context.getAttribute(CONTEXT_KEY);
    }

}
