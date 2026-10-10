package org.datahive.model;

import java.util.ArrayList;
import java.util.List;

/** Evaluation evidence calculated from the held-out rows of one training run. */
public final class TrainingEvaluation {
    private final int trainRows;
    private final int testRows;
    private final double accuracy;
    private final double macroPrecision;
    private final double macroRecall;
    private final double macroF1;
    private final double baselineAccuracy;
    private final List<String> labels;
    private final List<ClassMetric> classes;
    private final List<MatrixRow> matrixRows;

    private TrainingEvaluation(int trainRows, int testRows, double accuracy, double macroPrecision,
                               double macroRecall, double macroF1, double baselineAccuracy,
                               List<String> labels, List<ClassMetric> classes, List<MatrixRow> matrixRows) {
        this.trainRows = trainRows;
        this.testRows = testRows;
        this.accuracy = accuracy;
        this.macroPrecision = macroPrecision;
        this.macroRecall = macroRecall;
        this.macroF1 = macroF1;
        this.baselineAccuracy = baselineAccuracy;
        this.labels = List.copyOf(labels);
        this.classes = List.copyOf(classes);
        this.matrixRows = List.copyOf(matrixRows);
    }

    /** Builds class metrics and baseline performance using train-only class frequencies. */
    public static TrainingEvaluation from(List<String> labels, long[][] confusion,
                                          int[] trainingClassCounts, int trainRows, int testRows) {
        if (labels == null || labels.isEmpty() || labels.size() > 20 || confusion == null ||
                confusion.length != labels.size() || trainingClassCounts == null ||
                trainingClassCounts.length != labels.size() || trainRows < 1 || testRows < 1) {
            throw new IllegalArgumentException("Invalid classification evaluation dimensions");
        }
        int baselineClass = 0;
        for (int index = 0; index < labels.size(); index++) {
            if (confusion[index] == null || confusion[index].length != labels.size() || trainingClassCounts[index] < 0) {
                throw new IllegalArgumentException("Invalid classification evaluation values");
            }
            if (trainingClassCounts[index] > trainingClassCounts[baselineClass]) baselineClass = index;
        }
        List<ClassMetric> classes = new ArrayList<>();
        List<MatrixRow> matrixRows = new ArrayList<>();
        java.util.Set<String> uniqueLabels = new java.util.HashSet<>(labels);
        if (uniqueLabels.size() != labels.size() || labels.stream().anyMatch(label -> label == null || label.isBlank() || label.length() > 190)) {
            throw new IllegalArgumentException("Class labels must be unique, non-empty, and within the supported length");
        }
        double precisionSum = 0;
        double recallSum = 0;
        double f1Sum = 0;
        long correct = 0;
        long baselineCorrect = 0;
        long observedTestRows = 0;
        for (int actual = 0; actual < labels.size(); actual++) {
            long support = 0;
            long predicted = 0;
            List<MatrixCell> cells = new ArrayList<>();
            for (int prediction = 0; prediction < labels.size(); prediction++) {
                long count = confusion[actual][prediction];
                if (count < 0) throw new IllegalArgumentException("Confusion counts cannot be negative");
                support += count;
                observedTestRows += count;
                predicted += confusion[prediction][actual];
                if (actual == prediction) correct += count;
                if (prediction == baselineClass) baselineCorrect += count;
                cells.add(new MatrixCell(count, actual == prediction));
            }
            long truePositive = confusion[actual][actual];
            double precision = ratio(truePositive, predicted);
            double recall = ratio(truePositive, support);
            double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
            classes.add(new ClassMetric(labels.get(actual), support, predicted, precision, recall, f1));
            matrixRows.add(new MatrixRow(labels.get(actual), support, cells));
            precisionSum += precision;
            recallSum += recall;
            f1Sum += f1;
        }
        if (observedTestRows != testRows) throw new IllegalArgumentException("Confusion matrix total does not match the holdout size");
        double count = labels.size();
        return new TrainingEvaluation(trainRows, testRows, correct / (double) testRows,
                precisionSum / count, recallSum / count, f1Sum / count,
                baselineCorrect / (double) testRows, labels, classes, matrixRows);
    }

    public static TrainingEvaluation restore(List<String> labels, long[][] confusion,
                                             int trainRows, int testRows, double baselineAccuracy) {
        int[] placeholderCounts = new int[labels.size()];
        placeholderCounts[0] = 1;
        TrainingEvaluation calculated = from(labels, confusion, placeholderCounts, trainRows, testRows);
        if (!Double.isFinite(baselineAccuracy) || baselineAccuracy < 0 || baselineAccuracy > 1) {
            throw new IllegalArgumentException("Invalid saved baseline score");
        }
        return new TrainingEvaluation(trainRows, testRows, calculated.accuracy,
                calculated.macroPrecision, calculated.macroRecall, calculated.macroF1,
                baselineAccuracy, calculated.labels, calculated.classes, calculated.matrixRows);
    }

    private static double ratio(long numerator, long denominator) {
        return denominator == 0 ? 0 : numerator / (double) denominator;
    }

    public int getTrainRows() { return trainRows; }
    public int getTestRows() { return testRows; }
    public double getAccuracy() { return accuracy; }
    public double getMacroPrecision() { return macroPrecision; }
    public double getMacroRecall() { return macroRecall; }
    public double getMacroF1() { return macroF1; }
    public double getBaselineAccuracy() { return baselineAccuracy; }
    public String getTrainRowsLabel() { return trainRows + " train"; }
    public String getTestRowsLabel() { return testRows + " holdout"; }
    public String getAccuracyPercent() { return percent(accuracy); }
    public String getBaselineAccuracyPercent() { return percent(baselineAccuracy); }
    public String getMacroPrecisionPercent() { return percent(macroPrecision); }
    public String getMacroRecallPercent() { return percent(macroRecall); }
    public String getMacroF1Percent() { return percent(macroF1); }
    public List<String> getLabels() { return labels; }
    public List<ClassMetric> getClasses() { return classes; }
    public List<MatrixRow> getMatrixRows() { return matrixRows; }

    private static String percent(double value) {
        return new java.text.DecimalFormat("0.0%", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.ROOT))
                .format(value);
    }

    public static final class ClassMetric {
        private final String label;
        private final long support;
        private final long predicted;
        private final double precision;
        private final double recall;
        private final double f1;
        private ClassMetric(String label, long support, long predicted,
                            double precision, double recall, double f1) {
            this.label = label; this.support = support; this.predicted = predicted;
            this.precision = precision; this.recall = recall; this.f1 = f1;
        }
        public String getLabel() { return label; }
        public long getSupport() { return support; }
        public long getPredicted() { return predicted; }
        public double getPrecision() { return precision; }
        public double getRecall() { return recall; }
        public double getF1() { return f1; }
        public String getPrecisionPercent() { return percent(precision); }
        public String getRecallPercent() { return percent(recall); }
        public String getF1Percent() { return percent(f1); }
    }

    public static final class MatrixRow {
        private final String actualLabel;
        private final long support;
        private final List<MatrixCell> cells;
        private MatrixRow(String actualLabel, long support, List<MatrixCell> cells) {
            this.actualLabel = actualLabel; this.support = support; this.cells = List.copyOf(cells);
        }
        public String getActualLabel() { return actualLabel; }
        public long getSupport() { return support; }
        public List<MatrixCell> getCells() { return cells; }
    }

    public static final class MatrixCell {
        private final long count;
        private final boolean correct;
        private MatrixCell(long count, boolean correct) { this.count = count; this.correct = correct; }
        public long getCount() { return count; }
        public boolean isCorrect() { return correct; }
    }
}
