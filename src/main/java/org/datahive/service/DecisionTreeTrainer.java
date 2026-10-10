package org.datahive.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import org.datahive.model.TrainingEvaluation;

/** A deterministic CART classifier for small, numeric research datasets. */
public final class DecisionTreeTrainer {
    private static final int MAX_DEPTH = 6;
    private static final int MIN_LEAF_ROWS = 2;

    public void validate(CsvProfiler.Table table, String targetName,
                         List<String> featureNames) throws IOException {
        Prepared prepared = prepare(table, targetName, featureNames);
        if (prepared.labels().size() < 2) {
            throw new IOException("The target column needs at least two non-empty classes");
        }
        if (prepared.labels().size() > 20 || prepared.labels().stream().anyMatch(label -> label.length() > 190)) {
            throw new IOException("The decision-tree evaluation supports up to 20 classes with labels of 190 characters or less");
        }
        if (prepared.rows().size() < 10) {
            throw new IOException("At least 10 complete labeled rows are needed for a train/test split");
        }
        for (String label : prepared.labels()) {
            long count = prepared.rows().stream().filter(row -> row.label().equals(label)).count();
            if (count < 3) {
                throw new IOException("Each target class needs at least three complete rows");
            }
        }
    }

    public BinaryLogisticTrainer.Result train(CsvProfiler.Table table, String targetName,
                                               List<String> featureNames, int seed,
                                               BinaryLogisticTrainer.ProgressListener progress)
            throws IOException {
        Prepared prepared = prepare(table, targetName, featureNames);
        validate(table, targetName, featureNames);
        List<Integer> trainIndexes = new ArrayList<>();
        List<Integer> testIndexes = new ArrayList<>();
        Random random = new Random(seed);
        for (String label : prepared.labels()) {
            List<Integer> classIndexes = new ArrayList<>();
            for (int index = 0; index < prepared.rows().size(); index++) {
                if (prepared.rows().get(index).label().equals(label)) classIndexes.add(index);
            }
            java.util.Collections.shuffle(classIndexes, random);
            int holdout = Math.max(1, (int) Math.round(classIndexes.size() * 0.2));
            if (holdout >= classIndexes.size()) holdout = classIndexes.size() - 1;
            testIndexes.addAll(classIndexes.subList(0, holdout));
            trainIndexes.addAll(classIndexes.subList(holdout, classIndexes.size()));
        }

        double[] means = new double[prepared.featureNames().size()];
        for (int index : trainIndexes) {
            double[] values = prepared.rows().get(index).features();
            for (int feature = 0; feature < values.length; feature++) {
                if (!Double.isNaN(values[feature])) means[feature] += values[feature];
            }
        }
        for (int feature = 0; feature < means.length; feature++) {
            long observed = 0;
            for (int index : trainIndexes) {
                if (!Double.isNaN(prepared.rows().get(index).features()[feature])) observed++;
            }
            if (observed == 0) {
                throw new IOException("Feature '" + prepared.featureNames().get(feature)
                        + "' has no numeric values in the training split");
            }
            means[feature] /= observed;
        }

        double[][] values = new double[prepared.rows().size()][means.length];
        int[] labels = new int[prepared.rows().size()];
        for (int row = 0; row < prepared.rows().size(); row++) {
            values[row] = prepared.rows().get(row).features().clone();
            for (int feature = 0; feature < means.length; feature++) {
                if (Double.isNaN(values[row][feature])) values[row][feature] = means[feature];
            }
            labels[row] = prepared.labels().indexOf(prepared.rows().get(row).label());
        }

        progress.onProgress(35, "Prepared " + trainIndexes.size() + " training rows and "
                + testIndexes.size() + " held-out rows across " + prepared.labels().size() + " classes.");
        Node root = grow(values, labels, trainIndexes, 0, prepared.labels().size());
        progress.onProgress(85, "Fit a depth-limited decision tree using Gini impurity.");

        long[][] confusion = new long[prepared.labels().size()][prepared.labels().size()];
        for (int index : testIndexes) {
            int prediction = predict(root, values[index]);
            int actual = labels[index];
            confusion[actual][prediction]++;
        }
        int[] trainingClassCounts = new int[prepared.labels().size()];
        for (int index : trainIndexes) trainingClassCounts[labels[index]]++;
        TrainingEvaluation evaluation = TrainingEvaluation.from(prepared.labels(), confusion,
                trainingClassCounts, trainIndexes.size(), testIndexes.size());
        progress.onProgress(95, "Evaluated held-out rows with macro-averaged classification metrics.");
        return new BinaryLogisticTrainer.Result(evaluation.getAccuracy(),
                evaluation.getMacroPrecision(), evaluation.getMacroRecall(), evaluation.getMacroF1(),
                trainIndexes.size(), testIndexes.size(), "Macro average (" + prepared.labels().size() + " classes)",
                prepared.featureNames(), evaluation);
    }

