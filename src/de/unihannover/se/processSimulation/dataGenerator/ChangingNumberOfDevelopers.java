package de.unihannover.se.processSimulation.dataGenerator;

import java.io.FileWriter;
import java.io.IOException;

import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory.ParameterType;

public class ChangingNumberOfDevelopers {

    private static final int RUNS_PER_CONFIG = 1;

    public static void main(String[] args) {
        final BulkParameterFactory base = BulkParameterFactory.forCommercial();

        try (DataWriter dataWriter = new ArffWriter(new FileWriter("changingNumberOfDevelopers.arff"), "changingNumberOfDevelopers")) {
            base.addAttributesTo(dataWriter);
            DataGenerator.registerResultAttributes(dataWriter);

            for (int devCount = 2; devCount < 3; devCount++) {
                System.out.println("Running experiments for dev count " + devCount);
                BulkParameterFactory p = base.copyWithChangedParam(ParameterType.NUMBER_OF_DEVELOPERS, devCount);
                for (int i = 0; i < RUNS_PER_CONFIG; i++) {
                    System.out.println("fac=" + p);
                    DataGenerator.runExperimentWithBothModes(dataWriter, p, "DevCount" + devCount + "_" + i, true);
                    p = p.copyWithChangedSeed();
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

}
