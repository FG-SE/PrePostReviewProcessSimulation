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

    public static final String STORYPOINTS = "storypoints";
    public static final String CYCLETIME_MEAN = "cycletimeMean";
    public static final String CYCLETIME_STD_DEV = "cycletimeStdDev";
    public static final String STARTED_STORIES = "startedStories";
    public static final String FINISHED_STORIES = "finishedStories";
    public static final String REMAINING_BUGS = "remainingBugs";
    public static final String SIMULATION_DURATION = "simulationDuration";

    private static final int MAX_PARAM_CONFIGS = 500;
    private static final int RUNS_PER_CONFIG = 10;

    public static void main(String[] args) throws IOException {
        initTimeUnits();

        try (final DataWriter rawResultWriter = new ArffWriter(new FileWriter("rawResults.arff"), "rawResults")) {
            registerResultAttributes(rawResultWriter);

            final ParameterGenerator gen = new ParameterGenerator();
            int total = 0;
            final long startTime = System.currentTimeMillis();
            for (int i = 0; i < MAX_PARAM_CONFIGS; i++) {
                BulkParameterFactory fac = gen.next();
                if (i == 0) {
                    fac.addAttributesTo(rawResultWriter);
                }
                for (int j = 0; j < RUNS_PER_CONFIG; j++) {
                    System.out.println("parameter set " + i + "/" + MAX_PARAM_CONFIGS + ", run " + j + "/" + RUNS_PER_CONFIG);
                    System.out.println("hash=" + fac.hashCode() + ", fac=" + fac);
//                    if (i != 53) {
//                        //TEST
//                        total++;
//                        fac = fac.copyWithChangedSeed();
//                        continue;
//                    }
                    runExperimentWithBothModes(rawResultWriter, fac, i + "_" + j, false);
                    total++;
                    fac = fac.copyWithChangedSeed();
                }
                final long diffTime = (System.currentTimeMillis() - startTime) / 1000L;
                final long remaining = (MAX_PARAM_CONFIGS * RUNS_PER_CONFIG - total) * diffTime / total;
                System.out.println("Finished " + total + " runs after " + diffTime + " seconds, approx " + remaining + " seconds remaining");
            }
        }
    }

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

    static void runExperimentWithBothModes(final DataWriter rawResultWriter, BulkParameterFactory fac, String runId, boolean report) throws IOException {
        final Map<String, Object> experimentData = new HashMap<>();
        fac.saveData(experimentData);
        runExperiment(fac, ReviewMode.PRE_COMMIT, experimentData, report, runId);
        runExperiment(fac, ReviewMode.POST_COMMIT, experimentData, report, runId);
        rawResultWriter.writeTuple(experimentData);
        rawResultWriter.flush();
    }

    static void initTimeUnits() {
        //Designfehler in DESMO-J: Die Zeiteinheiten k�nnen im Experiment-Konstruktor angegeben werden, sind in
        //  Wirklichkeit aber statische Felder. Deshalb einmal am Anfang initialisieren und danach als Defaults nutzen.
        new Experiment("TimeUnitDummyExperiment", ".", TimeUnit.MINUTES, TimeUnit.HOURS, null, noOutputs(), noOutputs(), noOutputs(), noOutputs());
    }

    private static void runExperiment(
                    final ParametersFactory p, ReviewMode mode, Map<String, Object> experimentDataBuffer, boolean report, String runId) {
        final RealProcessingModel model = new RealProcessingModel("RealProcessingModel", mode, p, report);
        final Experiment exp;
        if (report) {
            exp = new Experiment("Experiment" + mode + "_" + runId,
                            null, null, null);
        } else {
            exp = new Experiment("Experiment" + mode + "_" + runId,
                        ".", null, null, null, noOutputs(), noOutputs(), noOutputs(), noOutputs());
        }
        exp.setSeedGenerator(p.getSeed());
        model.connectToExperiment(exp);

        final long expStartTime = System.currentTimeMillis();
        exp.setSilent(!report);
        exp.getOutputPath();
        exp.setShowProgressBar(false);
        if (report) {
            exp.tracePeriod(new TimeInstant(0), new TimeInstant(160, TimeUnit.HOURS));
        }
        exp.stop(new TimeInstant(8 * 400, TimeUnit.HOURS));
        exp.start();
        if (report) {
            exp.report();
        }
        exp.finish();
        final long expDuration = System.currentTimeMillis() - expStartTime;

        experimentDataBuffer.put(mode + STORYPOINTS, model.getFinishedStoryPoints());
        experimentDataBuffer.put(mode + CYCLETIME_MEAN, model.getStoryCycleTimeMean());
        experimentDataBuffer.put(mode + CYCLETIME_STD_DEV, model.getStoryCycleTimeStdDev());
        experimentDataBuffer.put(mode + STARTED_STORIES, model.getStartedStoryCount());
        experimentDataBuffer.put(mode + FINISHED_STORIES, model.getFinishedStoryCount());
        experimentDataBuffer.put(mode + REMAINING_BUGS, model.getRemainingBugCount());
        experimentDataBuffer.put(mode + SIMULATION_DURATION, expDuration);
    }

    private static ArrayList<String> noOutputs() {
        final ArrayList<String> noOutputs = new ArrayList<>();
        return noOutputs;
    }

}
