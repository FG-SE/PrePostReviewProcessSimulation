package de.unihannover.se.processSimulation.dataGenerator;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.ToDoubleFunction;

import de.unihannover.se.processSimulation.common.ParametersFactory;
import de.unihannover.se.processSimulation.common.ReviewMode;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRunSettings.ExperimentRunParameters;

public class ExperimentRun {

    public enum ExperimentRunSummary {
        UNREALISTIC,
        NO_REVIEW,
        PRE_BETTER_STORY_POINTS,
        POST_BETTER_STORY_POINTS,
        PRE_BETTER_BUGS,
        POST_BETTER_BUGS,
        PRE_BETTER_CYCLE_TIME,
        POST_BETTER_CYCLE_TIME,
        NEGLIGIBLE_DIFFERENCE,
        NOT_SIGNIFICANT
    }

    @FunctionalInterface
    public static interface ExperimentRunner {
        public abstract ExperimentResult runExperiment(
                        final ParametersFactory p, ReviewMode mode, boolean report, String runId);
    }

    public static interface SingleRunCallback {
        public abstract void handleResult(ExperimentResult no, ExperimentResult pre, ExperimentResult post);
    }

    private class CombinedResult {

        private final EnumMap<ReviewMode, ExperimentResult> map = new EnumMap<>(ReviewMode.class);

        public CombinedResult(ExperimentResult no, ExperimentResult pre, ExperimentResult post) {
            if (no != null) {
                this.map.put(ReviewMode.NO_REVIEW, no);
            }
            this.map.put(ReviewMode.PRE_COMMIT, pre);
            this.map.put(ReviewMode.POST_COMMIT, post);
        }

        public ExperimentResult getFor(ReviewMode mode) {
            return this.map.get(mode);
        }

        public double factorPrePost(ToDoubleFunction<ExperimentResult> getter) {
            final double valuePre = getter.applyAsDouble(this.map.get(ReviewMode.PRE_COMMIT));
            final double valuePost = getter.applyAsDouble(this.map.get(ReviewMode.POST_COMMIT));
            final double diff = valuePost - valuePre;
            final double avg = (valuePre + valuePost) / 2;
            if (avg == 0.0) {
                return 0.0;
            }
            return diff / avg;
        }

        public double factorNoReview() {
            final double valueNo = this.map.get(ReviewMode.NO_REVIEW).getFinishedStoryPoints();
            final double valuePre = this.map.get(ReviewMode.PRE_COMMIT).getFinishedStoryPoints();
            final double valuePost = this.map.get(ReviewMode.POST_COMMIT).getFinishedStoryPoints();
            if (valuePre == 0 && valuePost == 0) {
                return 0.0;
            }
            return valueNo / Math.max(valuePre, valuePost);
        }

        public double shareProductiveWork() {
            return Math.max(Math.max(
                            this.shareProductiveWork(ReviewMode.NO_REVIEW),
                            this.shareProductiveWork(ReviewMode.PRE_COMMIT)),
                        this.shareProductiveWork(ReviewMode.POST_COMMIT));
        }

        private double shareProductiveWork(ReviewMode mode) {
            final ExperimentResult r = this.map.get(mode);
            if (r == null) {
                //neutral element of Math.max
                return Double.MIN_VALUE;
            }
            return ((double) r.getFinishedStoryPoints()) / r.getInvestedPersonHours();
        }

    }

    private final ExperimentRunSettings settings;
    private final List<CombinedResult> results = new ArrayList<>();

    /**
     * Constructor hidden, create using {@link #perform}.
     */
    private ExperimentRun(ExperimentRunSettings settings) {
        this.settings = settings;
    }

    private void add(ExperimentResult no, ExperimentResult pre, ExperimentResult post) {
        this.results.add(new CombinedResult(no, pre, post));
    }

    public ExperimentRunSummary getSummary() {
        return this.determineSummary(false);
    }

