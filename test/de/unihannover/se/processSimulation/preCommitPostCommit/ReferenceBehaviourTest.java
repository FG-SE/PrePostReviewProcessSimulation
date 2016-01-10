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

package de.unihannover.se.processSimulation.preCommitPostCommit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;

import de.unihannover.se.processSimulation.common.ParametersFactory;
import de.unihannover.se.processSimulation.common.ReviewMode;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory.ParameterType;
import desmoj.core.simulator.CoroutineModel;
import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.TimeInstant;

public class ReferenceBehaviourTest {

    static {
        Experiment.setCoroutineModel(CoroutineModel.FIBERS);
    }

    private static PrePostModel runExperiment(ParametersFactory p, ReviewMode mode) throws Exception {
        final int hoursToReset = 8 * 400;
        final PrePostModel model = new PrePostModel("RealProcessingModel", mode, p, false, hoursToReset);
        final ArrayList<String> noOutputs = new ArrayList<>();
        final Experiment exp = new Experiment("UnitTest" + mode + "_" + p.hashCode(),
                        ".", null, noOutputs, noOutputs, noOutputs, noOutputs);
        exp.setSeedGenerator(p.getSeed());
        model.connectToExperiment(exp);

        exp.setSilent(true);
        exp.getOutputPath();
        exp.setShowProgressBar(false);
        exp.stop(new TimeInstant(hoursToReset + 8 * 600, TimeUnit.HOURS));
        exp.start();
        exp.finish();

        return model;
    }

