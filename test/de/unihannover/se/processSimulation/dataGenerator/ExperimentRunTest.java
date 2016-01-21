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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import co.paralleluniverse.common.util.Pair;
import de.unihannover.se.processSimulation.common.ParametersFactory;
import de.unihannover.se.processSimulation.common.ReviewMode;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRun.ExperimentRunner;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRun.PrePostComparison;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRun.SingleRunCallback;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRunSettings.ExperimentRunParameters;

public class ExperimentRunTest {

    public static final class StubExperiments implements ExperimentRunner {

        private final Map<Pair<String, ReviewMode>, ExperimentResult> data = new HashMap<>();

        public void put(String runId, ReviewMode mode, ExperimentResult result) {
            this.data.put(new Pair<String, ReviewMode>(runId, mode), result);
        }

        @Override
        public ExperimentResult runExperiment(ParametersFactory p, ReviewMode mode, File resultDir, String runId, int daysForStartup, int daysForMeasurement) {
            return this.data.get(new Pair<String, ReviewMode>(runId, mode));
        }

    }

    private static ExperimentResult result(
                    int finishedStoryPoints,
                    double storyCycleTimeMean,
                    int finishedStoryCount,
                    int bugCountFoundByCustomers) {
        return new ExperimentResult(
                        finishedStoryPoints,
                        storyCycleTimeMean,
                        1,
                        42,
                        finishedStoryCount,
                        bugCountFoundByCustomers,
                        finishedStoryPoints * 2,
                        finishedStoryPoints * 3,
                        1,
                        2,
                        3,
                        11.1,
                        11.2,
                        11.3,
                        11.4,
                        11.5,
                        11.6,
                        12.1,
                        12.2,
                        12.3,
                        12.4,
                        12.5,
                        12.6,
                        1.2,
                        2.1,
                        2.2,
                        3.1,
                        3.2,
                        3.3,
                        23,
                        false);
    }

    private static MedianWithConfidenceInterval median(double median, double lower, double upper) {
        return new MedianWithConfidenceInterval(median, lower, upper);
    }

    private static ExperimentRunSettings fixedRunCountSettings(int runCount) {
        return ExperimentRunSettings.defaultSettings()
                        .copyWithChangedParam(ExperimentRunParameters.MIN_RUNS, runCount)
                        .copyWithChangedParam(ExperimentRunParameters.MAX_RUNS, runCount);
    }

