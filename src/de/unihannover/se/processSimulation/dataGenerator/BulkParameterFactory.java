package de.unihannover.se.processSimulation.dataGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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
import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeSpan;

public class BulkParameterFactory extends ParametersFactory implements Cloneable {

    static {
        //Designfehler in DESMO-J: Die Zeiteinheiten können im Experiment-Konstruktor angegeben werden, sind in
        //  Wirklichkeit aber statische Felder. Deshalb einmal am Anfang initialisieren und danach als Defaults nutzen.
        final ArrayList<String> noOutputs = new ArrayList<>();
        new Experiment("TimeUnitDummyExperiment", ".", TimeUnit.MINUTES, TimeUnit.HOURS, null, noOutputs, noOutputs, noOutputs, noOutputs);
    }

    public static final String IMPLEMENTATION_SKILL_MODE = "implementationSkillMode";
    public static final String REVIEW_SKILL_MODE = "reviewSkillMode";
    public static final String GLOBAL_BUG_MODE = "globalBugMode";
    public static final String CONFLICT_PROPABILITY = "conflictPropability";
    public static final String IMPLEMENTATION_TIME_MODE = "implementationTimeMode";
    public static final String BUGFIX_TASK_TIME_MODE = "bugfixTaskTimeMode";
    public static final String REVIEW_REMARK_FIX_TIME_MODE = "reviewRemarkFixTimeMode";
    public static final String GLOBAL_BUG_SUSPEND_TIME_MODE = "globalBugSuspendTimeMode";
    public static final String BUG_ASSESSMENT_TIME_MODE = "bugAssessmentTimeMode";
    public static final String CONFLICT_RESOLUTION_TIME_MODE = "conflictResolutionTimeMode";
    public static final String BUG_ACTIVATION_TIME_EXPECTED_VALUE = "bugActivationTimeExpectedValue";
    public static final String PLANNING_TIME_MODE = "planningTimeMode";
    public static final String REVIEW_TIME_MODE = "reviewTimeMode";
    public static final String NUMBER_OF_DEVELOPERS = "numberOfDevelopers";
    public static final String TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR = "taskSwitchOverheadAfterOneHour";
    public static final String MAX_TASK_SWITCH_OVERHEAD = "maxTaskSwitchOverhead";
    public static final String DEPENDENCY_GRAPH_CONSTELLATION = "dependencyGraphConstellation";


    private static final String[] PARAMETER_IDS = new String[] {
        IMPLEMENTATION_SKILL_MODE,
        REVIEW_SKILL_MODE,
        GLOBAL_BUG_MODE,
        CONFLICT_PROPABILITY,
        IMPLEMENTATION_TIME_MODE,
        BUGFIX_TASK_TIME_MODE,
        REVIEW_REMARK_FIX_TIME_MODE,
        GLOBAL_BUG_SUSPEND_TIME_MODE,
        BUG_ASSESSMENT_TIME_MODE,
        CONFLICT_RESOLUTION_TIME_MODE,
        BUG_ACTIVATION_TIME_EXPECTED_VALUE,
        PLANNING_TIME_MODE,
        REVIEW_TIME_MODE,
        NUMBER_OF_DEVELOPERS,
        TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR,
        MAX_TASK_SWITCH_OVERHEAD,
        DEPENDENCY_GRAPH_CONSTELLATION,
        "seed"
    };

    private double implementationSkillMode;
    private double reviewSkillMode;
    private double globalBugMode;
    private double conflictPropability;
    private double implementationTimeMean;
    private double bugfixTaskTimeExpectedValue;
    private double reviewRemarkFixTimeExpectedValue;
    private double globalBugSuspendTimeMode;
    private double bugAssessmentTimeMode;
    private double conflictResolutionTimeMode;
    private double bugActivationTimeExpectedValue;
    private double planningTimeMode;
    private double reviewTimeMode;
    private int numberOfDevelopers;
    private double taskSwitchOverheadAfterOneHour;
    private double maxTaskSwitchOverhead;
    private DependencyGraphConstellation dependencyGraphConstellation;

