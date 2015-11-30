package de.unihannover.se.processSimulation.dataGenerator;

import java.io.IOException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import de.unihannover.se.processSimulation.common.Parameters;
import de.unihannover.se.processSimulation.common.ParametersFactory;
import de.unihannover.se.processSimulation.preCommitPostCommit.DependencyGraphConstellation;
import desmoj.core.dist.BoolDistBernoulli;
import desmoj.core.dist.ContDistBeta;
import desmoj.core.dist.ContDistExponential;
import desmoj.core.dist.ContDistNormal;
import desmoj.core.dist.Distribution;
import desmoj.core.dist.MersenneTwisterRandomGenerator;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeSpan;

public class BulkParameterFactory extends ParametersFactory implements Cloneable {

    public enum ParameterType {
        IMPLEMENTATION_SKILL_MODE(Double.class, ""),
        REVIEW_SKILL_MODE(Double.class, ""),
        GLOBAL_BUG_MODE(Double.class, ""),
        CONFLICT_PROBABILITY(Double.class, ""),
        IMPLEMENTATION_TIME_MODE(Double.class, ""),
        BUGFIX_TASK_TIME_MODE(Double.class, ""),
        REVIEW_REMARK_FIX_TIME_MODE(Double.class, ""),
        GLOBAL_BUG_SUSPEND_TIME_MODE(Double.class, ""),
        BUG_ASSESSMENT_TIME_MODE(Double.class, ""),
        CONFLICT_RESOLUTION_TIME_MODE(Double.class, ""),
        BUG_ACTIVATION_TIME_EXPECTED_VALUE(Double.class, ""),
        PLANNING_TIME_MODE(Double.class, ""),
        REVIEW_TIME_MODE(Double.class, ""),
        NUMBER_OF_DEVELOPERS(Integer.class, ""),
        TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR(Double.class, ""),
        MAX_TASK_SWITCH_OVERHEAD(Double.class, ""),
        DEPENDENCY_GRAPH_CONSTELLATION(DependencyGraphConstellation.class, "");


        private final Class<?> type;
        private final String description;

        private ParameterType(Class<?> type, String description) {
            this.type = type;
            this.description = description;
        }

        public Class<?> getType() {
            return this.type;
        }

        public void checkType(Object newValue) {
            if (!this.type.isInstance(newValue)) {
                throw new RuntimeException("Value " + newValue + " (" + newValue.getClass() + ") has wrong type for parameter " + this);
            }
        }

        public String getDescription() {
            return this.description;
        }

        public Object parse(String param) {
            if (this.type.equals(Double.class)) {
                return Double.valueOf(param);
            } else if (this.type.equals(Integer.class)) {
                return Integer.valueOf(param);
            } else if (this.type.isEnum()) {
                for (final Object enumValue : this.type.getEnumConstants()) {
                    if (((Enum<?>) enumValue).name().equals(param)) {
                        return enumValue;
                    }
                }
                throw new RuntimeException("Unknown enum constant " + param + " in " + this.type);
            } else {
                throw new RuntimeException("Invalid type in parameter " + this);
            }
        }
    }


    private final EnumMap<ParameterType, Object> parameters = new EnumMap<>(ParameterType.class);
    private int seed;

    private static final class DistributionBuilder {

        private final MersenneTwisterRandomGenerator seedSource;
        private final Model owner;

        public DistributionBuilder(MersenneTwisterRandomGenerator r, Model owner) {
            this.seedSource = r;
            this.owner = owner;
        }

