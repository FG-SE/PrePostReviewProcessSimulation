package de.unihannover.se.processSimulation.preCommitPostCommit;

import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;

import de.unihannover.se.processSimulation.common.ParametersFactory;
import de.unihannover.se.processSimulation.common.ReviewMode;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory;
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

    private static Matcher<Long> isSimilarTo(long finishedStoryPoints) {
        return new CustomTypeSafeMatcher<Long>("is similar to " + finishedStoryPoints) {
            @Override
            protected boolean matchesSafely(Long arg0) {
                return Math.abs(arg0 - finishedStoryPoints) < 10;
            }
        };
    }

    private static Matcher<Long> isSignificantlyLargerThan(long finishedStoryPoints) {
        return new CustomTypeSafeMatcher<Long>("is significantly larger than " + finishedStoryPoints) {
            @Override
            protected boolean matchesSafely(Long arg0) {
                return arg0 > finishedStoryPoints + 10;
            }
        };
    }

    private static Matcher<Double> isSignificantlyLargerThan(double storyCycleTimeMean) {
        return new CustomTypeSafeMatcher<Double>("is significantly larger than " + storyCycleTimeMean) {
            @Override
            protected boolean matchesSafely(Double arg0) {
                return arg0 > storyCycleTimeMean + 10.0;
            }
        };
    }

    @Test
    public void testWhenAllRelevantEffectsAreOffThereIsNoDifference() throws Exception {
        final ParametersFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(BulkParameterFactory.IMPLEMENTATION_SKILL_MODE, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.NUMBER_OF_DEVELOPERS, 2)
                        .copyWithChangedParam(BulkParameterFactory.GLOBAL_BUG_MODE, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.CONFLICT_PROPABILITY, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.MAX_TASK_SWITCH_OVERHEAD, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.NO_DEPENDENCIES);
        final RealProcessingModel modelPre = runExperiment(p, ReviewMode.PRE_COMMIT);
        final RealProcessingModel modelPost = runExperiment(p, ReviewMode.POST_COMMIT);
        assertThat(modelPre.getFinishedStoryPoints(), isSimilarTo(modelPost.getFinishedStoryPoints()));
    }

    @Test
    public void testWithLotsOfDependenciesPreCommitHasHigherCycleTime() throws Exception {
        final ParametersFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(BulkParameterFactory.IMPLEMENTATION_SKILL_MODE, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.NUMBER_OF_DEVELOPERS, 2)
                        .copyWithChangedParam(BulkParameterFactory.GLOBAL_BUG_MODE, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.CONFLICT_PROPABILITY, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.MAX_TASK_SWITCH_OVERHEAD, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.CHAINS);
        final RealProcessingModel modelPre = runExperiment(p, ReviewMode.PRE_COMMIT);
        final RealProcessingModel modelPost = runExperiment(p, ReviewMode.POST_COMMIT);
        assertThat(modelPre.getStoryCycleTimeMean(), isSignificantlyLargerThan(modelPost.getStoryCycleTimeMean()));
    }

    @Test
    public void testLotsOfGlobalBugsMakePreCommitBetter() throws Exception {
        final ParametersFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(BulkParameterFactory.IMPLEMENTATION_SKILL_MODE, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.NUMBER_OF_DEVELOPERS, 2)
                        .copyWithChangedParam(BulkParameterFactory.GLOBAL_BUG_MODE, 1.0)
                        .copyWithChangedParam(BulkParameterFactory.GLOBAL_BUG_SUSPEND_TIME_MODE, 10.0)
                        .copyWithChangedParam(BulkParameterFactory.CONFLICT_PROPABILITY, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.MAX_TASK_SWITCH_OVERHEAD, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.NO_DEPENDENCIES);
        final RealProcessingModel modelPre = runExperiment(p, ReviewMode.PRE_COMMIT);
        final RealProcessingModel modelPost = runExperiment(p, ReviewMode.POST_COMMIT);
        assertThat(modelPre.getFinishedStoryPoints(), isSignificantlyLargerThan(modelPost.getFinishedStoryPoints()));
    }

    @Test
    public void testLotsOfConflictsMakePostCommitBetter() throws Exception {
        final ParametersFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(BulkParameterFactory.IMPLEMENTATION_SKILL_MODE, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.NUMBER_OF_DEVELOPERS, 10)
                        .copyWithChangedParam(BulkParameterFactory.GLOBAL_BUG_MODE, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.CONFLICT_PROPABILITY, 1.0)
                        .copyWithChangedParam(BulkParameterFactory.CONFLICT_RESOLUTION_TIME_MODE, 99.0)
                        .copyWithChangedParam(BulkParameterFactory.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.MAX_TASK_SWITCH_OVERHEAD, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.NO_DEPENDENCIES);
        final RealProcessingModel modelPre = runExperiment(p, ReviewMode.PRE_COMMIT);
        final RealProcessingModel modelPost = runExperiment(p, ReviewMode.POST_COMMIT);
        assertThat(modelPost.getFinishedStoryPoints(), isSignificantlyLargerThan(modelPre.getFinishedStoryPoints()));
    }

    @Test
    public void testHighTaskSwitchOverheadAndLotsOfDependenciesMakePostCommitBetter() throws Exception {
        final ParametersFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(BulkParameterFactory.IMPLEMENTATION_SKILL_MODE, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.NUMBER_OF_DEVELOPERS, 2)
                        .copyWithChangedParam(BulkParameterFactory.GLOBAL_BUG_MODE, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.CONFLICT_PROPABILITY, 1.0)
                        .copyWithChangedParam(BulkParameterFactory.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, 1.0)
                        .copyWithChangedParam(BulkParameterFactory.MAX_TASK_SWITCH_OVERHEAD, 2.0)
                        .copyWithChangedParam(BulkParameterFactory.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.SIMPLISTIC);
        final RealProcessingModel modelPre = runExperiment(p, ReviewMode.PRE_COMMIT);
        final RealProcessingModel modelPost = runExperiment(p, ReviewMode.POST_COMMIT);
        assertThat(modelPost.getFinishedStoryPoints(), isSignificantlyLargerThan(modelPre.getFinishedStoryPoints()));
    }


    @Test
    public void testasdf() throws Exception {
        //TEST
        final ParametersFactory p = BulkParameterFactory
                        .forCommercial()
                        .copyWithChangedParam(BulkParameterFactory.IMPLEMENTATION_SKILL_MODE, 0.16)
                        .copyWithChangedParam(BulkParameterFactory.NUMBER_OF_DEVELOPERS, 2)
                        .copyWithChangedParam(BulkParameterFactory.GLOBAL_BUG_MODE, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.CONFLICT_PROPABILITY, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.MAX_TASK_SWITCH_OVERHEAD, 0.0)
                        .copyWithChangedParam(BulkParameterFactory.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.NO_DEPENDENCIES);
        final RealProcessingModel modelPre = runExperiment(p, ReviewMode.PRE_COMMIT);
        //TEST
        System.out.println("-------------------------");
        final RealProcessingModel modelPost = runExperiment(p, ReviewMode.POST_COMMIT);
        assertThat(modelPre.getFinishedStoryPoints(), isSimilarTo(modelPost.getFinishedStoryPoints()));
    }

    @Test
    public void testNoReviewIsWorseThanReview() throws Exception {
        final ParametersFactory p = BulkParameterFactory
                        .forCommercial();
        final RealProcessingModel modelPre = runExperiment(p, ReviewMode.PRE_COMMIT);
        final RealProcessingModel modelPost = runExperiment(p, ReviewMode.POST_COMMIT);
        final RealProcessingModel modelNo = runExperiment(p, ReviewMode.NO_REVIEW);
        assertThat(modelPre.getFinishedStoryPoints(), isSignificantlyLargerThan(modelNo.getFinishedStoryPoints()));
        assertThat(modelPost.getFinishedStoryPoints(), isSignificantlyLargerThan(modelNo.getFinishedStoryPoints()));
    }

    //TODO weg
    @Test
    public void testNoReviewIsWorseThanReview2() throws Exception {
        final ParametersFactory p = BulkParameterFactory
                        .forCommercial();
        final RealProcessingModel modelPre = runExperiment(p, ReviewMode.PRE_COMMIT);
        final RealProcessingModel modelPost = runExperiment(p, ReviewMode.POST_COMMIT);
        final RealProcessingModel modelNo = runExperiment(p, ReviewMode.NO_REVIEW);
        assertThat(modelPre.getFinishedStoryPoints(), isSignificantlyLargerThan(modelNo.getFinishedStoryPoints()));
        assertThat(modelPost.getFinishedStoryPoints(), isSignificantlyLargerThan(modelNo.getFinishedStoryPoints()));
    }

    //TODO weg
    @Test
    public void testNoReviewIsWorseThanReview3() throws Exception {
        final ParametersFactory p = BulkParameterFactory
                        .forCommercial();
        final RealProcessingModel modelPre = runExperiment(p, ReviewMode.PRE_COMMIT);
        final RealProcessingModel modelPost = runExperiment(p, ReviewMode.POST_COMMIT);
        final RealProcessingModel modelNo = runExperiment(p, ReviewMode.NO_REVIEW);
        assertThat(modelPre.getFinishedStoryPoints(), isSignificantlyLargerThan(modelNo.getFinishedStoryPoints()));
        assertThat(modelPost.getFinishedStoryPoints(), isSignificantlyLargerThan(modelNo.getFinishedStoryPoints()));
    }

    //TODO weg
    @Test
    public void testNoReviewIsWorseThanReview4() throws Exception {
        final ParametersFactory p = BulkParameterFactory
                        .forCommercial();
        final RealProcessingModel modelPre = runExperiment(p, ReviewMode.PRE_COMMIT);
        final RealProcessingModel modelPost = runExperiment(p, ReviewMode.POST_COMMIT);
        final RealProcessingModel modelNo = runExperiment(p, ReviewMode.NO_REVIEW);
        assertThat(modelPre.getFinishedStoryPoints(), isSignificantlyLargerThan(modelNo.getFinishedStoryPoints()));
        assertThat(modelPost.getFinishedStoryPoints(), isSignificantlyLargerThan(modelNo.getFinishedStoryPoints()));
    }

    //TODO weg
    @Test
    public void testNoReviewIsWorseThanReview5() throws Exception {
        for (int i = 0; i < 30; i++) {
            final ParametersFactory p = BulkParameterFactory
                            .forCommercial();
            final RealProcessingModel modelPre = runExperiment(p, ReviewMode.PRE_COMMIT);
            final RealProcessingModel modelPost = runExperiment(p, ReviewMode.POST_COMMIT);
            final RealProcessingModel modelNo = runExperiment(p, ReviewMode.NO_REVIEW);
            assertThat(modelPre.getFinishedStoryPoints(), isSignificantlyLargerThan(modelNo.getFinishedStoryPoints()));
            assertThat(modelPost.getFinishedStoryPoints(), isSignificantlyLargerThan(modelNo.getFinishedStoryPoints()));
        }
    }

}