    private static Prepared prepare(CsvProfiler.Table table, String targetName,
                                   List<String> featureNames) throws IOException {
        int targetIndex = table.headers().indexOf(targetName);
        if (targetIndex < 0) throw new IOException("The target column was not found in the CSV header");
        if (featureNames == null || featureNames.isEmpty()) {
            throw new IOException("Choose at least one numeric feature column");
        }
        List<Integer> indexes = new ArrayList<>();
        TreeSet<String> labels = new TreeSet<>();
        for (String feature : featureNames) {
            int index = table.headers().indexOf(feature);
            if (index < 0 || index == targetIndex || indexes.contains(index)) {
                throw new IOException("Choose distinct feature columns that differ from the target");
            }
            indexes.add(index);
        }
        List<PreparedRow> rows = new ArrayList<>();
        for (List<String> source : table.rows()) {
            String label = source.get(targetIndex).trim();
            if (label.isEmpty()) continue;
            double[] values = new double[indexes.size()];
            boolean valid = true;
            for (int feature = 0; feature < indexes.size(); feature++) {
                String raw = source.get(indexes.get(feature)).trim();
                if (raw.isEmpty()) {
                    values[feature] = Double.NaN;
                    continue;
                }
                try {
                    values[feature] = Double.parseDouble(raw);
                } catch (NumberFormatException exception) {
                    throw new IOException("Feature '" + featureNames.get(feature)
                            + "' contains a non-numeric value");
                }
                if (!Double.isFinite(values[feature])) valid = false;
            }
            if (valid) {
                labels.add(label);
                rows.add(new PreparedRow(label, values));
            }
        }
        for (int feature = 0; feature < indexes.size(); feature++) {
            final int offset = feature;
            boolean hasNumeric = rows.stream().anyMatch(row -> !Double.isNaN(row.features()[offset]));
            if (!hasNumeric) throw new IOException("Feature '" + featureNames.get(feature)
                    + "' has no usable numeric values");
        }
        return new Prepared(rows, List.copyOf(labels), List.copyOf(featureNames));
    }

    private static Node grow(double[][] values, int[] labels, List<Integer> rows,
                             int depth, int classCount) {
        int majority = majority(labels, rows, classCount);
        if (depth >= MAX_DEPTH || rows.size() < MIN_LEAF_ROWS * 2 || pure(labels, rows)) {
            return Node.leaf(majority);
        }
        Split best = null;
        double parentImpurity = impurity(labels, rows, classCount);
        for (int feature = 0; feature < values[0].length; feature++) {
            final int column = feature;
            List<Integer> sorted = new ArrayList<>(rows);
            sorted.sort(Comparator.comparingDouble(index -> values[index][column]));
            int[] leftCounts = new int[classCount];
            int[] rightCounts = new int[classCount];
            for (int index : sorted) rightCounts[labels[index]]++;
            for (int position = 1; position < sorted.size(); position++) {
                int moved = labels[sorted.get(position - 1)];
                leftCounts[moved]++;
                rightCounts[moved]--;
                if (position < MIN_LEAF_ROWS || sorted.size() - position < MIN_LEAF_ROWS) continue;
                double lower = values[sorted.get(position - 1)][feature];
                double upper = values[sorted.get(position)][feature];
                if (Double.compare(lower, upper) == 0) continue;
                double weighted = (position * impurity(leftCounts, position)
                        + (sorted.size() - position) * impurity(rightCounts, sorted.size() - position))
                        / sorted.size();
                double gain = parentImpurity - weighted;
                if (best == null || gain > best.gain()) {
                    best = new Split(feature, lower / 2.0 + upper / 2.0, gain);
                }
            }
        }
        if (best == null || best.gain() <= 1e-12) return Node.leaf(majority);
        List<Integer> left = new ArrayList<>();
        List<Integer> right = new ArrayList<>();
        for (int index : rows) {
            if (values[index][best.feature()] <= best.threshold()) left.add(index);
            else right.add(index);
        }
        if (left.size() < MIN_LEAF_ROWS || right.size() < MIN_LEAF_ROWS) return Node.leaf(majority);
        return new Node(best.feature(), best.threshold(), majority,
                grow(values, labels, left, depth + 1, classCount),
                grow(values, labels, right, depth + 1, classCount));
    }

    private static boolean pure(int[] labels, List<Integer> rows) {
        int first = labels[rows.get(0)];
        for (int row : rows) if (labels[row] != first) return false;
        return true;
    }

    private static int majority(int[] labels, List<Integer> rows, int classCount) {
        int[] counts = new int[classCount];
        for (int row : rows) counts[labels[row]]++;
        return indexOfMax(counts);
    }

    private static int indexOfMax(int[] values) {
        int max = 0;
        for (int index = 1; index < values.length; index++) if (values[index] > values[max]) max = index;
        return max;
    }

    private static int predict(Node node, double[] values) {
        Node current = node;
        while (!current.leaf()) current = values[current.feature()] <= current.threshold()
                ? current.left() : current.right();
        return current.prediction();
    }

    private static double impurity(int[] counts, int total) {
        if (total == 0) return 0;
        double sum = 0;
        for (int count : counts) {
            double probability = count / (double) total;
            sum += probability * probability;
        }
        return 1 - sum;
    }

    private static double impurity(int[] labels, List<Integer> rows, int classCount) {
        int[] counts = new int[classCount];
        for (int row : rows) counts[labels[row]]++;
        return impurity(counts, rows.size());
    }

    private static double ratio(long numerator, long denominator) {
        return denominator == 0 ? 0 : numerator / (double) denominator;
    }

    private record PreparedRow(String label, double[] features) { }
    private record Prepared(List<PreparedRow> rows, List<String> labels, List<String> featureNames) { }
    private record Split(int feature, double threshold, double gain) { }
    private record Node(int feature, double threshold, int prediction, Node left, Node right) {
        private static Node leaf(int prediction) { return new Node(-1, 0, prediction, null, null); }
        private boolean leaf() { return left == null; }
    }
}
