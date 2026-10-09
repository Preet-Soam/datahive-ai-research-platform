package org.datahive.service;

import java.io.IOException;
import java.util.List;

/** Selects a supported supervised classification model for a reproducible run. */
public final class ModelTrainer {
    public static final String LOGISTIC = "LOGISTIC_REGRESSION";
    public static final String DECISION_TREE = "DECISION_TREE";

    public void validate(String modelType, CsvProfiler.Table table,
                         String target, List<String> features) throws IOException {
        switch (modelType) {
            case LOGISTIC -> new BinaryLogisticTrainer().validate(table, target, features);
            case DECISION_TREE -> new DecisionTreeTrainer().validate(table, target, features);
            default -> throw new IOException("Choose a supported classification model");
        }
    }

    public BinaryLogisticTrainer.Result train(String modelType, CsvProfiler.Table table,
                                               String target, List<String> features,
                                               int seed, int epochs, double learningRate,
                                               BinaryLogisticTrainer.ProgressListener progress)
            throws IOException {
        return switch (modelType) {
            case LOGISTIC -> new BinaryLogisticTrainer().train(table, target, features,
                    seed, epochs, learningRate, progress);
            case DECISION_TREE -> new DecisionTreeTrainer().train(table, target, features,
                    seed, progress);
            default -> throw new IOException("Choose a supported classification model");
        };
    }

    public static String displayName(String modelType) {
        return switch (modelType) {
            case LOGISTIC -> "Binary logistic regression";
            case DECISION_TREE -> "Decision tree classifier";
            default -> "";
        };
    }
}