    private static<T extends Comparable<T>> T runExperimentAndGetMedianResult(
                    BulkParameterFactory p, ReviewMode mode, Function<PrePostModel, T> getter) throws Exception {

        final List<T> results = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            final PrePostModel model = runExperiment(p, mode);
            p = p.copyWithChangedSeed();
            results.add(getter.apply(model));
        }
        Collections.sort(results);
        return results.get(results.size() / 2);
    }

    private static Matcher<Long> isSimilarTo(long finishedStoryPoints) {
        return new CustomTypeSafeMatcher<Long>("is similar to " + finishedStoryPoints) {
            @Override
            protected boolean matchesSafely(Long arg0) {
                return Math.abs(arg0 - finishedStoryPoints) <= 0.005 * 0.5 * (arg0 + finishedStoryPoints);
            }
        };
    }

    private static Matcher<Long> isSignificantlyLargerThan(long finishedStoryPoints) {
        return new CustomTypeSafeMatcher<Long>("is significantly larger than " + finishedStoryPoints) {
            @Override
            protected boolean matchesSafely(Long arg0) {
                return Math.abs(arg0 - finishedStoryPoints) > 0.005 * 0.5 * (arg0 + finishedStoryPoints);
            }
        };
    }

    private static Matcher<Double> isSignificantlyLargerThan(double storyCycleTimeMean) {
        return new CustomTypeSafeMatcher<Double>("is significantly larger than " + storyCycleTimeMean) {
            @Override
            protected boolean matchesSafely(Double arg0) {
                return Math.abs(arg0 - storyCycleTimeMean) > 0.005 * 0.5 * (arg0 + storyCycleTimeMean);
            }
        };
    }

    @Test
    public void testWhenAllRelevantEffectsAreOffThereIsNoDifference() throws Exception {
        final BulkParameterFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_TRIANGLE_WIDTH, 0.0)
                        .copyWithChangedParam(ParameterType.NUMBER_OF_DEVELOPERS, 2)
                        .copyWithChangedParam(ParameterType.GLOBAL_ISSUE_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.GLOBAL_ISSUE_TRIANGLE_WIDTH, 0.0)
                        .copyWithChangedParam(ParameterType.CONFLICT_PROBABILITY, 0.0)
                        .copyWithChangedParam(ParameterType.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, 0.0)
                        .copyWithChangedParam(ParameterType.MAX_TASK_SWITCH_OVERHEAD, 0.0)
                        .copyWithChangedParam(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.NO_SUBDIVISION);
        final Long modelPre = runExperimentAndGetMedianResult(p, ReviewMode.PRE_COMMIT, PrePostModel::getFinishedStoryPoints);
        final Long modelPost = runExperimentAndGetMedianResult(p, ReviewMode.POST_COMMIT, PrePostModel::getFinishedStoryPoints);
        assertThat(modelPre, isSimilarTo(modelPost));
    }

    @Test
    public void testWithLotsOfDependenciesPreCommitHasHigherCycleTime() throws Exception {
        final BulkParameterFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.NUMBER_OF_DEVELOPERS, 2)
                        .copyWithChangedParam(ParameterType.GLOBAL_ISSUE_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.CONFLICT_PROBABILITY, 0.0)
                        .copyWithChangedParam(ParameterType.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, 0.0)
                        .copyWithChangedParam(ParameterType.MAX_TASK_SWITCH_OVERHEAD, 0.0)
                        .copyWithChangedParam(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.CHAINS);
        final Double modelPre = runExperimentAndGetMedianResult(p, ReviewMode.PRE_COMMIT, PrePostModel::getStoryCycleTimeMean);
        final Double modelPost = runExperimentAndGetMedianResult(p, ReviewMode.POST_COMMIT, PrePostModel::getStoryCycleTimeMean);
        assertThat(modelPre, isSignificantlyLargerThan(modelPost));
    }

    @Test
    public void testLotsOfGlobalBugsMakePreCommitBetter() throws Exception {
        final BulkParameterFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.NUMBER_OF_DEVELOPERS, 2)
                        .copyWithChangedParam(ParameterType.GLOBAL_ISSUE_MODE, 1.0)
                        .copyWithChangedParam(ParameterType.GLOBAL_ISSUE_SUSPEND_TIME_MODE, 10.0)
                        .copyWithChangedParam(ParameterType.CONFLICT_PROBABILITY, 0.0)
                        .copyWithChangedParam(ParameterType.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, 0.0)
                        .copyWithChangedParam(ParameterType.MAX_TASK_SWITCH_OVERHEAD, 0.0)
                        .copyWithChangedParam(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.NO_DEPENDENCIES);
        final Long modelPre = runExperimentAndGetMedianResult(p, ReviewMode.PRE_COMMIT, PrePostModel::getFinishedStoryPoints);
        final Long modelPost = runExperimentAndGetMedianResult(p, ReviewMode.POST_COMMIT, PrePostModel::getFinishedStoryPoints);
        assertThat(modelPre, isSignificantlyLargerThan(modelPost));
    }

    @Test
    public void testLotsOfConflictsMakePostCommitBetter() throws Exception {
        final BulkParameterFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.NUMBER_OF_DEVELOPERS, 10)
                        .copyWithChangedParam(ParameterType.GLOBAL_ISSUE_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.CONFLICT_PROBABILITY, 1.0)
                        .copyWithChangedParam(ParameterType.CONFLICT_RESOLUTION_TIME_MODE, 99.0)
                        .copyWithChangedParam(ParameterType.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, 0.0)
                        .copyWithChangedParam(ParameterType.MAX_TASK_SWITCH_OVERHEAD, 0.0)
                        .copyWithChangedParam(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.NO_DEPENDENCIES);
        final Long modelPre = runExperimentAndGetMedianResult(p, ReviewMode.PRE_COMMIT, PrePostModel::getFinishedStoryPoints);
        final Long modelPost = runExperimentAndGetMedianResult(p, ReviewMode.POST_COMMIT, PrePostModel::getFinishedStoryPoints);
        assertThat(modelPost, isSignificantlyLargerThan(modelPre));
    }

    @Test
    public void testHighTaskSwitchOverheadAndLotsOfDependenciesMakePostCommitBetter() throws Exception {
        final BulkParameterFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.NUMBER_OF_DEVELOPERS, 2)
                        .copyWithChangedParam(ParameterType.GLOBAL_ISSUE_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.CONFLICT_PROBABILITY, 1.0)
                        .copyWithChangedParam(ParameterType.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, 1.0)
                        .copyWithChangedParam(ParameterType.MAX_TASK_SWITCH_OVERHEAD, 2.0)
                        .copyWithChangedParam(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.SIMPLISTIC);
        final Long modelPre = runExperimentAndGetMedianResult(p, ReviewMode.PRE_COMMIT, PrePostModel::getFinishedStoryPoints);
        final Long modelPost = runExperimentAndGetMedianResult(p, ReviewMode.POST_COMMIT, PrePostModel::getFinishedStoryPoints);
        assertThat(modelPost, isSignificantlyLargerThan(modelPre));
    }


    @Test
    public void testNoReviewIsWorseThanReview() throws Exception {
        final BulkParameterFactory p = BulkParameterFactory
                        .forCommercial();
        final Long modelPre = runExperimentAndGetMedianResult(p, ReviewMode.PRE_COMMIT, PrePostModel::getFinishedStoryPoints);
        final Long modelPost = runExperimentAndGetMedianResult(p, ReviewMode.POST_COMMIT, PrePostModel::getFinishedStoryPoints);
        final Long modelNo = runExperimentAndGetMedianResult(p, ReviewMode.NO_REVIEW, PrePostModel::getFinishedStoryPoints);
        assertThat(modelPre, isSignificantlyLargerThan(modelNo));
        assertThat(modelPost, isSignificantlyLargerThan(modelNo));
    }

    @Test
    public void testNoReviewIsBetterThanReviewWhenThereAreNoBugs() throws Exception {
        final BulkParameterFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.0);
        final Long modelPre = runExperimentAndGetMedianResult(p, ReviewMode.PRE_COMMIT, PrePostModel::getFinishedStoryPoints);
        final Long modelPost = runExperimentAndGetMedianResult(p, ReviewMode.POST_COMMIT, PrePostModel::getFinishedStoryPoints);
        final Long modelNo = runExperimentAndGetMedianResult(p, ReviewMode.NO_REVIEW, PrePostModel::getFinishedStoryPoints);
        assertThat(modelNo, isSignificantlyLargerThan(modelPre));
        assertThat(modelNo, isSignificantlyLargerThan(modelPost));
    }

    @Test
    public void testLongerReviewDurationIncreasesCycleTimeWhenReviewIsActive() throws Exception {
        final BulkParameterFactory p1 = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.REVIEW_TIME_MEAN_DIFF, 1.0);
        final BulkParameterFactory p2 = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.REVIEW_TIME_MEAN_DIFF, 22.0);
        final Double modelShortPre = runExperimentAndGetMedianResult(p1, ReviewMode.PRE_COMMIT, PrePostModel::getStoryCycleTimeMean);
        final Double modelLongPre = runExperimentAndGetMedianResult(p2, ReviewMode.PRE_COMMIT, PrePostModel::getStoryCycleTimeMean);
        assertThat(modelLongPre, isSignificantlyLargerThan(modelShortPre));

        final Double modelShortPost = runExperimentAndGetMedianResult(p1, ReviewMode.POST_COMMIT, PrePostModel::getStoryCycleTimeMean);
        final Double modelLongPost = runExperimentAndGetMedianResult(p2, ReviewMode.POST_COMMIT, PrePostModel::getStoryCycleTimeMean);
        assertThat(modelLongPost, isSignificantlyLargerThan(modelShortPost));

        final Double modelShortNo = runExperimentAndGetMedianResult(p1, ReviewMode.NO_REVIEW, PrePostModel::getStoryCycleTimeMean);
        final Double modelLongNo = runExperimentAndGetMedianResult(p2, ReviewMode.NO_REVIEW, PrePostModel::getStoryCycleTimeMean);
        assertEquals(modelLongNo, modelShortNo);
    }

    @Test
    public void testLongerReviewDurationDecreasesStoryPointsWhenReviewIsActive() throws Exception {
        final BulkParameterFactory p1 = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.REVIEW_TIME_MEAN_DIFF, 1.0);
        final BulkParameterFactory p2 = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.REVIEW_TIME_MEAN_DIFF, 22.0);
        final Long modelShortPre = runExperimentAndGetMedianResult(p1, ReviewMode.PRE_COMMIT, PrePostModel::getFinishedStoryPoints);
        final Long modelLongPre = runExperimentAndGetMedianResult(p2, ReviewMode.PRE_COMMIT, PrePostModel::getFinishedStoryPoints);
        assertThat(modelShortPre, isSignificantlyLargerThan(modelLongPre));

        final Long modelShortPost = runExperimentAndGetMedianResult(p1, ReviewMode.POST_COMMIT, PrePostModel::getFinishedStoryPoints);
        final Long modelLongPost = runExperimentAndGetMedianResult(p2, ReviewMode.POST_COMMIT, PrePostModel::getFinishedStoryPoints);
        assertThat(modelShortPost, isSignificantlyLargerThan(modelLongPost));

        final Long modelShortNo = runExperimentAndGetMedianResult(p1, ReviewMode.NO_REVIEW, PrePostModel::getFinishedStoryPoints);
        final Long modelLongNo = runExperimentAndGetMedianResult(p2, ReviewMode.NO_REVIEW, PrePostModel::getFinishedStoryPoints);
        assertEquals(modelShortNo, modelLongNo);
    }

    @Test
    public void testWorseReviewSkillDecreasesStoryPointsPost() throws Exception {
        final BulkParameterFactory p1 = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.REVIEW_SKILL_MODE, 0.1);
        final BulkParameterFactory p2 = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.REVIEW_SKILL_MODE, 0.9);
        final Long modelBadPre = runExperimentAndGetMedianResult(p1, ReviewMode.POST_COMMIT, PrePostModel::getFinishedStoryPoints);
        final Long modelGoodPre = runExperimentAndGetMedianResult(p2, ReviewMode.POST_COMMIT, PrePostModel::getFinishedStoryPoints);
        assertThat(modelGoodPre, isSignificantlyLargerThan(modelBadPre));
    }

    @Test
    public void testWorseImplementationSkillDecreasesStoryPointsPost() throws Exception {
        final BulkParameterFactory p1 = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.9);
        final BulkParameterFactory p2 = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.1);
        final Long modelBadPre = runExperimentAndGetMedianResult(p1, ReviewMode.POST_COMMIT, PrePostModel::getFinishedStoryPoints);
        final Long modelGoodPre = runExperimentAndGetMedianResult(p2, ReviewMode.POST_COMMIT, PrePostModel::getFinishedStoryPoints);
        assertThat(modelGoodPre, isSignificantlyLargerThan(modelBadPre));
    }

    @Test
    public void testMoreFollowUpBugsHasInfluenceWithDependencies() throws Exception {
        final BulkParameterFactory p1 = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.CHAINS)
                        .copyWithChangedParam(ParameterType.FOLLOW_UP_ISSUE_SPAWN_PROBABILITY, 0.001);
        final BulkParameterFactory p2 = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.CHAINS)
                        .copyWithChangedParam(ParameterType.FOLLOW_UP_ISSUE_SPAWN_PROBABILITY, 0.1);
        final Long modelLowProb = runExperimentAndGetMedianResult(p1, ReviewMode.POST_COMMIT, PrePostModel::getFinishedStoryPoints);
        final Long modelHighProb = runExperimentAndGetMedianResult(p2, ReviewMode.POST_COMMIT, PrePostModel::getFinishedStoryPoints);
        assertThat(modelLowProb, isSignificantlyLargerThan(modelHighProb));
    }

    @Test
    public void testMoreFollowUpBugsHasNoInfluenceWithoutDependencies() throws Exception {
        final BulkParameterFactory p1 = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.NO_DEPENDENCIES)
                        .copyWithChangedParam(ParameterType.FOLLOW_UP_ISSUE_SPAWN_PROBABILITY, 0.001);
        final BulkParameterFactory p2 = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.NO_DEPENDENCIES)
                        .copyWithChangedParam(ParameterType.FOLLOW_UP_ISSUE_SPAWN_PROBABILITY, 0.1);
        final Long modelLowProb = runExperimentAndGetMedianResult(p1, ReviewMode.POST_COMMIT, PrePostModel::getFinishedStoryPoints);
        final Long modelHighProb = runExperimentAndGetMedianResult(p2, ReviewMode.POST_COMMIT, PrePostModel::getFinishedStoryPoints);
        assertEquals(modelLowProb, modelHighProb);
    }

    @Test
    public void testNoDifferenceInBugsNoMatterIfFixedAsRemarkOrAfterAssessment() throws Exception {
        final BulkParameterFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.REVIEW_SKILL_MODE, 0.05)
                        .copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.4)
                        .copyWithChangedParam(ParameterType.ISSUE_ACTIVATION_TIME_DEVELOPER_MEAN_DIFF, 300.0)
                        .copyWithChangedParam(ParameterType.ISSUE_ACTIVATION_TIME_CUSTOMER_MODE, 2000.0)
                        .copyWithChangedParam(ParameterType.ISSUE_ACTIVATION_TIME_CUSTOMER_MEAN_DIFF, 3000.0)
                        .copyWithChangedParam(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.NO_DEPENDENCIES)
                        .copyWithChangedParam(ParameterType.GLOBAL_ISSUE_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.GLOBAL_ISSUE_TRIANGLE_WIDTH, 0.0)
                        .copyWithChangedParam(ParameterType.FOLLOW_UP_ISSUE_SPAWN_PROBABILITY, 0.0)
                        .copyWithChangedParam(ParameterType.FIXING_ISSUE_RATE_FACTOR, 0.0)
                        .copyWithChangedParam(ParameterType.TASK_SWITCH_TIME_ISSUE_FACTOR, 0.0)
                        .copyWithChangedParam(ParameterType.FOLLOW_UP_ISSUE_SPAWN_PROBABILITY, 0.0);
        final Long modelPost = runExperimentAndGetMedianResult(p, ReviewMode.POST_COMMIT, PrePostModel::getIssueCountFoundByCustomers);
        final Long modelPre = runExperimentAndGetMedianResult(p, ReviewMode.PRE_COMMIT, PrePostModel::getIssueCountFoundByCustomers);
        assertThat(modelPre, isSimilarTo(modelPost));
    }

}
