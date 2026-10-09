package org.datahive.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** A small, deterministic CPU baseline for binary classification on numeric CSV features. */
public final class BinaryLogisticTrainer {
    /** Checks dataset readiness without spending time training a model. */
    public void validate(CsvProfiler.Table table, String targetName,
                         List<String> featureNames) throws IOException {
        int targetIndex = table.headers().indexOf(targetName);
        if (targetIndex < 0) throw new IOException("The target column was not found in the CSV header");
        if (featureNames == null || featureNames.isEmpty()) {
            throw new IOException("Choose at least one numeric feature column");
        }
        List<Integer> featureIndexes = new ArrayList<>();
        Set<Integer> uniqueIndexes = new HashSet<>();
        for (String feature : featureNames) {
            int index = table.headers().indexOf(feature);
            if (index < 0 || index == targetIndex || !uniqueIndexes.add(index)) {
                throw new IOException("Choose distinct feature columns that differ from the target");
            }
            featureIndexes.add(index);
        }

        Set<String> labels = new HashSet<>();
        for (List<String> row : table.rows()) {
            String label = row.get(targetIndex).trim();
            if (!label.isEmpty()) labels.add(label);
        }
        if (labels.size() != 2) {
            throw new IOException("The target column must contain exactly two non-empty classes");
        }

        long[] classRows = new long[2];
        long completeRows = 0;
        long[] numericValues = new long[featureIndexes.size()];
        List<String> orderedLabels = labels.stream().sorted(Comparator.naturalOrder()).toList();
        for (List<String> row : table.rows()) {
            String label = row.get(targetIndex).trim();
            if (label.isEmpty()) continue;
            boolean valid = true;
            for (int j = 0; j < featureIndexes.size(); j++) {
                String raw = row.get(featureIndexes.get(j)).trim();
                if (raw.isEmpty()) continue;
                try {
                    double value = Double.parseDouble(raw);
                    if (!Double.isFinite(value)) {
                        valid = false;
                    } else {
                        numericValues[j]++;
                    }
                } catch (NumberFormatException exception) {
                    throw new IOException("Feature '" + featureNames.get(j)
                            + "' contains a non-numeric value");
                }
            }
            if (valid) {
                completeRows++;
                classRows[orderedLabels.indexOf(label)]++;
            }
        }
        for (int j = 0; j < numericValues.length; j++) {
            if (numericValues[j] == 0) {
                throw new IOException("Feature '" + featureNames.get(j)
                        + "' has no usable numeric values");
            }
        }
        if (completeRows < 10) {
            throw new IOException("At least 10 complete labeled rows are needed for a train/test split");
        }
        if (classRows[0] < 3 || classRows[1] < 3) {
            throw new IOException("Each target class needs at least three complete rows");
        }
    }