    private int seed;

    private static final class DistributionBuilder {

        private final MersenneTwisterRandomGenerator seedSource;
        private final Model owner;

        public DistributionBuilder(MersenneTwisterRandomGenerator r, Model owner) {
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
                    double conflictPropability,
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
        this.implementationSkillMode = implementationSkillMode;
        this.reviewSkillMode = reviewSkillMode;
        this.globalBugMode = globalBugMode;
        this.conflictPropability = conflictPropability;
        this.implementationTimeMean = implementationTimeMode;
        this.bugfixTaskTimeExpectedValue = bugfixTaskTimeMode;
        this.reviewRemarkFixTimeExpectedValue = fixTimeMode;
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
                0.01, // conflictPropability??
                17.3, // implementationTimeMode
                13.0, //bugfixTaskTimeMode
                1.86, // reviewRemarkfixTimeMode
                0.15, // globalBugSuspendTimeMode??
                0.5, // bugAssessmentTimeMode??
                0.3, // conflictResolutionTimeMode??
                500.0, // bugActivationTimeExpectedValue??
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
                        b.posNormal("implementationSkillDist", this.implementationSkillMode),
                        b.beta("reviewSkillDist", this.reviewSkillMode),
                        b.beta("globalBugDist", this.globalBugMode),
                        b.bernoulli("conflictDist", this.conflictPropability),
                        b.logNormal("implementationTimeDist", this.implementationTimeMean, this.implementationTimeMean / 2.0),
                        b.exp("bugfixTaskTimeDist", this.bugfixTaskTimeExpectedValue),
                        b.exp("reviewRemarkFixTimeDist", this.reviewRemarkFixTimeExpectedValue),
                        b.posNormal("globalBugSuspendTimeDist", this.globalBugSuspendTimeMode),
                        b.posNormal("bugAssessmentTimeDist", this.bugAssessmentTimeMode),
                        b.posNormal("conflictResolutionTimeDist", this.conflictResolutionTimeMode),
                        b.exp("bugActivationTimeDist", this.bugActivationTimeExpectedValue),
                        b.posNormal("planningTimeDist", this.planningTimeMode),
                        b.exp("reviewTimeDist", this.reviewTimeMode),
                        this.numberOfDevelopers,
                        new TimeSpan(this.taskSwitchOverheadAfterOneHour, TimeUnit.HOURS),
                        new TimeSpan(this.maxTaskSwitchOverhead, TimeUnit.HOURS),
                        nextLong(r),
                        this.dependencyGraphConstellation);
    }

    private static long nextLong(MersenneTwisterRandomGenerator r) {
        return ((long)(r.nextInt(32)) << 32) + r.nextInt(32);
    }

    public BulkParameterFactory copyWithChangedSeed() {
        final BulkParameterFactory copy = this.copy();
        copy.seed++;
        return copy;
    }

