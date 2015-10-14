package de.unihannover.se.processSimulation.dataGenerator;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import de.unihannover.se.processSimulation.common.Parameters;
import de.unihannover.se.processSimulation.common.ParametersFactory;
import de.unihannover.se.processSimulation.preCommitPostCommit.DependencyGraphConstellation;
import desmoj.core.dist.BoolDistBernoulli;
import desmoj.core.dist.ContDistBeta;
import desmoj.core.dist.ContDistExponential;
import desmoj.core.dist.ContDistNormal;
import desmoj.core.dist.Distribution;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeSpan;

public class BulkParameterFactory extends ParametersFactory implements Cloneable {

    private final double implementationSkillMode;
    private final double reviewSkillMode;
    private final double globalBugMode;
    private final double conflictPropability;
    private final double implementationTimeMode;
    private final double fixTimeMode;
    private final double globalBugSuspendTimeMode;
    private final double bugAssessmentTimeMode;
    private final double conflictResolutionTimeMode;
    private final double bugActivationTimeExpectedValue;
    private final double planningTimeMode;
    private final double reviewTimeMode;
    private final int numberOfDevelopers;
    private final double taskSwitchOverheadAfterOneHour;
    private final double maxTaskSwitchOverhead;
    private final DependencyGraphConstellation dependencyGraphConstellation;

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

    public BulkParameterFactory(
                    double implementationSkillMode,
                    double reviewSkillMode,
                    double globalBugMode,
                    double conflictPropability,
                    double implementationTimeMode,
                    double fixTimeMode,
                    double globalBugSuspendTimeMode,
                    double bugAssessmentTimeMode,
                    double conflictResolutionTimeMode,
                    double bugActivationTimeExpectedValue,
                    double planningTimeMode,
                    double reviewTimeMode,
                    int numberOfDevelopers,
                    TimeSpan taskSwitchOverheadAfterOneHour,
                    TimeSpan maxTaskSwitchOverhead,
                    DependencyGraphConstellation dependencyGraphConstellation) {
        this.implementationSkillMode = implementationSkillMode;
        this.reviewSkillMode = reviewSkillMode;
        this.globalBugMode = globalBugMode;
        this.conflictPropability = conflictPropability;
        this.implementationTimeMode = implementationTimeMode;
        this.fixTimeMode = fixTimeMode;
        this.globalBugSuspendTimeMode = globalBugSuspendTimeMode;
        this.bugAssessmentTimeMode = bugAssessmentTimeMode;
        this.conflictResolutionTimeMode = conflictResolutionTimeMode;
        this.bugActivationTimeExpectedValue = bugActivationTimeExpectedValue;
        this.planningTimeMode = planningTimeMode;
        this.reviewTimeMode = reviewTimeMode;
        this.numberOfDevelopers = numberOfDevelopers;
        this.taskSwitchOverheadAfterOneHour = taskSwitchOverheadAfterOneHour.getTimeAsDouble(TimeUnit.HOURS);
        this.maxTaskSwitchOverhead = maxTaskSwitchOverhead.getTimeAsDouble(TimeUnit.HOURS);
        this.dependencyGraphConstellation = dependencyGraphConstellation;
        this.seed = 764;
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
                        b.posNormal("bugAssessmentTimeDist", this.bugAssessmentTimeMode),
                        b.posNormal("conflictResolutionTimeDist", this.conflictResolutionTimeMode),
                        b.exp("bugActivationTimeDist", this.bugActivationTimeExpectedValue),
                        b.posNormal("planningTimeDist", this.planningTimeMode),
                        b.posNormal("reviewTimeDist", this.reviewTimeMode),
                        this.numberOfDevelopers,
                        new TimeSpan(this.taskSwitchOverheadAfterOneHour, TimeUnit.HOURS),
                        new TimeSpan(this.maxTaskSwitchOverhead, TimeUnit.HOURS),
                        r.nextLong(),
                        this.dependencyGraphConstellation);
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
        experimentData.put("implementationSkillMode", this.implementationSkillMode);
        experimentData.put("reviewSkillMode", this.reviewSkillMode);
        experimentData.put("globalBugMode", this.globalBugMode);
        experimentData.put("conflictPropability", this.conflictPropability);
        experimentData.put("implementationTimeMode", this.implementationTimeMode);
        experimentData.put("fixTimeMode", this.fixTimeMode);
        experimentData.put("globalBugSuspendTimeMode", this.globalBugSuspendTimeMode);
        experimentData.put("conflictResolutionTimeMode", this.conflictResolutionTimeMode);
        experimentData.put("bugActivationTimeExpectedValue", this.bugActivationTimeExpectedValue);
        experimentData.put("planningTimeMode", this.planningTimeMode);
        experimentData.put("reviewTimeMode", this.reviewTimeMode);
        experimentData.put("numberOfDevelopers", this.numberOfDevelopers);
        experimentData.put("taskSwitchOverheadAfterOneHour", this.taskSwitchOverheadAfterOneHour);
        experimentData.put("maxTaskSwitchOverhead", this.maxTaskSwitchOverhead);
        experimentData.put("dependencyGraphConstellation", this.dependencyGraphConstellation);
        experimentData.put("seed", this.seed);
    }

    public void addAttributesTo(DataWriter rawResultWriter) throws IOException {
        final LinkedHashMap<String, Object> hs = new LinkedHashMap<>();
        this.saveData(hs);
        for (final Entry<String, Object> e : hs.entrySet()) {
            final String name = e.getKey();
            if (e.getValue() instanceof Enum) {
                rawResultWriter.addNominalAttribute(name, e.getValue().getClass().getSuperclass().getEnumConstants());
            } else {
                rawResultWriter.addNumericAttribute(name);
            }
        }
    }

    @Override
    public long getSeed() {
        return this.seed;
    }

}
