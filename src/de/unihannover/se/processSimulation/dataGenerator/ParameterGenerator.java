package de.unihannover.se.processSimulation.dataGenerator;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.TimeSpan;

public class ParameterGenerator {

    private final Random r;

    public ParameterGenerator() {
        this(1238);
    }

    public ParameterGenerator(long seed) {
        this.r = new Random(seed);
    }

    public BulkParameterFactory next() {
        final double implTime = this.numberBetween(0.0, 0.4);
        final double taskSwitchOverheadAfterHour = this.numberBetween(0.0, 0.25);
        return new BulkParameterFactory(
            this.numberBetween(0.0, 2.0), // implementationSkillMode,
            this.numberBetween(0.0, 1.0), // reviewSkillMode,
            this.numberBetween(0.0, 0.3), // globalBugMode,
            this.numberBetween(0.0, 0.2), // conflictPropability,
            implTime, // implementationTimeMode,
            this.numberBetween(0, 0.5), // fixTimeMode,
            this.numberBetween(0, 0.75), // globalBugSuspendTimeMode,
            this.numberBetween(0, 0.75), // bugAssessmentTimeMode,
            this.numberBetween(0, 0.75), // conflictResolutionTimeMode,
            this.numberBetween(1.0, 400.0), // bugActivationTimeExpectedValue,
            this.numberBetween(0.5, 10.0), // planningTimeMode,
            implTime * this.numberBetween(0.01, 1.0), // reviewTimeMode,
            (int) this.numberBetween(2, 30), // numberOfDevelopers,
            new TimeSpan(taskSwitchOverheadAfterHour, TimeUnit.HOURS), // taskSwitchOverheadAfterOneHour,
            new TimeSpan(taskSwitchOverheadAfterHour * this.numberBetween(1.1, 6.0), TimeUnit.HOURS)); // maxTaskSwitchOverhead
    }

    private double numberBetween(double min, double max) {
        assert min < max;
        return min + this.r.nextDouble() * (max - min);
    }

}
