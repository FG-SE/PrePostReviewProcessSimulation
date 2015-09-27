package de.unihannover.se.processSimulation.dataGenerator;

import java.util.Map;
import java.util.Random;

import de.unihannover.se.processSimulation.common.Parameters;
import de.unihannover.se.processSimulation.common.ParametersFactory;
import desmoj.core.dist.BoolDistBernoulli;
import desmoj.core.dist.ContDistBeta;
import desmoj.core.dist.ContDistExponential;
import desmoj.core.dist.ContDistNormal;
import desmoj.core.dist.Distribution;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeSpan;

public class BulkParameterFactory extends ParametersFactory implements Cloneable {

    private double implementationSkillMode;
    private double reviewSkillMode;
    private double globalBugMode;
    private double conflictPropability;
    private double implementationTimeMode;
    private double fixTimeMode;
    private double globalBugSuspendTimeMode;
    private double conflictResolutionTimeMode;
    private double bugActivationTimeExpectedValue;
    private double planningTimeMode;
    private double reviewTimeMode;
    private int numberOfDevelopers;
    private TimeSpan taskSwitchOverheadAfterOneHour;
    private TimeSpan maxTaskSwitchOverhead;

    private int seed;

    private static final class DistributionBuilder {

        private final Random seedSource;
        private final Model owner;

        public DistributionBuilder(Random r, Model owner) {
            this.seedSource = r;
            this.owner = owner;
        }

        public ContDistBeta beta(String name, double mostPropableValue) {
            assert mostPropableValue >= 0.0;
            assert mostPropableValue <= 1.0;
            if (mostPropableValue > 0.0) {
                final double alpha = 10.0;
                final double beta = (alpha - 1.0 - mostPropableValue*alpha + 2.0*mostPropableValue) / mostPropableValue;
                return this.setSeed(new ContDistBeta(this.owner, name, alpha, beta, true, true));
            } else {
                final double alpha = 1.0;
                final double beta = 10.0;
                return this.setSeed(new ContDistBeta(this.owner, name, alpha, beta, true, true));
            }
        }

        public ContDistExponential exp(String name, double expectedValue) {
            return this.setSeed(new ContDistExponential(this.owner, name, expectedValue, true, true));
        }

        public BoolDistBernoulli bernoulli(String name, double propabilityForTrue) {
            return this.setSeed(new BoolDistBernoulli(this.owner, name, propabilityForTrue, true, true));
        }

        public ContDistNormal posNormal(String name, double mode) {
            final ContDistNormal dist = new ContDistNormal(this.owner, name, mode, mode / 10.0, true, true);
            dist.setNonNegative(true);
            return this.setSeed(dist);
        }

//        public int numberBetween(int lowerInclusive, int upperInclusive) {
//            return lowerInclusive + this.seedSource.nextInt(upperInclusive - lowerInclusive + 1);
//        }

        private<T extends Distribution> T setSeed(T dist) {
            dist.setSeed(this.seedSource.nextLong());
            return dist;
        }

    }

    @Override
    public Parameters create(Model owner) {
        final Random r = new Random(this.seed);
        final DistributionBuilder b = new DistributionBuilder(r, owner);
        return new Parameters(
                        b.posNormal("implementationSkillDist", this.implementationSkillMode),
                        b.beta("reviewSkillDist", this.reviewSkillMode),
                        b.beta("globalBugDist", this.globalBugMode),
                        b.bernoulli("conflictDist", this.conflictPropability),
                        b.posNormal("implementationTimeDist", this.implementationTimeMode),
                        b.posNormal("fixTimeDist", this.fixTimeMode),
                        b.posNormal("globalBugSuspendTimeDist", this.globalBugSuspendTimeMode),
                        b.posNormal("conflictResolutionTimeDist", this.conflictResolutionTimeMode),
                        b.exp("bugActivationTimeDist", this.bugActivationTimeExpectedValue),
                        b.posNormal("planningTimeDist", this.planningTimeMode),
                        b.posNormal("reviewTimeDist", this.reviewTimeMode),
                        this.numberOfDevelopers,
                        this.taskSwitchOverheadAfterOneHour,
                        this.maxTaskSwitchOverhead,
                        r.nextLong());
    }

    public BulkParameterFactory copyWithChangedSeed() {
        try {
            final BulkParameterFactory copy = (BulkParameterFactory) this.clone();
            copy.seed++;
            return copy;
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException("should not happen", e);
        }
    }

    public void saveData(Map<String, Object> experimentData) {
        // TODO Auto-generated method stub

    }

}