    public Result train(CsvProfiler.Table table, String targetName, List<String> featureNames,
                        int seed, int epochs, double learningRate, ProgressListener progress) throws IOException {
        if (epochs < 20 || epochs > 500 || !Double.isFinite(learningRate) || learningRate < 0.005 || learningRate > 0.5) {
            throw new IOException("Training parameters are outside the supported range");
        }
        int targetIndex = table.headers().indexOf(targetName);
        if (targetIndex < 0) throw new IOException("The target column was not found in the CSV header");
        List<Integer> featureIndexes = new ArrayList<>();
        for (String feature : featureNames) {
            int index = table.headers().indexOf(feature);
            if (index >= 0 && index != targetIndex) featureIndexes.add(index);
        }
        if (featureIndexes.isEmpty()) throw new IOException("Choose a target with at least one numeric feature column");

        Set<String> labels = new HashSet<>();
        for (List<String> row : table.rows()) {
            String label = row.get(targetIndex).trim();
            if (!label.isEmpty()) labels.add(label);
        }
        if (labels.size() != 2) throw new IOException("The target column must contain exactly two non-empty classes");
        List<String> orderedLabels = labels.stream().sorted(Comparator.naturalOrder()).toList();
        List<double[]> values = new ArrayList<>();
        List<Integer> outcomes = new ArrayList<>();
        for (List<String> row : table.rows()) {
            String label = row.get(targetIndex).trim();
            if (label.isEmpty()) continue;
            double[] features = new double[featureIndexes.size()];
            boolean valid = true;
            for (int j = 0; j < featureIndexes.size(); j++) {
                String raw = row.get(featureIndexes.get(j)).trim();
                if (raw.isEmpty()) features[j] = Double.NaN;
                else {
                    try { features[j] = Double.parseDouble(raw); }
                    catch (NumberFormatException exception) { throw new IOException("Feature '" + featureNames.get(j) + "' contains a non-numeric value"); }
                    if (!Double.isFinite(features[j])) valid = false;
                }
            }
            if (valid) {
                values.add(features);
                outcomes.add(orderedLabels.indexOf(label));
            }
        }
        if (values.size() < 10) throw new IOException("At least 10 complete labeled rows are needed for a train/test split");

        List<Integer> trainRows = new ArrayList<>();
        List<Integer> testRows = new ArrayList<>();
        Random random = new Random(seed);
        for (int label = 0; label < 2; label++) {
            List<Integer> classRows = new ArrayList<>();
            for (int i = 0; i < outcomes.size(); i++) if (outcomes.get(i) == label) classRows.add(i);
            if (classRows.size() < 3) throw new IOException("Each target class needs at least three complete rows");
            java.util.Collections.shuffle(classRows, random);
            int heldOut = Math.max(1, (int) Math.round(classRows.size() * 0.2));
            if (heldOut >= classRows.size()) heldOut = classRows.size() - 1;
            testRows.addAll(classRows.subList(0, heldOut));
            trainRows.addAll(classRows.subList(heldOut, classRows.size()));
        }
        java.util.Collections.shuffle(trainRows, random);

        int dimensions = featureIndexes.size();
        double[] means = new double[dimensions];
        double[] scales = new double[dimensions];
        for (int index : trainRows) for (int j = 0; j < dimensions; j++) {
            if (!Double.isNaN(values.get(index)[j])) means[j] += values.get(index)[j];
        }
        for (int j = 0; j < dimensions; j++) {
            long observed = 0;
            for (int index : trainRows) if (!Double.isNaN(values.get(index)[j])) observed++;
            if (observed == 0) throw new IOException("A selected feature column contains no numeric values");
            means[j] /= observed;
        }
        for (int index : trainRows) for (int j = 0; j < dimensions; j++) {
            double value = Double.isNaN(values.get(index)[j]) ? means[j] : values.get(index)[j];
            scales[j] += (value - means[j]) * (value - means[j]);
        }
        for (int j = 0; j < dimensions; j++) {
            scales[j] = Math.sqrt(scales[j] / trainRows.size());
            if (scales[j] < 1e-9) scales[j] = 1;
        }

        double[][] x = new double[values.size()][dimensions];
        for (int i = 0; i < values.size(); i++) for (int j = 0; j < dimensions; j++) {
            double value = Double.isNaN(values.get(i)[j]) ? means[j] : values.get(i)[j];
            x[i][j] = (value - means[j]) / scales[j];
        }
        double[] weights = new double[dimensions];
        double bias = 0;
        for (int epoch = 0; epoch < epochs; epoch++) {
            for (int index : trainRows) {
                double error = sigmoid(dot(weights, x[index]) + bias) - outcomes.get(index);
                for (int j = 0; j < dimensions; j++) weights[j] -= learningRate * (error * x[index][j] + 0.001 * weights[j]);
                bias -= learningRate * error;
            }
            int updateInterval = Math.max(1, epochs / 30);
            if (epoch % updateInterval == 0 || epoch == epochs - 1) progress.onProgress((epoch + 1) * 80 / epochs,
                    "Training epoch " + (epoch + 1) + " of " + epochs + " on " + trainRows.size() + " rows.");
        }

        long truePositive = 0, falsePositive = 0, falseNegative = 0, correct = 0;
        for (int index : testRows) {
            int predicted = sigmoid(dot(weights, x[index]) + bias) >= 0.5 ? 1 : 0;
            int actual = outcomes.get(index);
            if (predicted == actual) correct++;
            if (predicted == 1 && actual == 1) truePositive++;
            if (predicted == 1 && actual == 0) falsePositive++;
            if (predicted == 0 && actual == 1) falseNegative++;
        }
        double precision = ratio(truePositive, truePositive + falsePositive);
        double recall = ratio(truePositive, truePositive + falseNegative);
        double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
        progress.onProgress(95, "Evaluated " + testRows.size() + " held-out rows; positive class is '" + orderedLabels.get(1) + "'.");
        return new Result(correct / (double) testRows.size(), precision, recall, f1,
                trainRows.size(), testRows.size(), orderedLabels.get(1), featureNames);
    }

    private static double dot(double[] left, double[] right) {
        double value = 0;
        for (int i = 0; i < left.length; i++) value += left[i] * right[i];
        return value;
    }
    private static double sigmoid(double value) {
        if (value >= 0) return 1 / (1 + Math.exp(-Math.min(value, 40)));
        double exp = Math.exp(Math.max(value, -40));
        return exp / (1 + exp);
    }
    private static double ratio(long numerator, long denominator) { return denominator == 0 ? 0 : numerator / (double) denominator; }

    @FunctionalInterface public interface ProgressListener { void onProgress(int progress, String message) throws IOException; }
    public record Result(double accuracy, double precision, double recall, double f1, int trainRows,
                         int testRows, String positiveClass, List<String> features) { }
}