    private ExperimentRunSummary determineSummary(boolean onlySignificant) {
        final MedianWithConfidenceInterval factorProductiveWork = this.getShareProductiveWork();
        if (factorProductiveWork.smallerThan(this.settings.get(ExperimentRunParameters.LIMIT_UNREALISTIC), onlySignificant)) {
            return ExperimentRunSummary.UNREALISTIC;
        }

        final MedianWithConfidenceInterval factorNoReview = this.getFactorNoReview();
        if (factorNoReview.largerThan(this.settings.get(ExperimentRunParameters.LIMIT_NO_REVIEW), onlySignificant)) {
            return ExperimentRunSummary.NO_REVIEW;
        }

        final MedianWithConfidenceInterval factorStoryPoints = this.getFactorStoryPoints();
        if (factorStoryPoints.smallerThan(-this.settings.get(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_STORY_POINTS), onlySignificant)) {
            return ExperimentRunSummary.PRE_BETTER_STORY_POINTS;
        }
        if (factorStoryPoints.largerThan(this.settings.get(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_STORY_POINTS), onlySignificant)) {
            return ExperimentRunSummary.POST_BETTER_STORY_POINTS;
        }
        if (!factorStoryPoints.largerThan(-this.settings.get(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_STORY_POINTS), onlySignificant)
            || !factorStoryPoints.smallerThan(this.settings.get(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_STORY_POINTS), onlySignificant)) {
            assert onlySignificant;
            return ExperimentRunSummary.NOT_SIGNIFICANT;
        }

        final MedianWithConfidenceInterval factorBugs = this.getFactorBugs();
        if (factorBugs.smallerThan(-this.settings.get(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_BUGS), onlySignificant)) {
            return ExperimentRunSummary.POST_BETTER_BUGS;
        }
        if (factorBugs.largerThan(this.settings.get(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_BUGS), onlySignificant)) {
            return ExperimentRunSummary.PRE_BETTER_BUGS;
        }
        if (!factorBugs.largerThan(-this.settings.get(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_BUGS), onlySignificant)
            || !factorBugs.smallerThan(this.settings.get(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_BUGS), onlySignificant)) {
            assert onlySignificant;
            return ExperimentRunSummary.NOT_SIGNIFICANT;
        }

        final MedianWithConfidenceInterval factorCycleTime = this.getFactorCycleTime();
        if (factorCycleTime.smallerThan(-this.settings.get(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_CYCLE_TIME), onlySignificant)) {
            return ExperimentRunSummary.POST_BETTER_CYCLE_TIME;
        }
        if (factorCycleTime.largerThan(this.settings.get(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_CYCLE_TIME), onlySignificant)) {
            return ExperimentRunSummary.PRE_BETTER_CYCLE_TIME;
        }
        if (!factorCycleTime.largerThan(-this.settings.get(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_CYCLE_TIME), onlySignificant)
            || !factorCycleTime.smallerThan(this.settings.get(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_CYCLE_TIME), onlySignificant)) {
            assert onlySignificant;
            return ExperimentRunSummary.NOT_SIGNIFICANT;
        }

        return ExperimentRunSummary.NEGLIGIBLE_DIFFERENCE;
    }

    public boolean isSummaryStatisticallySignificant() {
        return this.determineSummary(false) == this.determineSummary(true);
    }

    public boolean stillNeedsNoReviewData() {
        final ExperimentRunSummary summaryNoSig = this.determineSummary(false);
        final ExperimentRunSummary summarySig = this.determineSummary(true);
        return this.combineSummariesThatDontNeedNoReview(summaryNoSig) != this.combineSummariesThatDontNeedNoReview(summarySig);
    }

    private ExperimentRunSummary combineSummariesThatDontNeedNoReview(ExperimentRunSummary s) {
        if (s == ExperimentRunSummary.NO_REVIEW
            || s == ExperimentRunSummary.UNREALISTIC) {
            return s;
        } else {
            return ExperimentRunSummary.NEGLIGIBLE_DIFFERENCE;
        }
    }

    public MedianWithConfidenceInterval getFinishedStoryMedian() {
        final MedianWithConfidenceInterval medianNo = this.median(this.getResults(ReviewMode.NO_REVIEW, ExperimentResult::getFinishedStoryCount));
        final MedianWithConfidenceInterval medianPre = this.median(this.getResults(ReviewMode.PRE_COMMIT, ExperimentResult::getFinishedStoryCount));
        final MedianWithConfidenceInterval medianPost = this.median(this.getResults(ReviewMode.POST_COMMIT, ExperimentResult::getFinishedStoryCount));
        if (medianNo.getMedian() > medianPre.getMedian()) {
            if (medianNo.getMedian() > medianPost.getMedian()) {
                return medianNo;
            } else {
                return medianPost;
            }
        } else {
            if (medianPre.getMedian() > medianPost.getMedian()) {
                return medianPre;
            } else {
                return medianPost;
            }
        }
    }

    public MedianWithConfidenceInterval getFinishedStoryPointsMedian(ReviewMode mode) {
        final double[] values = this.getResults(mode, ExperimentResult::getFinishedStoryPoints);
        return this.median(values);
    }

    public MedianWithConfidenceInterval getStoryCycleTimeMeanMedian(ReviewMode mode) {
        final double[] values = this.getResults(mode, ExperimentResult::getStoryCycleTimeMean);
        return this.median(values);
    }

    public MedianWithConfidenceInterval getBugCountMedian(ReviewMode mode) {
        final double[] values = this.getResults(mode, ExperimentResult::getBugCountFoundByCustomers);
        return this.median(values);
    }

