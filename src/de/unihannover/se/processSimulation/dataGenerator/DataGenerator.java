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

    private static final int MAX_PARAM_CONFIGS = 5;
    private static final int RUNS_PER_CONFIG = 1;

    public static void main(String[] args) throws IOException {
        initTimeUnits();

        try (final DataWriter rawResultWriter = new CsvWriter(new FileWriter("rawResults.csv"))) {
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
            for (int i = 0; i < MAX_PARAM_CONFIGS; i++) {
                BulkParameterFactory fac = gen.next();
                if (i == 0) {
                    fac.addAttributesTo(rawResultWriter);
                }
                for (int j = 0; j < RUNS_PER_CONFIG; j++) {
                    System.out.println("parameter set " + i + "/" + MAX_PARAM_CONFIGS + ", run " + j + "/" + RUNS_PER_CONFIG);
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

    private static void initTimeUnits() {
        //Designfehler in DESMO-J: Die Zeiteinheiten können im Experiment-Konstruktor angegeben werden, sind in
        //  Wirklichkeit aber statische Felder. Deshalb einmal am Anfang initialisieren und danach als Defaults nutzen.
        new Experiment("TimeUnitDummyExperiment", ".", TimeUnit.MINUTES, TimeUnit.HOURS, null, noOutputs(), noOutputs(), noOutputs(), noOutputs());
    }

    private static void runExperiment(
                    final ParametersFactory p, ReviewMode mode, Map<String, Object> experimentDataBuffer, boolean report) {
        final RealProcessingModel model = new RealProcessingModel("RealProcessingModel", mode, p);
        final Experiment exp;
        if (report) {
            exp = new Experiment("DevelopmentProcessModelTestExperiment" + mode + p.hashCode(),
                            null, null, null);
        } else {
            exp = new Experiment("DevelopmentProcessModelTestExperiment" + mode + p.hashCode(),
                        ".", null, null, null, noOutputs(), noOutputs(), noOutputs(), noOutputs());
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

    private static ArrayList<String> noOutputs() {
        final ArrayList<String> noOutputs = new ArrayList<>();
        return noOutputs;
    }

}
