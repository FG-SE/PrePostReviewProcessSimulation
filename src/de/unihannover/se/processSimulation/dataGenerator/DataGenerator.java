package de.unihannover.se.processSimulation.dataGenerator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.unihannover.se.processSimulation.common.ParametersFactory;
import de.unihannover.se.processSimulation.common.ReviewMode;
import de.unihannover.se.processSimulation.preCommitPostCommit.RealProcessingModel;
import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.TimeInstant;

public class DataGenerator {

    private static final String STORYPOINTS = "storypoints";
    private static final String CYCLETIME_MEAN = "cycletimeMean";
    private static final String CYCLETIME_STD_DEV = "cycletimeStdDev";
    private static final String STARTED_STORIES = "startedStories";
    private static final String REMAINING_BUGS = "remainingBugs";

    public static void main(String[] args) throws IOException {
        try (final DataWriter rawResultWriter = new CsvWriter(new FileWriter("rawResults.arff"))) {
            rawResultWriter.addNumericAttribute(ReviewMode.PRE_COMMIT + STORYPOINTS);
            rawResultWriter.addNumericAttribute(ReviewMode.PRE_COMMIT + CYCLETIME_MEAN);
            rawResultWriter.addNumericAttribute(ReviewMode.PRE_COMMIT + CYCLETIME_STD_DEV);
            rawResultWriter.addNumericAttribute(ReviewMode.PRE_COMMIT + STARTED_STORIES);
            rawResultWriter.addNumericAttribute(ReviewMode.PRE_COMMIT + REMAINING_BUGS);
            rawResultWriter.addNumericAttribute(ReviewMode.POST_COMMIT + STORYPOINTS);
            rawResultWriter.addNumericAttribute(ReviewMode.POST_COMMIT + CYCLETIME_MEAN);
            rawResultWriter.addNumericAttribute(ReviewMode.POST_COMMIT + CYCLETIME_STD_DEV);
            rawResultWriter.addNumericAttribute(ReviewMode.POST_COMMIT + STARTED_STORIES);
            rawResultWriter.addNumericAttribute(ReviewMode.POST_COMMIT + REMAINING_BUGS);

            final ParameterGenerator gen = new ParameterGenerator();
            for (int i = 0; i < 5; i++) {
                BulkParameterFactory fac = gen.next();
                if (i == 0) {
                    fac.addAttributesTo(rawResultWriter);
                }
                for (int j = 0; j < 1; j++) {
                    final Map<String, Object> experimentData = new HashMap<>();
                    fac.saveData(experimentData);
                    runExperiment(fac, ReviewMode.PRE_COMMIT, experimentData, true);
                    runExperiment(fac, ReviewMode.POST_COMMIT, experimentData, true);
                    rawResultWriter.writeTuple(experimentData);
                    rawResultWriter.flush();
                    fac = fac.copyWithChangedSeed();
                }
            }
        }
    }

    private static void runExperiment(
                    final ParametersFactory p, ReviewMode mode, Map<String, Object> experimentDataBuffer, boolean report) {
        final RealProcessingModel model = new RealProcessingModel("RealProcessingModel", mode, p);
        final Experiment exp;
        if (report) {
            exp = new Experiment("DevelopmentProcessModelTestExperiment" + mode + p.hashCode(),
                            TimeUnit.MINUTES, TimeUnit.HOURS, null);
        } else {
            final ArrayList<String> noOutputs = new ArrayList<>();
            exp = new Experiment("DevelopmentProcessModelTestExperiment" + mode + p.hashCode(),
                        ".", TimeUnit.MINUTES, TimeUnit.HOURS, null, noOutputs, noOutputs, noOutputs, noOutputs);
        }
        model.connectToExperiment(exp);

        exp.setSilent(!report);
        exp.getOutputPath();
        exp.setShowProgressBar(false);
        exp.stop(new TimeInstant(8 * 400, TimeUnit.HOURS));
        exp.start();
        if (report) {
            exp.report();
        }
        exp.finish();

        experimentDataBuffer.put(mode + STORYPOINTS, model.getFinishedStoryPoints());
        experimentDataBuffer.put(mode + CYCLETIME_MEAN, model.getStoryCycleTimeMean());
        experimentDataBuffer.put(mode + CYCLETIME_STD_DEV, model.getStoryCycleTimeStdDev());
        experimentDataBuffer.put(mode + STARTED_STORIES, model.getStartedStoryCount());
        experimentDataBuffer.put(mode + REMAINING_BUGS, model.getRemainingBugCount());
    }

}
