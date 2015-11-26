package de.unihannover.se.processSimulation.dataGenerator;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import de.unihannover.se.processSimulation.common.ReviewMode;

public class ExperimentRun {

    public enum ExperimentRunSummary {
        UNREALISTIC,
        NO_REVIEW,
        NEGLIGIBLE_DIFFERENCE,
        PRE_BETTER,
        POST_BETTER
    }

    public static interface SingleRunCallback {
        public abstract void handleResult(ExperimentResult no, ExperimentResult pre, ExperimentResult post);
    }

    private class CombinedResult {

        private final EnumMap<ReviewMode, ExperimentResult> map = new EnumMap<>(ReviewMode.class);

        public CombinedResult(ExperimentResult no, ExperimentResult pre, ExperimentResult post) {
            this.map.put(ReviewMode.NO_REVIEW, no);
            this.map.put(ReviewMode.PRE_COMMIT, pre);
            this.map.put(ReviewMode.POST_COMMIT, post);
        }

        public ExperimentResult getFor(ReviewMode mode) {
            return this.map.get(mode);
        }

    }

    private static final double CONFIDENCE_P = 0.05;

    private final List<CombinedResult> results = new ArrayList<>();

    /**
     * Constructor hidden, create using {@link #perform}.
     */
    private ExperimentRun() {
    }

    private void add(ExperimentResult no, ExperimentResult pre, ExperimentResult post) {
        this.results.add(new CombinedResult(no, pre, post));
    }

    public ExperimentRunSummary getSummary() {
        // TODO Auto-generated method stub
        return ExperimentRunSummary.UNREALISTIC;
    }

    public boolean isSummaryStatisticallySignificant() {
        // TODO Auto-generated method stub
        return false;
    }

    public String getFinishedStoryMedian() {
        // TODO Auto-generated method stub
        return null;
    }

    public MedianWithConfidenceInterval getFinishedStoryPointsMedian(ReviewMode mode) {
        final double[] values = this.results.stream().mapToDouble(x -> x.getFor(mode).getFinishedStoryPoints()).toArray();
        return StatisticsUtil.median(values, CONFIDENCE_P);
    }

    public MedianWithConfidenceInterval getStoryCycleTimeMeanMedian(ReviewMode mode) {
        final double[] values = this.results.stream().mapToDouble(x -> x.getFor(mode).getStoryCycleTimeMean()).toArray();
        return StatisticsUtil.median(values, CONFIDENCE_P);
    }

    public MedianWithConfidenceInterval getRemainingBugCountMedian(ReviewMode mode) {
        final double[] values = this.results.stream().mapToDouble(x -> x.getFor(mode).getRemainingBugCount()).toArray();
        return StatisticsUtil.median(values, CONFIDENCE_P);
    }

    public String getFactorStoryPoints() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getFactorBugs() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getFactorCycleTime() {
        // TODO Auto-generated method stub
        return null;
    }


    public static ExperimentRun perform(
                    BulkParameterFactory initialParameters, int minRuns, int maxRuns, SingleRunCallback detailsCallback) {

        final ExperimentRun result = new ExperimentRun();

        BulkParameterFactory f = initialParameters;
        int i = 0;
        while (i < minRuns) {
            final ExperimentResult no = DataGenerator.runExperiment(f, ReviewMode.NO_REVIEW, false, Integer.toString(i));
            final ExperimentResult pre = DataGenerator.runExperiment(f, ReviewMode.PRE_COMMIT, false, Integer.toString(i));
            final ExperimentResult post = DataGenerator.runExperiment(f, ReviewMode.POST_COMMIT, false, Integer.toString(i));
            result.add(no, pre, post);
            detailsCallback.handleResult(no, pre, post);
            f = f.copyWithChangedSeed();
            i++;
        }

        // TODO Auto-generated method stub

        return result;
    }

}
