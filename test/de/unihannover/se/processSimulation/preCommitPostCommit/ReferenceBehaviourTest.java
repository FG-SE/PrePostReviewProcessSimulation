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

    private static RealProcessingModel runExperiment(ParametersFactory p, ReviewMode mode) throws Exception {
        final RealProcessingModel model = new RealProcessingModel("RealProcessingModel", mode, p, false);
        final ArrayList<String> noOutputs = new ArrayList<>();
        final Experiment exp = new Experiment("UnitTest" + mode + "_" + p.hashCode(),
                        ".", null, noOutputs, noOutputs, noOutputs, noOutputs);
        exp.setSeedGenerator(p.getSeed());
        model.connectToExperiment(exp);

        exp.setSilent(true);
        exp.getOutputPath();
        exp.setShowProgressBar(false);
        exp.stop(new TimeInstant(RealProcessingModel.HOURS_TO_RESET + 8 * 600, TimeUnit.HOURS));
        exp.start();
        exp.finish();

        return model;
    }

    private static<T extends Comparable<T>> T runExperimentAndGetMedianResult(
                    BulkParameterFactory p, ReviewMode mode, Function<RealProcessingModel, T> getter) throws Exception {

        final List<T> results = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            final RealProcessingModel model = runExperiment(p, mode);
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
                return Math.abs(arg0 - finishedStoryPoints) < 0.005 * 0.5 * (arg0 + finishedStoryPoints);
            }
        };
    }

    private static Matcher<Long> isSignificantlyLargerThan(long finishedStoryPoints) {
        return new CustomTypeSafeMatcher<Long>("is significantly larger than " + finishedStoryPoints) {
            @Override
            protected boolean matchesSafely(Long arg0) {
                return Math.abs(arg0 - finishedStoryPoints) >= 0.005 * 0.5 * (arg0 + finishedStoryPoints);
            }
        };
    }

    private static Matcher<Double> isSignificantlyLargerThan(double storyCycleTimeMean) {
        return new CustomTypeSafeMatcher<Double>("is significantly larger than " + storyCycleTimeMean) {
            @Override
            protected boolean matchesSafely(Double arg0) {
                return Math.abs(arg0 - storyCycleTimeMean) >= 0.005 * 0.5 * (arg0 + storyCycleTimeMean);
            }
        };
    }

    @Test
    public void testWhenAllRelevantEffectsAreOffThereIsNoDifference() throws Exception {
        final BulkParameterFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_STDDEV_FACTOR, 0.0)
                        .copyWithChangedParam(ParameterType.NUMBER_OF_DEVELOPERS, 2)
                        .copyWithChangedParam(ParameterType.GLOBAL_BUG_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.GLOBAL_BUG_TRIANGLE_WIDTH, 0.0)
                        .copyWithChangedParam(ParameterType.CONFLICT_PROBABILITY, 0.0)
                        .copyWithChangedParam(ParameterType.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, 0.0)
                        .copyWithChangedParam(ParameterType.MAX_TASK_SWITCH_OVERHEAD, 0.0)
                        .copyWithChangedParam(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.NO_SUBDIVISION);
        final Long modelPre = runExperimentAndGetMedianResult(p, ReviewMode.PRE_COMMIT, RealProcessingModel::getFinishedStoryPoints);
        final Long modelPost = runExperimentAndGetMedianResult(p, ReviewMode.POST_COMMIT, RealProcessingModel::getFinishedStoryPoints);
        assertThat(modelPre, isSimilarTo(modelPost));
    }

    @Test
    public void testWithLotsOfDependenciesPreCommitHasHigherCycleTime() throws Exception {
        final BulkParameterFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.NUMBER_OF_DEVELOPERS, 2)
                        .copyWithChangedParam(ParameterType.GLOBAL_BUG_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.CONFLICT_PROBABILITY, 0.0)
                        .copyWithChangedParam(ParameterType.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, 0.0)
                        .copyWithChangedParam(ParameterType.MAX_TASK_SWITCH_OVERHEAD, 0.0)
                        .copyWithChangedParam(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.CHAINS);
        final Double modelPre = runExperimentAndGetMedianResult(p, ReviewMode.PRE_COMMIT, RealProcessingModel::getStoryCycleTimeMean);
        final Double modelPost = runExperimentAndGetMedianResult(p, ReviewMode.POST_COMMIT, RealProcessingModel::getStoryCycleTimeMean);
        assertThat(modelPre, isSignificantlyLargerThan(modelPost));
    }

    @Test
    public void testLotsOfGlobalBugsMakePreCommitBetter() throws Exception {
        final BulkParameterFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.NUMBER_OF_DEVELOPERS, 2)
                        .copyWithChangedParam(ParameterType.GLOBAL_BUG_MODE, 1.0)
                        .copyWithChangedParam(ParameterType.GLOBAL_BUG_SUSPEND_TIME_MODE, 10.0)
                        .copyWithChangedParam(ParameterType.CONFLICT_PROBABILITY, 0.0)
                        .copyWithChangedParam(ParameterType.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, 0.0)
                        .copyWithChangedParam(ParameterType.MAX_TASK_SWITCH_OVERHEAD, 0.0)
                        .copyWithChangedParam(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.NO_DEPENDENCIES);
        final Long modelPre = runExperimentAndGetMedianResult(p, ReviewMode.PRE_COMMIT, RealProcessingModel::getFinishedStoryPoints);
        final Long modelPost = runExperimentAndGetMedianResult(p, ReviewMode.POST_COMMIT, RealProcessingModel::getFinishedStoryPoints);
        assertThat(modelPre, isSignificantlyLargerThan(modelPost));
    }

    @Test
    public void testLotsOfConflictsMakePostCommitBetter() throws Exception {
        final BulkParameterFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.NUMBER_OF_DEVELOPERS, 10)
                        .copyWithChangedParam(ParameterType.GLOBAL_BUG_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.CONFLICT_PROBABILITY, 1.0)
                        .copyWithChangedParam(ParameterType.CONFLICT_RESOLUTION_TIME_MODE, 99.0)
                        .copyWithChangedParam(ParameterType.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, 0.0)
                        .copyWithChangedParam(ParameterType.MAX_TASK_SWITCH_OVERHEAD, 0.0)
                        .copyWithChangedParam(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.NO_DEPENDENCIES);
        final Long modelPre = runExperimentAndGetMedianResult(p, ReviewMode.PRE_COMMIT, RealProcessingModel::getFinishedStoryPoints);
        final Long modelPost = runExperimentAndGetMedianResult(p, ReviewMode.POST_COMMIT, RealProcessingModel::getFinishedStoryPoints);
        assertThat(modelPost, isSignificantlyLargerThan(modelPre));
    }

    @Test
    public void testHighTaskSwitchOverheadAndLotsOfDependenciesMakePostCommitBetter() throws Exception {
        final BulkParameterFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.NUMBER_OF_DEVELOPERS, 2)
                        .copyWithChangedParam(ParameterType.GLOBAL_BUG_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.CONFLICT_PROBABILITY, 1.0)
                        .copyWithChangedParam(ParameterType.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, 1.0)
                        .copyWithChangedParam(ParameterType.MAX_TASK_SWITCH_OVERHEAD, 2.0)
                        .copyWithChangedParam(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.SIMPLISTIC);
        final Long modelPre = runExperimentAndGetMedianResult(p, ReviewMode.PRE_COMMIT, RealProcessingModel::getFinishedStoryPoints);
        final Long modelPost = runExperimentAndGetMedianResult(p, ReviewMode.POST_COMMIT, RealProcessingModel::getFinishedStoryPoints);
        assertThat(modelPost, isSignificantlyLargerThan(modelPre));
    }


    @Test
    public void testasdf() throws Exception {
        //TEST
        final BulkParameterFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.16)
                        .copyWithChangedParam(ParameterType.NUMBER_OF_DEVELOPERS, 2)
                        .copyWithChangedParam(ParameterType.GLOBAL_BUG_MODE, 0.0)
                        .copyWithChangedParam(ParameterType.CONFLICT_PROBABILITY, 0.0)
                        .copyWithChangedParam(ParameterType.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, 0.0)
                        .copyWithChangedParam(ParameterType.MAX_TASK_SWITCH_OVERHEAD, 0.0)
                        .copyWithChangedParam(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.NO_DEPENDENCIES);
        final Long modelPre = runExperimentAndGetMedianResult(p, ReviewMode.PRE_COMMIT, RealProcessingModel::getFinishedStoryPoints);
        final Long modelPost = runExperimentAndGetMedianResult(p, ReviewMode.POST_COMMIT, RealProcessingModel::getFinishedStoryPoints);
        assertThat(modelPre, isSimilarTo(modelPost));
    }

    @Test
    public void testNoReviewIsWorseThanReview() throws Exception {
        final BulkParameterFactory p = BulkParameterFactory
                        .forCommercial();
        final Long modelPre = runExperimentAndGetMedianResult(p, ReviewMode.PRE_COMMIT, RealProcessingModel::getFinishedStoryPoints);
        final Long modelPost = runExperimentAndGetMedianResult(p, ReviewMode.POST_COMMIT, RealProcessingModel::getFinishedStoryPoints);
        final Long modelNo = runExperimentAndGetMedianResult(p, ReviewMode.NO_REVIEW, RealProcessingModel::getFinishedStoryPoints);
        assertThat(modelPre, isSignificantlyLargerThan(modelNo));
        assertThat(modelPost, isSignificantlyLargerThan(modelNo));
    }

    @Test
    public void testNoReviewIsBetterThanReviewWhenThereAreNoBugs() throws Exception {
        final BulkParameterFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.0);
        final Long modelPre = runExperimentAndGetMedianResult(p, ReviewMode.PRE_COMMIT, RealProcessingModel::getFinishedStoryPoints);
        final Long modelPost = runExperimentAndGetMedianResult(p, ReviewMode.POST_COMMIT, RealProcessingModel::getFinishedStoryPoints);
        final Long modelNo = runExperimentAndGetMedianResult(p, ReviewMode.NO_REVIEW, RealProcessingModel::getFinishedStoryPoints);
        assertThat(modelNo, isSignificantlyLargerThan(modelPre));
        assertThat(modelNo, isSignificantlyLargerThan(modelPost));
    }

}