    @Test
    public void testStatistics() {
        final BulkParameterFactory f = BulkParameterFactory.forCommercial();
        final StubExperiments stub = new StubExperiments();

        stub.put("0", ReviewMode.NO_REVIEW, result(1, 0.5, 1, 0));
        stub.put("1", ReviewMode.NO_REVIEW, result(2, 1.5, 1, 0));
        stub.put("2", ReviewMode.NO_REVIEW, result(3, 2.5, 1, 1));
        stub.put("3", ReviewMode.NO_REVIEW, result(4, 0.5, 2, 1));
        stub.put("4", ReviewMode.NO_REVIEW, result(5, 1.5, 2, 2));
        stub.put("5", ReviewMode.NO_REVIEW, result(6, 2.5, 2, 2));
        stub.put("6", ReviewMode.NO_REVIEW, result(7, 0.5, 3, 3));
        stub.put("7", ReviewMode.NO_REVIEW, result(8, 1.5, 3, 3));
        stub.put("8", ReviewMode.NO_REVIEW, result(9, 2.5, 3, 4));
        stub.put("9", ReviewMode.NO_REVIEW, result(10, 0.5, 4, 4));
        stub.put("10", ReviewMode.NO_REVIEW, result(11, 1.5, 4, 5));

        stub.put("0", ReviewMode.PRE_COMMIT, result(11, 10.5, 11, 10));
        stub.put("1", ReviewMode.PRE_COMMIT, result(12, 11.5, 11, 10));
        stub.put("2", ReviewMode.PRE_COMMIT, result(13, 12.5, 11, 11));
        stub.put("3", ReviewMode.PRE_COMMIT, result(14, 10.5, 12, 11));
        stub.put("4", ReviewMode.PRE_COMMIT, result(15, 11.5, 12, 12));
        stub.put("5", ReviewMode.PRE_COMMIT, result(16, 12.5, 12, 12));
        stub.put("6", ReviewMode.PRE_COMMIT, result(17, 10.5, 13, 13));
        stub.put("7", ReviewMode.PRE_COMMIT, result(18, 11.5, 13, 13));
        stub.put("8", ReviewMode.PRE_COMMIT, result(19, 12.5, 13, 14));
        stub.put("9", ReviewMode.PRE_COMMIT, result(20, 10.5, 14, 14));
        stub.put("10", ReviewMode.PRE_COMMIT, result(21, 11.5, 14, 15));

        stub.put("0", ReviewMode.POST_COMMIT, result(21, 20.5, 21, 20));
        stub.put("1", ReviewMode.POST_COMMIT, result(22, 21.5, 21, 20));
        stub.put("2", ReviewMode.POST_COMMIT, result(23, 22.5, 21, 21));
        stub.put("3", ReviewMode.POST_COMMIT, result(24, 20.5, 22, 21));
        stub.put("4", ReviewMode.POST_COMMIT, result(25, 21.5, 22, 22));
        stub.put("5", ReviewMode.POST_COMMIT, result(26, 22.5, 22, 22));
        stub.put("6", ReviewMode.POST_COMMIT, result(27, 20.5, 23, 23));
        stub.put("7", ReviewMode.POST_COMMIT, result(28, 21.5, 23, 23));
        stub.put("8", ReviewMode.POST_COMMIT, result(29, 22.5, 23, 24));
        stub.put("9", ReviewMode.POST_COMMIT, result(30, 20.5, 24, 24));
        stub.put("10", ReviewMode.POST_COMMIT, result(31, 21.5, 24, 25));

        final ExperimentRunSettings settings = fixedRunCountSettings(11)
                        .copyWithChangedParam(ExperimentRunParameters.CONFIDENCE_P, 0.05)
                        .copyWithChangedParam(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_BUGS, 0.05);
        final ExperimentRun result = ExperimentRun.perform(settings, stub, f, dummyCallback());

        assertEquals(median(6, 2, 10), result.getFinishedStoryPointsMedian(ReviewMode.NO_REVIEW));
        assertEquals(median(16, 12, 20), result.getFinishedStoryPointsMedian(ReviewMode.PRE_COMMIT));
        assertEquals(median(26, 22, 30), result.getFinishedStoryPointsMedian(ReviewMode.POST_COMMIT));

        assertEquals(median(1.5, 0.5, 2.5), result.getStoryCycleTimeMeanMedian(ReviewMode.NO_REVIEW));
        assertEquals(median(11.5, 10.5, 12.5), result.getStoryCycleTimeMeanMedian(ReviewMode.PRE_COMMIT));
        assertEquals(median(21.5, 20.5, 22.5), result.getStoryCycleTimeMeanMedian(ReviewMode.POST_COMMIT));

        assertEquals(median(2, 0, 4), result.getIssueCountMedian(ReviewMode.NO_REVIEW));
        assertEquals(median(12, 10, 14), result.getIssueCountMedian(ReviewMode.PRE_COMMIT));
        assertEquals(median(22, 20, 24), result.getIssueCountMedian(ReviewMode.POST_COMMIT));

        assertEquals(median(22, 21, 24), result.getFinishedStoryMedian());

        assertEquals(median(10/21.0, 10/25.0, 10/17.0), result.getFactorStoryPoints());
        assertEquals(median(10/16.5, 10/17.5, 10/15.5), result.getFactorCycleTime());
        assertEquals(median(0.097087, 0.067114, 0.116009), result.getFactorIssues());

        assertEquals(PrePostComparison.POST_BETTER, result.getSummary().getStoryPointsResult());
        assertTrue(result.isSummaryStatisticallySignificant());
    }

    private static SingleRunCallback dummyCallback() {
        return new SingleRunCallback() {
            @Override
            public void handleResult(ExperimentResult no, ExperimentResult pre, ExperimentResult post) {
            }
        };
    }

