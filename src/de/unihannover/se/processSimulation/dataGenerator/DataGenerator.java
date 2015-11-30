package de.unihannover.se.processSimulation.dataGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import de.unihannover.se.processSimulation.common.ParametersFactory;
import de.unihannover.se.processSimulation.common.ReviewMode;
import de.unihannover.se.processSimulation.preCommitPostCommit.RealProcessingModel;
import desmoj.core.dist.MersenneTwisterRandomGenerator;
import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.TimeInstant;

public class DataGenerator {

    public static final String STORYPOINTS = "storypoints";
    public static final String CYCLETIME_MEAN = "cycletimeMean";
    public static final String CYCLETIME_STD_DEV = "cycletimeStdDev";
    public static final String STARTED_STORIES = "startedStories";
    public static final String FINISHED_STORIES = "finishedStories";
    public static final String REMAINING_BUGS = "remainingBugs";
    public static final String SIMULATION_DURATION = "simulationDuration";

    static void registerResultAttributes(final DataWriter rawResultWriter) throws IOException {
        rawResultWriter.addNumericAttribute(ReviewMode.PRE_COMMIT + STORYPOINTS);
        rawResultWriter.addNumericAttribute(ReviewMode.PRE_COMMIT + CYCLETIME_MEAN);
        rawResultWriter.addNumericAttribute(ReviewMode.PRE_COMMIT + CYCLETIME_STD_DEV);
        rawResultWriter.addNumericAttribute(ReviewMode.PRE_COMMIT + STARTED_STORIES);
        rawResultWriter.addNumericAttribute(ReviewMode.PRE_COMMIT + FINISHED_STORIES);
        rawResultWriter.addNumericAttribute(ReviewMode.PRE_COMMIT + REMAINING_BUGS);
        rawResultWriter.addNumericAttribute(ReviewMode.PRE_COMMIT + SIMULATION_DURATION);
        rawResultWriter.addNumericAttribute(ReviewMode.POST_COMMIT + STORYPOINTS);
        rawResultWriter.addNumericAttribute(ReviewMode.POST_COMMIT + CYCLETIME_MEAN);
        rawResultWriter.addNumericAttribute(ReviewMode.POST_COMMIT + CYCLETIME_STD_DEV);
        rawResultWriter.addNumericAttribute(ReviewMode.POST_COMMIT + STARTED_STORIES);
        rawResultWriter.addNumericAttribute(ReviewMode.POST_COMMIT + FINISHED_STORIES);
        rawResultWriter.addNumericAttribute(ReviewMode.POST_COMMIT + REMAINING_BUGS);
        rawResultWriter.addNumericAttribute(ReviewMode.POST_COMMIT + SIMULATION_DURATION);
    }

    public static ExperimentResult runExperiment(
                    final ParametersFactory p, ReviewMode mode, boolean report, String runId) {
        final RealProcessingModel model = new RealProcessingModel("RealProcessingModel", mode, p, report);
        final Experiment exp;
        if (report) {
            exp = new Experiment("Experiment" + mode + "_" + runId,
                        ".\\experimentResults", null, Experiment.DEFAULT_REPORT_OUTPUT_TYPE,
                        Experiment.DEFAULT_TRACE_OUTPUT_TYPE, Experiment.DEFAULT_ERROR_OUTPUT_TYPE,
                        Experiment.DEFAULT_DEBUG_OUTPUT_TYPE);
        } else {
            exp = new Experiment("Experiment" + mode + "_" + runId,
                        ".\\experimentResults", null, noOutputs(), noOutputs(), noOutputs(), noOutputs());
        }
        exp.setRandomNumberGenerator(MersenneTwisterRandomGenerator.class);
        exp.setSeedGenerator(p.getSeed());
        model.connectToExperiment(exp);

        final long expStartTime = System.currentTimeMillis();
        exp.setSilent(!report);
        exp.getOutputPath();
        exp.setShowProgressBar(false);
        if (report) {
            exp.tracePeriod(new TimeInstant(0), new TimeInstant(160, TimeUnit.HOURS));
        }
        final int relevantRunningHours = 8 * 600;
        exp.stop(new TimeInstant(RealProcessingModel.HOURS_TO_RESET + relevantRunningHours, TimeUnit.HOURS));
        exp.start();
        if (report) {
            exp.report();
        }
        exp.finish();
        final long expDuration = System.currentTimeMillis() - expStartTime;

        return new ExperimentResult(
                        model.getFinishedStoryPoints(),
                        model.getStoryCycleTimeMean(),
                        model.getStoryCycleTimeStdDev(),
                        model.getStartedStoryCount(),
                        model.getFinishedStoryCount(),
                        model.getRemainingBugCount(),
                        p.getNumberOfDevelopers() * relevantRunningHours,
                        expDuration);

    }

    private static ArrayList<String> noOutputs() {
        final ArrayList<String> noOutputs = new ArrayList<>();
        return noOutputs;
    }

}
