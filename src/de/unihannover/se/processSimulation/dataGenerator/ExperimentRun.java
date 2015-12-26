/**
    This file is part of LUH PrePostReview Process Simulation.

    LUH PrePostReview Process Simulation is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    LUH PrePostReview Process Simulation is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with LUH PrePostReview Process Simulation. If not, see <http://www.gnu.org/licenses/>.
 */

package de.unihannover.se.processSimulation.dataGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.ToDoubleFunction;

import de.unihannover.se.processSimulation.common.ParametersFactory;
import de.unihannover.se.processSimulation.common.ReviewMode;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRunSettings.ExperimentRunParameters;

public class ExperimentRun {

    public static class ExperimentRunSummary {
        private final RealismCheckResult realismCheckResult;
        private final ReviewNoReviewComparison noReviewResult;
        private final PrePostComparison storyPointsResult;
        private final PrePostComparison cycleTimeResult;
        private final PrePostComparison bugsResult;

        public ExperimentRunSummary(
                        RealismCheckResult realismCheckResult,
                        ReviewNoReviewComparison noReviewResult,
                        PrePostComparison storyPointsResult,
                        PrePostComparison cycleTimeResult,
                        PrePostComparison bugsResult) {
            this.realismCheckResult = realismCheckResult;
            this.noReviewResult = noReviewResult;
            this.storyPointsResult = storyPointsResult;
            this.cycleTimeResult = cycleTimeResult;
            this.bugsResult = bugsResult;
        }

        public RealismCheckResult getRealismCheckResult() {
            return this.realismCheckResult;
        }

        public ReviewNoReviewComparison getNoReviewResult() {
            return this.noReviewResult;
        }

        public PrePostComparison getStoryPointsResult() {
            return this.storyPointsResult;
        }

        public PrePostComparison getCycleTimeResult() {
            return this.cycleTimeResult;
        }

        public PrePostComparison getBugsResult() {
            return this.bugsResult;
        }