    public BulkParameterFactory copyWithChangedParamMult(String paramId, double factor) {
        final BulkParameterFactory copy = this.copy();
        final Object oldValue = copy.getParam(paramId);
        if (oldValue instanceof Double) {
            copy.setParam(paramId, ((Double) oldValue) * factor);
        } else if (oldValue instanceof Integer) {
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

    public BulkParameterFactory copyWithChangedParam(String paramId, Object newValue) {
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
        for (final String param : PARAMETER_IDS) {
            experimentData.put(param, this.getParam(param));
        }
    }

    private Object getParam(String param) {
        switch(param) {
        case IMPLEMENTATION_SKILL_MODE: return this.implementationSkillMode;
        case REVIEW_SKILL_MODE: return this.reviewSkillMode;
        case GLOBAL_BUG_MODE: return this.globalBugMode;
        case CONFLICT_PROPABILITY: return this.conflictPropability;
        case IMPLEMENTATION_TIME_MODE: return this.implementationTimeMean;
        case BUGFIX_TASK_TIME_MODE: return this.bugfixTaskTimeExpectedValue;
        case REVIEW_REMARK_FIX_TIME_MODE: return this.reviewRemarkFixTimeExpectedValue;
        case GLOBAL_BUG_SUSPEND_TIME_MODE: return this.globalBugSuspendTimeMode;
        case BUG_ASSESSMENT_TIME_MODE: return this.bugAssessmentTimeMode;
        case CONFLICT_RESOLUTION_TIME_MODE: return this.conflictResolutionTimeMode;
        case BUG_ACTIVATION_TIME_EXPECTED_VALUE: return this.bugActivationTimeExpectedValue;
        case PLANNING_TIME_MODE: return this.planningTimeMode;
        case REVIEW_TIME_MODE: return this.reviewTimeMode;
        case NUMBER_OF_DEVELOPERS: return this.numberOfDevelopers;
        case TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR: return this.taskSwitchOverheadAfterOneHour;
        case MAX_TASK_SWITCH_OVERHEAD: return this.maxTaskSwitchOverhead;
        case DEPENDENCY_GRAPH_CONSTELLATION: return this.dependencyGraphConstellation;
        case "seed": return this.seed;
        default: throw new RuntimeException("invalid parameter id: " + param);
        }
    }

    private void setParam(String param, Object newValue) {
        switch(param) {
        case IMPLEMENTATION_SKILL_MODE:
            this.implementationSkillMode = (Double) newValue;
            break;
        case REVIEW_SKILL_MODE:
            this.reviewSkillMode = (Double) newValue;
            break;
        case GLOBAL_BUG_MODE:
            this.globalBugMode = (Double) newValue;
            break;
        case CONFLICT_PROPABILITY:
            this.conflictPropability = (Double) newValue;
            break;
        case IMPLEMENTATION_TIME_MODE:
            this.implementationTimeMean = (Double) newValue;
            break;
        case BUGFIX_TASK_TIME_MODE:
            this.bugfixTaskTimeExpectedValue = (Double) newValue;
            break;
        case REVIEW_REMARK_FIX_TIME_MODE:
            this.reviewRemarkFixTimeExpectedValue = (Double) newValue;
            break;
        case GLOBAL_BUG_SUSPEND_TIME_MODE:
            this.globalBugSuspendTimeMode = (Double) newValue;
            break;
        case BUG_ASSESSMENT_TIME_MODE:
            this.bugAssessmentTimeMode = (Double) newValue;
            break;
        case CONFLICT_RESOLUTION_TIME_MODE:
            this.conflictResolutionTimeMode = (Double) newValue;
            break;
        case BUG_ACTIVATION_TIME_EXPECTED_VALUE:
            this.bugActivationTimeExpectedValue = (Double) newValue;
            break;
        case PLANNING_TIME_MODE:
            this.planningTimeMode = (Double) newValue;
            break;
        case REVIEW_TIME_MODE:
            this.reviewTimeMode = (Double) newValue;
            break;
        case NUMBER_OF_DEVELOPERS:
            this.numberOfDevelopers = (Integer) newValue;
            break;
        case TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR:
            this.taskSwitchOverheadAfterOneHour = (Double) newValue;
            break;
        case MAX_TASK_SWITCH_OVERHEAD:
            this.maxTaskSwitchOverhead = (Double) newValue;
            break;
        case DEPENDENCY_GRAPH_CONSTELLATION:
            this.dependencyGraphConstellation = (DependencyGraphConstellation) newValue;
            break;
        case "seed":
            this.seed = (Integer) newValue;
            break;
        default:
            throw new RuntimeException("invalid parameter id: " + param);
        }
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

    public List<String> getChangeableParams() {
        final ArrayList<String> ret = new ArrayList<>(Arrays.asList(PARAMETER_IDS));
        ret.remove(DEPENDENCY_GRAPH_CONSTELLATION);
        ret.remove("seed");
        return ret;
    }

}
