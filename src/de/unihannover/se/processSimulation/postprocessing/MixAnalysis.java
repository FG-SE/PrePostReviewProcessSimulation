package de.unihannover.se.processSimulation.postprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import co.paralleluniverse.common.util.Pair;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory.ParameterType;
import de.unihannover.se.processSimulation.dataGenerator.DataGenerator;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRun;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRun.PrePostComparison;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRunSettings;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRunSettings.ExperimentRunParameters;
import de.unihannover.se.processSimulation.dataGenerator.MedianWithConfidenceInterval;

/**
 * Takes two data points and simulates all combinations of each with one component from the other.
 */
public class MixAnalysis {

    public static void main(String[] args) throws Exception {
        System.out.println("Mix analysis for parameters: " + args[0] + " and " + args[1]);
        System.out.println();
        final BulkParameterFactory f1 = readParamsInWekaFormat(args[0]);
        final BulkParameterFactory f2 = readParamsInWekaFormat(args[1]);
        final String filename = new File(args[0]).getName();

        final List<Pair<String, Double>> results = new ArrayList<>();
        performExperiment(results, f1, "original data 1", filename);
        performExperiment(results, f2, "original data 2", filename);
        for (int i = 0; i < ParameterType.values().length; i++) {
            final ParameterType t = ParameterType.values()[i];
            final long mixPattern = 1L << i;
            final BulkParameterFactory mix1 = BulkParameterFactory.mix(f1, f2, mixPattern);
            final BulkParameterFactory mix2 = BulkParameterFactory.mix(f2, f1, mixPattern);
            performExperiment(results, mix1, "D1 with " + t + " = " + mix1.getParam(t), filename);
            performExperiment(results, mix2, "D2 with " + t + " = " + mix2.getParam(t), filename);
        }

        System.out.println();
        System.out.println("== Final results ==");
        Collections.sort(results, (o1, o2) -> o1.getSecond().compareTo(o2.getSecond()));
        for (final Pair<String, Double> result : results) {
            System.out.println(result.getFirst() + ": \t" + result.getSecond());
        }
    }

    private static BulkParameterFactory readParamsInWekaFormat(String filename) throws IOException {
        BulkParameterFactory f = BulkParameterFactory.forCommercial();
        final List<ParameterType> types = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = r.readLine()) != null) {
                final String[] parts = line.split(":");
                final ParameterType type = ParameterType.valueOf(parts[0].trim());
                final Object value = type.parse(parts[1].trim());
                types.add(type);
                f = f.copyWithChangedParam(type, value);
            }
        }
        return f;
    }

    private static void performExperiment(List<Pair<String, Double>> results, BulkParameterFactory f, String title, String filename) {
        final ExperimentRunSettings runSettings = ExperimentRunSettings.defaultSettings()
                        .copyWithChangedParam(ExperimentRunParameters.MIN_RUNS, 20)
                        .copyWithChangedParam(ExperimentRunParameters.MAX_RUNS, 160);
        final ExperimentRun result = ExperimentRun.perform(runSettings, DataGenerator::runExperiment, f, (no, pre, post) -> {System.out.print(".");});
        System.out.println();
        final MedianWithConfidenceInterval median;
        final PrePostComparison prePostComparison;
        final String type;
        if (filename.contains("storyPoints")) {
            median = result.getFactorStoryPoints();
            prePostComparison = result.getSummary().getStoryPointsResult();
            type = "storyPoints";
        } else if (filename.contains("cycleTime")) {
            median = result.getFactorCycleTime();
            prePostComparison = result.getSummary().getCycleTimeResult();
            type = "cycleTime";
        } else {
            median = result.getFactorIssues();
            prePostComparison = result.getSummary().getIssuesResult();
            type = "issues";
        }
        System.out.println(title + ": " + median + " " + prePostComparison + " " + type);
        results.add(new Pair<String, Double>(title, median.getMedian()));
    }

}