    private double[] getResults(ReviewMode mode, ToDoubleFunction<ExperimentResult> getter) {
        return this.results.stream().filter(x -> x.getFor(mode) != null).map(x -> x.getFor(mode)).mapToDouble(getter).toArray();
    }

    /**
     * Returns the median relative difference between the number of finished story points from pre commit and post commit reviews.
     * For every random trial, the factor (post - pre) / avg(post, pre) is calculated.
     * I.e. if the result is > 0, post commit review resulted in more story points.
     */
    public MedianWithConfidenceInterval getFactorStoryPoints() {
        final double[] values = this.results.stream().mapToDouble(x -> x.factorPrePost(ExperimentResult::getFinishedStoryPoints)).toArray();
        return this.median(values);
    }

    /**
     * Returns the median relative difference between the number of remaining bugs from pre commit and post commit reviews.
     * For every random trial, the factor (post - pre) / avg(post, pre) is calculated.
     * I.e. if the result is > 0, post commit review resulted in more remaining bugs.
     */
    public MedianWithConfidenceInterval getFactorBugs() {
        final double[] values = this.results.stream().mapToDouble(x -> x.factorPrePost(ExperimentResult::getBugCountFoundByCustomers)).toArray();
        return this.median(values);
    }

    /**
     * Returns the median relative difference between the mean story cycle time from pre commit and post commit reviews.
     * For every random trial, the factor (post - pre) / avg(post, pre) is calculated.
     * I.e. if the result is > 0, post commit review had a larger cycle time.
     */
    public MedianWithConfidenceInterval getFactorCycleTime() {
        final double[] values = this.results.stream().mapToDouble(x -> x.factorPrePost(ExperimentResult::getStoryCycleTimeMean)).toArray();
        return this.median(values);
    }

    /**
     * Returns the median factor between the story points finished without and with review.
     * For every random trial, the factor no review / max(post, pre) is calculated.
     * I.e. if the result is > 1, no review seems to be better than review
     */
    public MedianWithConfidenceInterval getFactorNoReview() {
        final double[] values = this.results.stream()
                        .filter(x -> x.getFor(ReviewMode.NO_REVIEW) != null)
                        .mapToDouble(x -> x.factorNoReview()).toArray();
        return this.median(values);
    }

    /**
     * Returns the median share of actually reached to maximum possible story points (= person hours spent).
     * For every random trial, the factor max(post, pre, no) / person hours spent is calculated.
     * The result is between 0 and 1. The smaller it is, the less efficient is the team.
     */
    public MedianWithConfidenceInterval getShareProductiveWork() {
        final double[] values = this.results.stream().mapToDouble(x -> x.shareProductiveWork()).toArray();
        return this.median(values);
    }

    private MedianWithConfidenceInterval median(double[] values) {
        return StatisticsUtil.median(values, this.settings.get(ExperimentRunParameters.CONFIDENCE_P));
    }

    public static ExperimentRun perform(
                    ExperimentRunSettings runSettings,
                    ExperimentRunner experimentRunner,
                    BulkParameterFactory initialParameters,
                    SingleRunCallback detailsCallback) {

        final int minRuns = (int) runSettings.get(ExperimentRunParameters.MIN_RUNS);
        final int maxRuns = (int) runSettings.get(ExperimentRunParameters.MAX_RUNS);
        final ExperimentRun result = new ExperimentRun(runSettings);

        BulkParameterFactory f = initialParameters;
        int i = 0;
        while (i < minRuns || (i < maxRuns && result.stillNeedsNoReviewData())) {
            final ExperimentResult no = experimentRunner.runExperiment(f, ReviewMode.NO_REVIEW, false, Integer.toString(i));
            final ExperimentResult pre = experimentRunner.runExperiment(f, ReviewMode.PRE_COMMIT, false, Integer.toString(i));
            final ExperimentResult post = experimentRunner.runExperiment(f, ReviewMode.POST_COMMIT, false, Integer.toString(i));
            result.add(no, pre, post);
            detailsCallback.handleResult(no, pre, post);
            f = f.copyWithChangedSeed();
            i++;
        }

        while (i < maxRuns && !result.isSummaryStatisticallySignificant()) {
            final ExperimentResult pre = experimentRunner.runExperiment(f, ReviewMode.PRE_COMMIT, false, Integer.toString(i));
            final ExperimentResult post = experimentRunner.runExperiment(f, ReviewMode.POST_COMMIT, false, Integer.toString(i));
            result.add(null, pre, post);
            detailsCallback.handleResult(null, pre, post);
            f = f.copyWithChangedSeed();
            i++;
        }

        return result;
    }

}
