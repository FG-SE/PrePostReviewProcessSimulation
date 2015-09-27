package de.unihannover.se.processSimulation.dataGenerator;

import java.io.FileWriter;
import java.io.IOException;
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

    public static void main(String[] args) throws IOException {
        try (final ArffWriter rawResultWriter = new ArffWriter(new FileWriter("rawResults.arff"), "rawResults")) {
            rawResultWriter.addNumericAttribute(ReviewMode.PRE_COMMIT + STORYPOINTS);
            rawResultWriter.addNumericAttribute(ReviewMode.PRE_COMMIT + CYCLETIME_MEAN);
            rawResultWriter.addNumericAttribute(ReviewMode.PRE_COMMIT + CYCLETIME_STD_DEV);
            rawResultWriter.addNumericAttribute(ReviewMode.POST_COMMIT + STORYPOINTS);
            rawResultWriter.addNumericAttribute(ReviewMode.POST_COMMIT + CYCLETIME_MEAN);
            rawResultWriter.addNumericAttribute(ReviewMode.POST_COMMIT + CYCLETIME_STD_DEV);

            final ParameterGenerator gen = new ParameterGenerator();
            for (int i = 0; i < 2; i++) {
                BulkParameterFactory fac = gen.next();
                for (int j = 0; j < 100; j++) {
                    final Map<String, Object> experimentData = new HashMap<>();
                    fac.saveData(experimentData);
                    runExperiment(fac, ReviewMode.PRE_COMMIT, experimentData);
                    runExperiment(fac, ReviewMode.POST_COMMIT, experimentData);
                    rawResultWriter.writeTuple(experimentData);
                    fac = fac.copyWithChangedSeed();
                }
            }
        }
    }

    private static void runExperiment(
                    final ParametersFactory p, ReviewMode mode, Map<String, Object> experimentDataBuffer) {
        final RealProcessingModel model = new RealProcessingModel("RealProcessingModel", mode, p);
        final Experiment exp = new Experiment("DevelopmentProcessModelTestExperiment" + mode,
                        TimeUnit.MINUTES, TimeUnit.HOURS, null);
        model.connectToExperiment(exp);

        exp.setSilent(true);
        exp.setShowProgressBar(false);
        exp.stop(new TimeInstant(8 * 400, TimeUnit.HOURS));
        exp.start();
        exp.finish();

        experimentDataBuffer.put(mode + STORYPOINTS, model.getFinishedStoryPoints());
        experimentDataBuffer.put(mode + CYCLETIME_MEAN, model.getStoryCycleTimeMean());
        experimentDataBuffer.put(mode + CYCLETIME_STD_DEV, model.getStoryCycleTimeStdDev());
    }

}
