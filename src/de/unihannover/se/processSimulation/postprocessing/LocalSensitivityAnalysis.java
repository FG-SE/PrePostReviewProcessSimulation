package de.unihannover.se.processSimulation.postprocessing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import co.paralleluniverse.common.util.Pair;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory.DistributionFactory;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory.ParameterType;
import de.unihannover.se.processSimulation.dataGenerator.DataGenerator;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRun;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRunSettings;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRunSettings.ExperimentRunParameters;
import de.unihannover.se.processSimulation.dataGenerator.MedianWithConfidenceInterval;
import de.unihannover.se.processSimulation.preCommitPostCommit.DependencyGraphConstellation;

public class LocalSensitivityAnalysis {

    public static void main(String[] args) throws Exception {
        BulkParameterFactory f = BulkParameterFactory.forCommercial();
        final List<ParameterType> types = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader("localSensitivityInput.txt"))) {
            String line;
            while ((line = r.readLine()) != null) {
                final String[] parts = line.split(":");
                final ParameterType type = ParameterType.valueOf(parts[0].trim());
                final Object value = type.parse(parts[1].trim());
                types.add(type);
                f = f.copyWithChangedParam(type, value);
            }
        }

        final List<Pair<String, Double>> results = new ArrayList<>();
        performExperiment(results, f, "original data");
        for (final ParameterType t : types) {
            if (Double.class.isAssignableFrom(t.getType())) {
                final BulkParameterFactory fUpp = f.copyWithChangedParam(t, ((Number) f.getParam(t)).doubleValue() * 2.0);
                performExperiment(results, fUpp, t + " +> " + fUpp.getParam(t));

                final BulkParameterFactory fLow = f.copyWithChangedParam(t, ((Number) f.getParam(t)).doubleValue() * 0.5);
                performExperiment(results, fLow, t + " -> " + fLow.getParam(t));
            } else if (Integer.class.isAssignableFrom(t.getType())) {
                final BulkParameterFactory fUpp = f.copyWithChangedParam(t, (int) (((Number) f.getParam(t)).doubleValue() * 2.0));
                performExperiment(results, fUpp, t + " +> " + fUpp.getParam(t));

                final BulkParameterFactory fLow = f.copyWithChangedParam(t, (int) (((Number) f.getParam(t)).doubleValue() * 0.5));
                performExperiment(results, fLow, t + " -> " + fLow.getParam(t));
            } else if (t.getType().equals(DistributionFactory.class)) {
                final DistributionFactory oldValue = (DistributionFactory) f.getParam(t);
                DistributionFactory newValue;
                if (oldValue == DistributionFactory.EXPSHIFT) {
                    newValue = DistributionFactory.LOGNORMAL;
                } else {
                    newValue = DistributionFactory.EXPSHIFT;
                }
                performExperiment(results, f.copyWithChangedParam(t, newValue), t + " -> " + newValue);
            } else if (t.getType().equals(DependencyGraphConstellation.class)) {
                final DependencyGraphConstellation oldValue = (DependencyGraphConstellation) f.getParam(t);
                DependencyGraphConstellation newValue;
                if (oldValue == DependencyGraphConstellation.NO_DEPENDENCIES) {
                    newValue = DependencyGraphConstellation.REALISTIC;
                } else {
                    newValue = DependencyGraphConstellation.NO_DEPENDENCIES;
                }
                performExperiment(results, f.copyWithChangedParam(t, newValue), t + " -> " + newValue);
            }
        }

        System.out.println();
        System.out.println("== Final results ==");
        Collections.sort(results, (o1, o2) -> o1.getSecond().compareTo(o2.getSecond()));
        for (final Pair<String, Double> result : results) {
            System.out.println(result.getFirst() + ": \t" + result.getSecond());
        }
    }

    private static void performExperiment(List<Pair<String, Double>> results, BulkParameterFactory f, String title) {
        final ExperimentRunSettings runSettings = ExperimentRunSettings.defaultSettings()
                        .copyWithChangedParam(ExperimentRunParameters.MIN_RUNS, 40)
                        .copyWithChangedParam(ExperimentRunParameters.MAX_RUNS, 60);
        final ExperimentRun result = ExperimentRun.perform(runSettings, DataGenerator::runExperiment, f, (no, pre, post) -> {System.out.print(".");});
        System.out.println();
        final MedianWithConfidenceInterval median = result.getFactorIssues();
        System.out.println(title + ": " + median + " " + result.getSummary().getIssuesResult());
        results.add(new Pair<String, Double>(title, median.getMedian()));
    }

}