    @Test
    public void testInsignificantDifference() {
        final BulkParameterFactory f = BulkParameterFactory.forCommercial();
        final StubExperiments stub = new StubExperiments();

        stub.put("0", ReviewMode.NO_REVIEW, result(100, 1, 1, 1));
        stub.put("1", ReviewMode.NO_REVIEW, result(100, 1, 1, 1));
        stub.put("2", ReviewMode.NO_REVIEW, result(100, 1, 1, 1));
        stub.put("3", ReviewMode.NO_REVIEW, result(200, 1, 1, 2));
        stub.put("4", ReviewMode.NO_REVIEW, result(200, 1, 1, 2));
        stub.put("5", ReviewMode.NO_REVIEW, result(200, 1, 1, 2));
        stub.put("6", ReviewMode.NO_REVIEW, result(300, 1, 1, 3));
        stub.put("7", ReviewMode.NO_REVIEW, result(300, 1, 1, 3));
        stub.put("8", ReviewMode.NO_REVIEW, result(300, 1, 1, 3));

        stub.put("0", ReviewMode.PRE_COMMIT, result(101, 1, 1, 10));
        stub.put("1", ReviewMode.PRE_COMMIT, result(101, 1, 1, 10));
        stub.put("2", ReviewMode.PRE_COMMIT, result(101, 1, 1, 10));
        stub.put("3", ReviewMode.PRE_COMMIT, result(201, 1, 1, 20));
        stub.put("4", ReviewMode.PRE_COMMIT, result(201, 1, 1, 20));
        stub.put("5", ReviewMode.PRE_COMMIT, result(201, 1, 1, 20));
        stub.put("6", ReviewMode.PRE_COMMIT, result(301, 1, 1, 30));
        stub.put("7", ReviewMode.PRE_COMMIT, result(301, 1, 1, 30));
        stub.put("8", ReviewMode.PRE_COMMIT, result(301, 1, 1, 30));

        stub.put("0", ReviewMode.POST_COMMIT, result(111, 1, 1, 11));
        stub.put("1", ReviewMode.POST_COMMIT, result(111, 1, 1, 11));
        stub.put("2", ReviewMode.POST_COMMIT, result(111, 1, 1, 11));
        stub.put("3", ReviewMode.POST_COMMIT, result(211, 1, 1, 21));
        stub.put("4", ReviewMode.POST_COMMIT, result(211, 1, 1, 21));
        stub.put("5", ReviewMode.POST_COMMIT, result(211, 1, 1, 21));
        stub.put("6", ReviewMode.POST_COMMIT, result(311, 1, 1, 31));
        stub.put("7", ReviewMode.POST_COMMIT, result(311, 1, 1, 31));
        stub.put("8", ReviewMode.POST_COMMIT, result(311, 1, 1, 31));

        final ExperimentRunSettings settings4 = fixedRunCountSettings(9)
                        .copyWithChangedParam(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_STORY_POINTS, 0.04);
        final ExperimentRunSettings settings2 = fixedRunCountSettings(9)
                        .copyWithChangedParam(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_STORY_POINTS, 0.02);
        final ExperimentRunSettings settings10 = fixedRunCountSettings(9)
                        .copyWithChangedParam(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_STORY_POINTS, 0.10);

        final ExperimentRun result4 = ExperimentRun.perform(settings4, stub, f, dummyCallback());

        assertEquals(median(200, 100, 300), result4.getFinishedStoryPointsMedian(ReviewMode.NO_REVIEW));
        assertEquals(median(201, 101, 301), result4.getFinishedStoryPointsMedian(ReviewMode.PRE_COMMIT));
        assertEquals(median(211, 111, 311), result4.getFinishedStoryPointsMedian(ReviewMode.POST_COMMIT));

        assertEquals(median(10/206.0, 10/306.0, 10/106.0), result4.getFactorStoryPoints());

        assertEquals(PrePostComparison.POST_BETTER, result4.getSummary().getStoryPointsResult());
        assertFalse(result4.isSummaryStatisticallySignificant());

        //with smaller limit, result becomes significant
        final ExperimentRun result2 = ExperimentRun.perform(settings2, stub, f, dummyCallback());
        assertEquals(result4.getFactorStoryPoints(), result2.getFactorStoryPoints());
        assertEquals(PrePostComparison.POST_BETTER, result2.getSummary().getStoryPointsResult());
        assertTrue(result2.isSummaryStatisticallySignificant());

        //with larger limit, result becomes significant too (but with different outcome)
        final ExperimentRun result10 = ExperimentRun.perform(settings10, stub, f, dummyCallback());
        assertEquals(result4.getFactorStoryPoints(), result10.getFactorStoryPoints());
        assertEquals(PrePostComparison.NEGLIGIBLE_DIFFERENCE, result10.getSummary().getStoryPointsResult());
        assertTrue(result10.isSummaryStatisticallySignificant());
    }