        @Override
        public int hashCode() {
            return this.storyPointsResult.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ExperimentRunSummary)) {
                return false;
            }
            final ExperimentRunSummary e = (ExperimentRunSummary) o;
            return this.bugsResult == e.bugsResult
                && this.cycleTimeResult == e.cycleTimeResult
                && this.storyPointsResult == e.storyPointsResult
                && this.noReviewResult == e.noReviewResult
                && this.realismCheckResult == e.realismCheckResult;
        }

        public static ExperimentRunSummary unrealistic() {
            return new ExperimentRunSummary(
                            RealismCheckResult.UNREALISTIC,
                            ReviewNoReviewComparison.UNREALISTIC,
                            PrePostComparison.UNREALISTIC,
                            PrePostComparison.UNREALISTIC,
                            PrePostComparison.UNREALISTIC);
        }

        public static ExperimentRunSummary noReview() {
            return new ExperimentRunSummary(
                            RealismCheckResult.REALISTIC,
                            ReviewNoReviewComparison.NO_REVIEW,
                            PrePostComparison.NO_REVIEW,
                            PrePostComparison.NO_REVIEW,
                            PrePostComparison.NO_REVIEW);
        }

        public static ExperimentRunSummary review(PrePostComparison summaryStoryPoints, PrePostComparison summaryCycleTime, PrePostComparison summaryBugs) {
            return new ExperimentRunSummary(
                            RealismCheckResult.REALISTIC,
                            ReviewNoReviewComparison.REVIEW,
                            summaryStoryPoints,
                            summaryCycleTime,
                            summaryBugs);
        }
    }

    public enum RealismCheckResult {
        UNREALISTIC,
        REALISTIC,
        NOT_SIGNIFICANT
    }

    public enum ReviewNoReviewComparison {
        UNREALISTIC,
        NO_REVIEW,
        REVIEW,
        NOT_SIGNIFICANT
    }

    public enum PrePostComparison {
        UNREALISTIC,
        NO_REVIEW,
        PRE_BETTER,
        POST_BETTER,
        NEGLIGIBLE_DIFFERENCE,
        NOT_SIGNIFICANT
    }

    @FunctionalInterface
    public static interface ExperimentRunner {
        public abstract ExperimentResult runExperiment(
                        final ParametersFactory p, ReviewMode mode, File resultDir, String runId, int workingDaysForStartup, int workingDaysForMeasurement);
    }

    public static interface SingleRunCallback {
        public abstract void handleResult(ExperimentResult no, ExperimentResult pre, ExperimentResult post);
    }

    private static class CombinedResult {

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
    private int numberOfTrials;

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

    public ExperimentRunSummary getSignificantSummary() {
        return this.determineSummary(true);
    }

    private ExperimentRunSummary determineSummary(boolean onlySignificant) {
        final MedianWithConfidenceInterval factorProductiveWork = this.getShareProductiveWork();
        if (factorProductiveWork.smallerThan(this.settings.get(ExperimentRunParameters.LIMIT_UNREALISTIC), onlySignificant)) {
            return ExperimentRunSummary.unrealistic();
        }

        final MedianWithConfidenceInterval factorNoReview = this.getFactorNoReview();
        if (factorNoReview.largerThan(this.settings.get(ExperimentRunParameters.LIMIT_NO_REVIEW), onlySignificant)) {
            return ExperimentRunSummary.noReview();
        }

        final PrePostComparison summaryStoryPoints = this.comparePrePost(
                        this.getFactorStoryPoints(),
                        this.settings.get(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_STORY_POINTS),
                        onlySignificant,
                        true);
        final PrePostComparison summaryCycleTime = this.comparePrePost(
                        this.getFactorCycleTime(),
                        this.settings.get(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_CYCLE_TIME),
                        onlySignificant,
                        false);
        final PrePostComparison summaryBugs = this.comparePrePost(
                        this.getFactorBugs(),
                        this.settings.get(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_BUGS),
                        onlySignificant,
                        false);

        return ExperimentRunSummary.review(summaryStoryPoints, summaryCycleTime, summaryBugs);
    }

    private PrePostComparison comparePrePost(MedianWithConfidenceInterval factorToCompare, double limit, boolean onlySignificant, boolean smallerMeansPre) {
        if (factorToCompare.smallerThan(-limit, onlySignificant)) {
            return smallerMeansPre ? PrePostComparison.PRE_BETTER : PrePostComparison.POST_BETTER;
        }
        if (factorToCompare.largerThan(limit, onlySignificant)) {
            return smallerMeansPre ? PrePostComparison.POST_BETTER : PrePostComparison.PRE_BETTER;
        }
        if (!factorToCompare.largerThan(-limit, onlySignificant)
            || !factorToCompare.smallerThan(limit, onlySignificant)) {
            assert onlySignificant;
            return PrePostComparison.NOT_SIGNIFICANT;
        }
        return PrePostComparison.NEGLIGIBLE_DIFFERENCE;
    }

    public boolean isSummaryStatisticallySignificant() {
        return this.determineSummary(false).equals(this.determineSummary(true));
    }

    private boolean stillNeedsNoReviewData() {
        final ExperimentRunSummary summaryNoSig = this.determineSummary(false);
        final ExperimentRunSummary summarySig = this.determineSummary(true);
        return summaryNoSig.getNoReviewResult() != summarySig.getNoReviewResult();
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
        final double[] values = this.getResults(mode, ExperimentResult::getStoryCycleTimeMeanWithDefault);
        return this.median(values);
    }

    public MedianWithConfidenceInterval getBugCountMedian(ReviewMode mode) {
        final double[] values = this.getResults(mode, ExperimentResult::getBugCountFoundByCustomers);
        return this.median(values);
    }

    public MedianWithConfidenceInterval getBugCountPerStoryPointMedian(ReviewMode mode) {
        final double[] values = this.getResults(mode, ExperimentResult::getBugCountFoundByCustomersPerStoryPoint);
        return this.median(values);
    }

    public MedianWithConfidenceInterval getWastedTimeTaskSwitchMedian(ReviewMode mode) {
        final double[] values = this.getResults(mode, ExperimentResult::getWastedTimeTaskSwitch);
        return this.median(values);
    }

    public MedianWithConfidenceInterval getConflictCountMedian(ReviewMode mode) {
        final double[] values = this.getResults(mode, ExperimentResult::getConflictCount);
        return this.median(values);
    }

    public MedianWithConfidenceInterval getGlobalBugCountMedian(ReviewMode mode) {
        final double[] values = this.getResults(mode, ExperimentResult::getGlobalBugCount);
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
        final double[] values = this.results.stream().mapToDouble(x -> x.factorPrePost(ExperimentResult::getBugCountFoundByCustomersPerStoryPoint)).toArray();
        return this.median(values);
    }

    /**
     * Returns the median relative difference between the mean story cycle time from pre commit and post commit reviews.
     * For every random trial, the factor (post - pre) / avg(post, pre) is calculated.
     * I.e. if the result is > 0, post commit review had a larger cycle time.
     */
    public MedianWithConfidenceInterval getFactorCycleTime() {
        final double[] values = this.results.stream().mapToDouble(x -> x.factorPrePost(ExperimentResult::getStoryCycleTimeMeanWithDefault)).toArray();
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

    /**
     * Returns the number of runs that were executed to reach the results (with sufficient statistical significance).
     */
    public int getNumberOfTrials() {
        return this.numberOfTrials;
    }

    public int getCountFinishedStoryPointsPreLarger() {
        return (int) this.results.stream().filter(
                        x -> x.getFor(ReviewMode.PRE_COMMIT).getFinishedStoryPoints() > x.getFor(ReviewMode.POST_COMMIT).getFinishedStoryPoints()
                    ).count();
    }

    public int getCountBugCountPerStoryPointPreLarger() {
        return (int) this.results.stream().filter(
                        x -> x.getFor(ReviewMode.PRE_COMMIT).getBugCountFoundByCustomersPerStoryPoint() > x.getFor(ReviewMode.POST_COMMIT).getBugCountFoundByCustomersPerStoryPoint()
                    ).count();
    }

    public int getCountCycleTimePreLarger() {
        return (int) this.results.stream().filter(
                        x -> x.getFor(ReviewMode.PRE_COMMIT).getStoryCycleTimeMeanWithDefault() > x.getFor(ReviewMode.POST_COMMIT).getStoryCycleTimeMeanWithDefault()
                    ).count();
    }

    public static ExperimentRun perform(
                    ExperimentRunSettings runSettings,
                    ExperimentRunner experimentRunner,
                    BulkParameterFactory initialParameters,
                    SingleRunCallback detailsCallback) {

        final int minRuns = (int) runSettings.get(ExperimentRunParameters.MIN_RUNS);
        final int maxRuns = (int) runSettings.get(ExperimentRunParameters.MAX_RUNS);
        final int daysForStartup = (int) runSettings.get(ExperimentRunParameters.WORKING_DAYS_FOR_STARTUP);
        final int daysForMeasurement = (int) runSettings.get(ExperimentRunParameters.WORKING_DAYS_FOR_MEASUREMENT);
        final ExperimentRun result = new ExperimentRun(runSettings);

        BulkParameterFactory f = initialParameters;
        int i = 0;
        while (i < minRuns || (i < maxRuns && result.stillNeedsNoReviewData())) {
            final ExperimentResult no = experimentRunner.runExperiment(f, ReviewMode.NO_REVIEW, null, Integer.toString(i), daysForStartup, daysForMeasurement);
            final ExperimentResult pre = experimentRunner.runExperiment(f, ReviewMode.PRE_COMMIT, null, Integer.toString(i), daysForStartup, daysForMeasurement);
            final ExperimentResult post = experimentRunner.runExperiment(f, ReviewMode.POST_COMMIT, null, Integer.toString(i), daysForStartup, daysForMeasurement);
            if (no.hadError() || pre.hadError() || post.hadError()) {
                throw new RuntimeException("Had an error in run " + i);
            }
            result.add(no, pre, post);
            detailsCallback.handleResult(no, pre, post);
            f = f.copyWithChangedSeed();
            i++;
        }

        while (i < maxRuns && !result.isSummaryStatisticallySignificant()) {
            final ExperimentResult pre = experimentRunner.runExperiment(f, ReviewMode.PRE_COMMIT, null, Integer.toString(i), daysForStartup, daysForMeasurement);
            final ExperimentResult post = experimentRunner.runExperiment(f, ReviewMode.POST_COMMIT, null, Integer.toString(i), daysForStartup, daysForMeasurement);
            if (pre.hadError() || post.hadError()) {
                throw new RuntimeException("Had an error in run " + i);
            }
            result.add(null, pre, post);
            detailsCallback.handleResult(null, pre, post);
            f = f.copyWithChangedSeed();
            i++;
        }
        result.numberOfTrials = i;

        return result;
    }

}