        public ContDistBeta beta(String name, double mostProbableValue) {
            assert mostProbableValue >= 0.0;
            assert mostProbableValue <= 1.0;
            if (mostProbableValue > 0.0) {
                final double alpha = 10.0;
                final double beta = (alpha - 1.0 - mostProbableValue*alpha + 2.0*mostProbableValue) / mostProbableValue;
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

        public BoolDistBernoulli bernoulli(String name, double probabilityForTrue) {
            return this.setSeed(new BoolDistBernoulli(this.owner, name, probabilityForTrue, true, true));
        }

        public ContDistNormal posNormal(String name, double mode) {
            final ContDistNormal dist = new ContDistNormal(this.owner, name, mode, mode / 10.0, true, true);
            dist.setNonNegative(true);
            return this.setSeed(dist);
        }

        public ContDistLognormal logNormal(String name, double mean, double median) {
            final double mu = Math.log(median);
            final double sigma = Math.sqrt(2.0 * (Math.log(mean) - mu));
            final ContDistLognormal dist = new ContDistLognormal(this.owner, name, true, true, mu, sigma);
            return this.setSeed(dist);
        }

//        public int numberBetween(int lowerInclusive, int upperInclusive) {
//            return lowerInclusive + this.seedSource.nextInt(upperInclusive - lowerInclusive + 1);
//        }

        private<T extends Distribution> T setSeed(T dist) {
            dist.setSeed(nextLong(this.seedSource));
            return dist;
        }

    }

    public BulkParameterFactory(
                    double implementationSkillMode,
                    double reviewSkillMode,
                    double globalBugMode,
                    double conflictProbability,
                    double implementationTimeMode,
                    double bugfixTaskTimeMode,
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
        this.parameters.put(ParameterType.IMPLEMENTATION_SKILL_MODE, implementationSkillMode);
        this.parameters.put(ParameterType.REVIEW_SKILL_MODE, reviewSkillMode);
        this.parameters.put(ParameterType.GLOBAL_BUG_MODE, globalBugMode);
        this.parameters.put(ParameterType.CONFLICT_PROBABILITY, conflictProbability);
        this.parameters.put(ParameterType.IMPLEMENTATION_TIME_MODE, implementationTimeMode);
        this.parameters.put(ParameterType.BUGFIX_TASK_TIME_MODE, bugfixTaskTimeMode);
        this.parameters.put(ParameterType.REVIEW_REMARK_FIX_TIME_MODE, fixTimeMode);
        this.parameters.put(ParameterType.GLOBAL_BUG_SUSPEND_TIME_MODE, globalBugSuspendTimeMode);
        this.parameters.put(ParameterType.BUG_ASSESSMENT_TIME_MODE, bugAssessmentTimeMode);
        this.parameters.put(ParameterType.CONFLICT_RESOLUTION_TIME_MODE, conflictResolutionTimeMode);
        this.parameters.put(ParameterType.BUG_ACTIVATION_TIME_EXPECTED_VALUE, bugActivationTimeExpectedValue);
        this.parameters.put(ParameterType.PLANNING_TIME_MODE, planningTimeMode);
        this.parameters.put(ParameterType.REVIEW_TIME_MODE, reviewTimeMode);
        this.parameters.put(ParameterType.NUMBER_OF_DEVELOPERS, numberOfDevelopers);
        this.parameters.put(ParameterType.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, taskSwitchOverheadAfterOneHour.getTimeAsDouble(TimeUnit.HOURS));
        this.parameters.put(ParameterType.MAX_TASK_SWITCH_OVERHEAD, maxTaskSwitchOverhead.getTimeAsDouble(TimeUnit.HOURS));
        this.parameters.put(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION, dependencyGraphConstellation);
        this.seed = 764;
    }

    public static BulkParameterFactory forCommercial() {
        //implementationSkill calculation:
        //  4196 review remarks in tickets that took 46879 hours
        //  and (508+542) new bugs in last two years, from (940+923) tasks
        //  and rough guess of about 1000 more problems in the same tasks
        //  => 4196/46879 + 2000/(1900*15) = 0.09 + 0.07 = 0.16

        return new BulkParameterFactory(
                0.16, // implementationSkillMode
                0.56, // reviewSkillMode
                0.001, // globalBugMode??
                0.01, // conflictProbability??
                17.3, // implementationTimeMode
                13.0, //bugfixTaskTimeMode
                1.86, // reviewRemarkfixTimeMode
                0.15, // globalBugSuspendTimeMode??
                0.5, // bugAssessmentTimeMode??
                0.3, // conflictResolutionTimeMode??
                1000.0, // bugActivationTimeExpectedValue??
                4.0, // planningTimeMode??
                3.0, // reviewTimeMode
                12, // numberOfDevelopers
                new TimeSpan(5, TimeUnit.MINUTES), // taskSwitchOverheadAfterOneHour
                new TimeSpan(30, TimeUnit.MINUTES), // maxTaskSwitchOverhead
                DependencyGraphConstellation.REALISTIC
        );
    }


    @Override
    public Parameters create(Model owner) {
        final MersenneTwisterRandomGenerator r = new MersenneTwisterRandomGenerator(this.seed);
        final DistributionBuilder b = new DistributionBuilder(r, owner);
        return new Parameters(
                        b.posNormal("implementationSkillDist", this.getParamD(ParameterType.IMPLEMENTATION_SKILL_MODE)),
                        b.beta("reviewSkillDist", this.getParamD(ParameterType.REVIEW_SKILL_MODE)),
                        b.beta("globalBugDist", this.getParamD(ParameterType.GLOBAL_BUG_MODE)),
                        b.bernoulli("conflictDist", this.getParamD(ParameterType.CONFLICT_PROBABILITY)),
                        b.logNormal("implementationTimeDist", this.getParamD(ParameterType.IMPLEMENTATION_TIME_MODE), this.getParamD(ParameterType.IMPLEMENTATION_TIME_MODE) / 2.0),
                        b.exp("bugfixTaskTimeDist", this.getParamD(ParameterType.BUGFIX_TASK_TIME_MODE)),
                        b.exp("reviewRemarkFixTimeDist", this.getParamD(ParameterType.REVIEW_REMARK_FIX_TIME_MODE)),
                        b.posNormal("globalBugSuspendTimeDist", this.getParamD(ParameterType.GLOBAL_BUG_SUSPEND_TIME_MODE)),
                        b.posNormal("bugAssessmentTimeDist", this.getParamD(ParameterType.BUG_ASSESSMENT_TIME_MODE)),
                        b.posNormal("conflictResolutionTimeDist", this.getParamD(ParameterType.CONFLICT_RESOLUTION_TIME_MODE)),
                        b.exp("bugActivationTimeDist", this.getParamD(ParameterType.BUG_ACTIVATION_TIME_EXPECTED_VALUE)),
                        b.posNormal("planningTimeDist", this.getParamD(ParameterType.PLANNING_TIME_MODE)),
                        b.exp("reviewTimeDist", this.getParamD(ParameterType.REVIEW_TIME_MODE)),
                        this.getParamI(ParameterType.NUMBER_OF_DEVELOPERS),
                        new TimeSpan(this.getParamD(ParameterType.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR), TimeUnit.HOURS),
                        new TimeSpan(this.getParamD(ParameterType.MAX_TASK_SWITCH_OVERHEAD), TimeUnit.HOURS),
                        nextLong(r),
                        (DependencyGraphConstellation) this.getParam(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION));
    }

    private static long nextLong(MersenneTwisterRandomGenerator r) {
        return ((long)(r.nextInt(32)) << 32) + r.nextInt(32);
    }

    public BulkParameterFactory copyWithChangedSeed() {
        final BulkParameterFactory copy = this.copy();
        copy.seed++;
        return copy;
    }

    public BulkParameterFactory copyWithChangedParamMult(ParameterType paramId, double factor) {
        final BulkParameterFactory copy = this.copy();
        final Object oldValue = copy.getParam(paramId);
        if (paramId.getType().equals(Double.class)) {
            copy.setParam(paramId, ((Double) oldValue) * factor);
        } else if (paramId.getType().equals(Integer.class)) {
            final double dv = ((Integer) oldValue) * factor;
            final long rounded = Math.round(dv);
            if (rounded != ((Integer) oldValue).intValue()) {
                copy.setParam(paramId, (int) rounded);
            } else {
                copy.setParam(paramId, (int) (factor > 1 ? rounded + 1 : rounded - 1));
            }
        } else {
            throw new RuntimeException("Parameter " + paramId + " has unsupported type " + oldValue.getClass());
        }
        return copy;
    }

    public BulkParameterFactory copyWithChangedParam(ParameterType paramId, Object newValue) {
        final BulkParameterFactory copy = this.copy();
        copy.setParam(paramId, newValue);
        return copy;
    }

    private BulkParameterFactory copy() {
        try {
            return (BulkParameterFactory) this.clone();
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException("should not happen", e);
        }
    }

    public void saveData(Map<String, Object> experimentData) {
        for (final ParameterType param : ParameterType.values()) {
            experimentData.put(param.toString(), this.getParam(param));
        }
        experimentData.put("seed", this.seed);
    }

    public Object getParam(ParameterType param) {
        assert this.parameters.containsKey(param);
        return this.parameters.get(param);
    }

    private double getParamD(ParameterType param) {
        return (Double) this.getParam(param);
    }

    private int getParamI(ParameterType param) {
        return (Integer) this.getParam(param);
    }

    private void setParam(ParameterType param, Object newValue) {
        param.checkType(newValue);
        this.parameters.put(param, newValue);
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

    @Override
    public int getNumberOfDevelopers() {
        return this.getParamI(ParameterType.NUMBER_OF_DEVELOPERS);
    }

}
