package de.unihannover.se.processSimulation.dataGenerator;

import java.io.FileWriter;
import java.io.IOException;

import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory.ParameterType;

public class SensitivityAnalysis {

    private static final double FACTOR_UP = 1.1;
    private static final double FACTOR_DOWN = 0.9;
    private static final int RUNS_PER_CONFIG = 3;

    public static void main(String[] args) {

        BulkParameterFactory p = BulkParameterFactory.forCommercial();

        try (DataWriter dataWriter = new ArffWriter(new FileWriter("sensitivityAnalysis.arff"), "sensitivityAnalysis")) {
            p.addAttributesTo(dataWriter);
            //TEST
            p = p.copyWithChangedParam(ParameterType.NUMBER_OF_DEVELOPERS, 3);
            DataGenerator.registerResultAttributes(dataWriter);
            performRandomizedRuns(dataWriter, p, "Base");
//            for (final String valueWithChange : p.getChangeableParams()) {
//                System.out.println("Running sensitivity analysis with change in param " + valueWithChange);
//                runComparison(dataWriter, p, valueWithChange, true);
//                runComparison(dataWriter, p, valueWithChange, false);
//            }
        } catch (final IOException e) {
            e.printStackTrace();
        }

    }

    private static void runComparison(DataWriter dataWriter, BulkParameterFactory baseParams, ParameterType valueWithChange, boolean up) throws IOException {

        final BulkParameterFactory p = baseParams.copyWithChangedParamMult(valueWithChange, up ? FACTOR_UP : FACTOR_DOWN);

        performRandomizedRuns(dataWriter, p, valueWithChange + "_" + up);
    }

    private static void performRandomizedRuns(DataWriter dataWriter, BulkParameterFactory p, String runIdPrefix) throws IOException {
        for (int i = 0; i < RUNS_PER_CONFIG; i++) {
            DataGenerator.runExperimentWithBothModes(dataWriter, p, runIdPrefix + "_" + i, true);
            p = p.copyWithChangedSeed();
        }
    }

}