    @Test
    public void testInsignificantNegligibleDifference() {
        final BulkParameterFactory f = BulkParameterFactory.forCommercial();
        final StubExperiments stub = new StubExperiments();

        stub.put("0", ReviewMode.NO_REVIEW, result(10, 1, 1, 1));
        stub.put("1", ReviewMode.NO_REVIEW, result(10, 1, 1, 1));
        stub.put("2", ReviewMode.NO_REVIEW, result(10, 1, 1, 1));
        stub.put("3", ReviewMode.NO_REVIEW, result(10, 1, 1, 1));
        stub.put("4", ReviewMode.NO_REVIEW, result(10, 1, 1, 1));
        stub.put("5", ReviewMode.NO_REVIEW, result(10, 1, 1, 1));
        stub.put("6", ReviewMode.NO_REVIEW, result(10, 1, 1, 1));
        stub.put("7", ReviewMode.NO_REVIEW, result(10, 1, 1, 1));
        stub.put("8", ReviewMode.NO_REVIEW, result(10, 1, 1, 1));

        stub.put("0", ReviewMode.PRE_COMMIT, result(100, 10, 1, 1));
        stub.put("1", ReviewMode.PRE_COMMIT, result(100, 10, 1, 1));
        stub.put("2", ReviewMode.PRE_COMMIT, result(100, 10, 1, 1));
        stub.put("3", ReviewMode.PRE_COMMIT, result(200, 10, 1, 1));
        stub.put("4", ReviewMode.PRE_COMMIT, result(200, 10, 1, 1));
        stub.put("5", ReviewMode.PRE_COMMIT, result(200, 10, 1, 1));
        stub.put("6", ReviewMode.PRE_COMMIT, result(300, 10, 1, 1));
        stub.put("7", ReviewMode.PRE_COMMIT, result(300, 10, 1, 1));
        stub.put("8", ReviewMode.PRE_COMMIT, result(300, 10, 1, 1));

        stub.put("0", ReviewMode.POST_COMMIT, result(300, 1, 1, 1));
        stub.put("1", ReviewMode.POST_COMMIT, result(300, 1, 1, 1));
        stub.put("2", ReviewMode.POST_COMMIT, result(300, 1, 1, 1));
        stub.put("3", ReviewMode.POST_COMMIT, result(200, 1, 1, 1));
        stub.put("4", ReviewMode.POST_COMMIT, result(200, 1, 1, 1));
        stub.put("5", ReviewMode.POST_COMMIT, result(200, 1, 1, 1));
        stub.put("6", ReviewMode.POST_COMMIT, result(100, 1, 1, 1));
        stub.put("7", ReviewMode.POST_COMMIT, result(100, 1, 1, 1));
        stub.put("8", ReviewMode.POST_COMMIT, result(100, 1, 1, 1));

        final ExperimentRun result = ExperimentRun.perform(fixedRunCountSettings(9), stub, f, dummyCallback());

        assertEquals(median(10, 10, 10), result.getFinishedStoryPointsMedian(ReviewMode.NO_REVIEW));
        assertEquals(median(200, 100, 300), result.getFinishedStoryPointsMedian(ReviewMode.PRE_COMMIT));
        assertEquals(median(200, 100, 300), result.getFinishedStoryPointsMedian(ReviewMode.POST_COMMIT));

        assertEquals(median(0.0, -1.0, 1.0), result.getFactorStoryPoints());

        assertEquals(PrePostComparison.NEGLIGIBLE_DIFFERENCE, result.getSummary().getStoryPointsResult());
        assertFalse(result.isSummaryStatisticallySignificant());
    }
}
