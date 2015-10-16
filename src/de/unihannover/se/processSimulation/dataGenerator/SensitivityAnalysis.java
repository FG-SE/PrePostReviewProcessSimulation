package de.unihannover.se.processSimulation.dataGenerator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import de.unihannover.se.processSimulation.preCommitPostCommit.DependencyGraphConstellation;
import desmoj.core.simulator.TimeSpan;

public class SensitivityAnalysis {

    private static final double FACTOR_UP = 1.1;
    private static final double FACTOR_DOWN = 0.9;
    private static final int RUNS_PER_CONFIG = 2;

    public static void main(String[] args) {

        DataGenerator.initTimeUnits();

        final Object[] params = new Object[] {
                0.9, // implementationSkillMode??
                0.5, // reviewSkillMode??
                0.001, // globalBugMode??
                0.01, // conflictPropability??
                17.3, // implementationTimeMode
                13.0, //bugfixTaskTimeMode
                0.1, // reviewRemarkfixTimeMode??
                0.15, // globalBugSuspendTimeMode??
                0.5, // bugAssessmentTimeMode??
                0.3, // conflictResolutionTimeMode??
                500.0, // bugActivationTimeExpectedValue??
                4.0, // planningTimeMode??
                3.0, // reviewTimeMode
                12, // numberOfDevelopers
                5, // taskSwitchOverheadAfterOneHour - Minuten
                30, // maxTaskSwitchOverhead - Minuten
        };

        try (DataWriter dataWriter = new ArffWriter(new FileWriter("sensitivityAnalysis.arff"), "sensitivityAnalysis")) {
            for (int valueWithChange = -1; valueWithChange < params.length; valueWithChange++) {
                System.out.println("Running sensitivity analysis with change in param " + valueWithChange);
                runComparison(dataWriter, params, valueWithChange, true);
                runComparison(dataWriter, params, valueWithChange, false);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }

    }

    private static void runComparison(DataWriter dataWriter, Object[] params, int valueWithChange, boolean up) throws IOException {
        final BulkParameterFactory p = new BulkParameterFactory(
                        adjustIfNeededD(params, 0, valueWithChange, up), // implementationSkillMode
                        adjustIfNeededD(params, 1, valueWithChange, up), // reviewSkillMode
                        adjustIfNeededD(params, 2, valueWithChange, up), // globalBugMode
                        adjustIfNeededD(params, 3, valueWithChange, up), // conflictPropability
                        adjustIfNeededD(params, 4, valueWithChange, up), // implementationTimeMode
                        adjustIfNeededD(params, 5, valueWithChange, up), //bugfixTaskTimeMode
                        adjustIfNeededD(params, 6, valueWithChange, up), // reviewRemarkfixTimeMode
                        adjustIfNeededD(params, 7, valueWithChange, up), // globalBugSuspendTimeMode
                        adjustIfNeededD(params, 8, valueWithChange, up), // bugAssessmentTimeMode
                        adjustIfNeededD(params, 9, valueWithChange, up), // conflictResolutionTimeMode
                        adjustIfNeededD(params, 10, valueWithChange, up), // bugActivationTimeExpectedValue
                        adjustIfNeededD(params, 11, valueWithChange, up), // planningTimeMode
                        adjustIfNeededD(params, 12, valueWithChange, up), // reviewTimeMode
                        adjustIfNeededI(params, 13, valueWithChange, up), // numberOfDevelopers
                        new TimeSpan(adjustIfNeededI(params, 14, valueWithChange, up), TimeUnit.MINUTES), // taskSwitchOverheadAfterOneHour
                        new TimeSpan(adjustIfNeededI(params, 15, valueWithChange, up), TimeUnit.MINUTES), // maxTaskSwitchOverhead
                        DependencyGraphConstellation.REALISTIC);

        if (valueWithChange == -1 && up) {
            p.addAttributesTo(dataWriter);
            DataGenerator.registerResultAttributes(dataWriter);
        }

        performRandomizedRuns(dataWriter, p, valueWithChange + "_" + up);
    }

    private static double adjustIfNeededD(Object[] params, int i, int valueWithChange, boolean up) {
        if (i == valueWithChange) {
            final double factor = up ? FACTOR_UP : FACTOR_DOWN;
            return ((Double) params[i]) * factor;
        } else {
            return (Double) params[i];
        }
    }

    private static int adjustIfNeededI(Object[] params, int i, int valueWithChange, boolean up) {
        if (i == valueWithChange) {
            final double factor = up ? FACTOR_UP : FACTOR_DOWN;
            final double dv = ((Integer) params[i]) * factor;
            final long rounded = Math.round(dv);
            if (rounded != ((Integer) params[i]).intValue()) {
                return (int) rounded;
            } else {
                return (int) (up ? rounded + 1 : rounded - 1);
            }
        } else {
            return (Integer) params[i];
        }
    }

    private static void performRandomizedRuns(DataWriter dataWriter, BulkParameterFactory p, String runIdPrefix) throws IOException {
        for (int i = 0; i < RUNS_PER_CONFIG; i++) {
            DataGenerator.runExperimentWithBothModes(dataWriter, p, runIdPrefix + "_" + i, true);
            p = p.copyWithChangedSeed();
        }
    }

}
