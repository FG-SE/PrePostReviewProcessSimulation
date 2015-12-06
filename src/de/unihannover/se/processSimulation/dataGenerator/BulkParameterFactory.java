/**
    This file is part of LUH PrePostReview Process Simulation.

    LUH PrePostReview Process Simulation is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    LUH PrePostReview Process Simulation is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with LUH PrePostReview Process Simulation. If not, see <http://www.gnu.org/licenses/>.
 */

package de.unihannover.se.processSimulation.dataGenerator;

import java.util.EnumMap;
import java.util.concurrent.TimeUnit;

import de.unihannover.se.processSimulation.common.Parameters;
import de.unihannover.se.processSimulation.common.ParametersFactory;
import de.unihannover.se.processSimulation.preCommitPostCommit.DependencyGraphConstellation;
import desmoj.core.dist.BoolDistBernoulli;
import desmoj.core.dist.ContDist;
import desmoj.core.dist.ContDistAggregate;
import desmoj.core.dist.ContDistBeta;
import desmoj.core.dist.ContDistConstant;
import desmoj.core.dist.ContDistExponential;
import desmoj.core.dist.ContDistNormal;
import desmoj.core.dist.ContDistTriangular;
import desmoj.core.dist.Distribution;
import desmoj.core.dist.MersenneTwisterRandomGenerator;
import desmoj.core.dist.NumericalDist;
import desmoj.core.dist.Operator;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeSpan;

public class BulkParameterFactory extends ParametersFactory implements Cloneable {

    public enum DistributionFactory {
        POSNORMAL {
            @Override
            public NumericalDist<Double> create(DistributionBuilder b, String name, double mean, double mode) {
                throw new AssertionError("Two parameter create not supported for pos-normal");
            }

            @Override
            public NumericalDist<Double> create(DistributionBuilder b, String name, double mean) {
                return b.posNormal(name, mean);
            }
        },
        LOGNORMAL {
            @Override
            public NumericalDist<Double> create(DistributionBuilder b, String name, double mean, double mode) {
                return b.logNormal(name, mean, mode);
            }

            @Override
            public NumericalDist<Double> create(DistributionBuilder b, String name, double mean) {
                return b.logNormal(name, mean, mean / 5.0);
            }
        },
        EXPSHIFT {
            @Override
            public NumericalDist<Double> create(DistributionBuilder b, String name, double mean, double mode) {
                return b.expShift(name, mean, mode);
            }

            @Override
            public NumericalDist<Double> create(DistributionBuilder b, String name, double mean) {
                return b.exp(name, mean);
            }
        };

        public abstract NumericalDist<Double> create(DistributionBuilder b, String name, double mean, double mode);

        public abstract NumericalDist<Double> create(DistributionBuilder b, String name, double mean);
    }

    public enum ParameterType {
        IMPLEMENTATION_SKILL_MODE(Double.class, ""),
        IMPLEMENTATION_SKILL_TRIANGLE_WIDTH(Double.class, ""),
        REVIEW_SKILL_MODE(Double.class, ""),
        REVIEW_SKILL_TRIANGLE_WIDTH(Double.class, ""),
        GLOBAL_BUG_MODE(Double.class, ""),
        GLOBAL_BUG_TRIANGLE_WIDTH(Double.class, ""),
        CONFLICT_PROBABILITY(Double.class, ""),
        IMPLEMENTATION_TIME_DIST(DistributionFactory.class, ""),
        IMPLEMENTATION_TIME_MODE(Double.class, ""),
        IMPLEMENTATION_TIME_MEAN_DIFF(Double.class, ""),
        BUGFIX_TASK_TIME_DIST(DistributionFactory.class, ""),
        BUGFIX_TASK_TIME_MODE(Double.class, ""),
        BUGFIX_TASK_TIME_MEAN_DIFF(Double.class, ""),
        REVIEW_REMARK_FIX_TIME_DIST(DistributionFactory.class, ""),
        REVIEW_REMARK_FIX_TIME_MODE(Double.class, ""),
        REVIEW_REMARK_FIX_TIME_MEAN_DIFF(Double.class, ""),
        GLOBAL_BUG_SUSPEND_TIME_MODE(Double.class, ""),
        GLOBAL_BUG_SUSPEND_TIME_TRIANGLE_WIDTH(Double.class, ""),
        BUG_ASSESSMENT_TIME_MODE(Double.class, ""),
        BUG_ASSESSMENT_TIME_TRIANGLE_WIDTH(Double.class, ""),
        CONFLICT_RESOLUTION_TIME_MODE(Double.class, ""),
        CONFLICT_RESOLUTION_TIME_TRIANGLE_WIDTH(Double.class, ""),
        INTERNAL_BUG_SHARE(Double.class, ""),
        BUG_ACTIVATION_TIME_DEVELOPER_EXPECTED_VALUE(Double.class, ""),
        BUG_ACTIVATION_TIME_CUSTOMER_EXPECTED_VALUE(Double.class, ""),
        PLANNING_TIME_DIST(DistributionFactory.class, ""),
        PLANNING_TIME_MEAN(Double.class, ""),
        REVIEW_TIME_DIST(DistributionFactory.class, ""),
        REVIEW_TIME_MODE(Double.class, ""),
        REVIEW_TIME_MEAN_DIFF(Double.class, ""),
        NUMBER_OF_DEVELOPERS(Integer.class, ""),
        TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR(Double.class, ""),
        MAX_TASK_SWITCH_OVERHEAD(Double.class, ""),
        BOARD_SEARCH_CUTOFF_LIMIT(Integer.class, ""),
        TASK_SWITCH_TIME_BUG_FACTOR(Double.class, ""),
        FIXING_BUG_RATE_FACTOR(Double.class, ""),
        FOLLOW_UP_BUG_SPAWN_PROBABILITY(Double.class, ""),
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

        public ContDist triangularProbability(String name, double mostProbableValue, double width) {
            return this.triangular(name, mostProbableValue, width, 0.0, 1.0);
        }

        public ContDist triangular(String name, double mostProbableValue, double width, double min, double max) {
            assert width <= (max - min);
            if (width == 0.0) {
                return this.setSeed(new ContDistConstant(this.owner, name, mostProbableValue, true, true));
            }
            double lower = mostProbableValue - width / 2.0;
            double upper = mostProbableValue + width / 2.0;
            if (upper > max) {
                lower -= upper - max;
                upper = max;
            }
            if (lower < min) {
                upper -= lower - min;
                lower = min;
            }
            return this.setSeed(new ContDistTriangular(this.owner, name, lower, upper, mostProbableValue, true, true));
        }

        public ContDistExponential exp(String name, double expectedValue) {
            return this.setSeed(new ContDistExponential(this.owner, name, expectedValue, true, true));
        }

        public ContDist expShift(String name, double mean, double mode) {
            final ContDistExponential exp = this.setSeed(new ContDistExponential(this.owner, name, mean - mode, false, false));
            final ContDistConstant shift = new ContDistConstant(this.owner, name + "-shift", mode, false, false);
            return this.setSeed(new ContDistAggregate(this.owner,
                            name,
                            exp,
                            shift,
                            Operator.PLUS,
                            true,
                            true));
        }

        public BoolDistBernoulli bernoulli(String name, double probabilityForTrue) {
            return this.setSeed(new BoolDistBernoulli(this.owner, name, probabilityForTrue, true, true));
        }

        public ContDistNormal posNormal(String name, double mode) {
            return this.posNormal(name, mode, 0.1);
        }

        public ContDistNormal posNormal(String name, double mode, double stdDevFactor) {
            final ContDistNormal dist = new ContDistNormal(this.owner, name, mode, mode * stdDevFactor, true, true);
            dist.setNonNegative(true);
            return this.setSeed(dist);
        }

        public ContDistLognormal logNormal(String name, double mean, double mode) {
            assert mean >= mode;
            final double mu = (2 * Math.log(mean) + Math.log(mode)) / 3;
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

    private BulkParameterFactory() {
        this.seed = 764;
    }

    public static BulkParameterFactory forCommercial() {
        //implementationSkill calculation:
        //  4196 review remarks in tickets that took 46879 hours
        //  and (508+542) new bugs in last two years, from (940+923) tasks
        //  and rough guess of about 1000 more problems in the same tasks
        //  => 4196/46879 + 2000/(1900*15) = 0.09 + 0.07 = 0.16

        final BulkParameterFactory ret = new BulkParameterFactory();
        ret.parameters.put(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.16);
        ret.parameters.put(ParameterType.IMPLEMENTATION_SKILL_TRIANGLE_WIDTH, 0.1);
        ret.parameters.put(ParameterType.REVIEW_SKILL_MODE, 0.56);
        ret.parameters.put(ParameterType.REVIEW_SKILL_TRIANGLE_WIDTH, 0.05);
        ret.parameters.put(ParameterType.GLOBAL_BUG_MODE, 0.001);
        ret.parameters.put(ParameterType.GLOBAL_BUG_TRIANGLE_WIDTH, 0.001);
        ret.parameters.put(ParameterType.CONFLICT_PROBABILITY, 0.01);
        ret.parameters.put(ParameterType.IMPLEMENTATION_TIME_DIST, DistributionFactory.LOGNORMAL);
        ret.parameters.put(ParameterType.IMPLEMENTATION_TIME_MODE, 0.3357338);
        ret.parameters.put(ParameterType.IMPLEMENTATION_TIME_MEAN_DIFF, 19.8);
        ret.parameters.put(ParameterType.BUGFIX_TASK_TIME_DIST, DistributionFactory.LOGNORMAL);
        ret.parameters.put(ParameterType.BUGFIX_TASK_TIME_MODE, 0.03317843);
        ret.parameters.put(ParameterType.BUGFIX_TASK_TIME_MEAN_DIFF, 15.77376);
        ret.parameters.put(ParameterType.REVIEW_REMARK_FIX_TIME_DIST, DistributionFactory.LOGNORMAL);
        ret.parameters.put(ParameterType.REVIEW_REMARK_FIX_TIME_MODE, 0.02);
        ret.parameters.put(ParameterType.REVIEW_REMARK_FIX_TIME_MEAN_DIFF, 1.8);
        ret.parameters.put(ParameterType.GLOBAL_BUG_SUSPEND_TIME_MODE, 0.15);
        ret.parameters.put(ParameterType.GLOBAL_BUG_SUSPEND_TIME_TRIANGLE_WIDTH, 0.1);
        ret.parameters.put(ParameterType.BUG_ASSESSMENT_TIME_MODE, 0.5);
        ret.parameters.put(ParameterType.BUG_ASSESSMENT_TIME_TRIANGLE_WIDTH, 0.5);
        ret.parameters.put(ParameterType.CONFLICT_RESOLUTION_TIME_MODE, 0.3);
        ret.parameters.put(ParameterType.CONFLICT_RESOLUTION_TIME_TRIANGLE_WIDTH, 0.2);
        ret.parameters.put(ParameterType.INTERNAL_BUG_SHARE, 0.6);
        ret.parameters.put(ParameterType.BUG_ACTIVATION_TIME_DEVELOPER_EXPECTED_VALUE, 1000.0);
        ret.parameters.put(ParameterType.BUG_ACTIVATION_TIME_CUSTOMER_EXPECTED_VALUE, 1000.0);
        ret.parameters.put(ParameterType.PLANNING_TIME_DIST, DistributionFactory.LOGNORMAL);
        ret.parameters.put(ParameterType.PLANNING_TIME_MEAN, 4.0);
        ret.parameters.put(ParameterType.REVIEW_TIME_DIST, DistributionFactory.LOGNORMAL);
        ret.parameters.put(ParameterType.REVIEW_TIME_MODE, 0.03225049);
        ret.parameters.put(ParameterType.REVIEW_TIME_MEAN_DIFF, 1.6254);
        ret.parameters.put(ParameterType.NUMBER_OF_DEVELOPERS, 12);
        ret.parameters.put(ParameterType.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, new TimeSpan(5, TimeUnit.MINUTES).getTimeAsDouble(TimeUnit.HOURS));
        ret.parameters.put(ParameterType.MAX_TASK_SWITCH_OVERHEAD, new TimeSpan(30, TimeUnit.MINUTES).getTimeAsDouble(TimeUnit.HOURS));
        ret.parameters.put(ParameterType.BOARD_SEARCH_CUTOFF_LIMIT, 100);
        ret.parameters.put(ParameterType.TASK_SWITCH_TIME_BUG_FACTOR, 0.0);
        ret.parameters.put(ParameterType.FIXING_BUG_RATE_FACTOR, 0.5);
        ret.parameters.put(ParameterType.FOLLOW_UP_BUG_SPAWN_PROBABILITY, 0.05);
        ret.parameters.put(ParameterType.DEPENDENCY_GRAPH_CONSTELLATION, DependencyGraphConstellation.REALISTIC);
        return ret;
    }


    @Override
    public Parameters create(Model owner) {
        final MersenneTwisterRandomGenerator r = new MersenneTwisterRandomGenerator(this.seed);
        final DistributionBuilder b = new DistributionBuilder(r, owner);
        return new Parameters(
                        b.triangular("implementationSkillDist",
                                        this.getParamD(ParameterType.IMPLEMENTATION_SKILL_MODE),
                                        this.getParamD(ParameterType.IMPLEMENTATION_SKILL_TRIANGLE_WIDTH),
                                        0.0,
                                        Double.MAX_VALUE),
                        b.triangularProbability("reviewSkillDist",
                                        this.getParamD(ParameterType.REVIEW_SKILL_MODE),
                                        this.getParamD(ParameterType.REVIEW_SKILL_TRIANGLE_WIDTH)),
                        b.triangularProbability("globalBugDist",
                                        this.getParamD(ParameterType.GLOBAL_BUG_MODE),
                                        this.getParamD(ParameterType.GLOBAL_BUG_TRIANGLE_WIDTH)),
                        b.bernoulli("conflictDist",
                                        this.getParamD(ParameterType.CONFLICT_PROBABILITY)),
                        this.getParamDi(ParameterType.IMPLEMENTATION_TIME_DIST).create(b, "implementationTimeDist",
                                        this.getParamD(ParameterType.IMPLEMENTATION_TIME_MODE) + this.getParamD(ParameterType.IMPLEMENTATION_TIME_MEAN_DIFF),
                                        this.getParamD(ParameterType.IMPLEMENTATION_TIME_MODE)),
                        this.getParamDi(ParameterType.BUGFIX_TASK_TIME_DIST).create(b, "bugfixTaskTimeDist",
                                        this.getParamD(ParameterType.BUGFIX_TASK_TIME_MODE) + this.getParamD(ParameterType.BUGFIX_TASK_TIME_MEAN_DIFF),
                                        this.getParamD(ParameterType.BUGFIX_TASK_TIME_MODE)),
                        this.getParamDi(ParameterType.REVIEW_REMARK_FIX_TIME_DIST).create(b, "reviewRemarkFixTimeDist",
                                        this.getParamD(ParameterType.REVIEW_REMARK_FIX_TIME_MODE) + this.getParamD(ParameterType.REVIEW_REMARK_FIX_TIME_MEAN_DIFF),
                                        this.getParamD(ParameterType.REVIEW_REMARK_FIX_TIME_MODE)),
                        b.triangular("globalBugSuspendTimeDist",
                                        this.getParamD(ParameterType.GLOBAL_BUG_SUSPEND_TIME_MODE),
                                        this.getParamD(ParameterType.GLOBAL_BUG_SUSPEND_TIME_TRIANGLE_WIDTH),
                                        0,
                                        Double.MAX_VALUE),
                        b.triangular("bugAssessmentTimeDist",
                                        this.getParamD(ParameterType.BUG_ASSESSMENT_TIME_MODE),
                                        this.getParamD(ParameterType.BUG_ASSESSMENT_TIME_TRIANGLE_WIDTH),
                                        0,
                                        Double.MAX_VALUE),
                        b.triangular("conflictResolutionTimeDist",
                                        this.getParamD(ParameterType.CONFLICT_RESOLUTION_TIME_MODE),
                                        this.getParamD(ParameterType.CONFLICT_RESOLUTION_TIME_TRIANGLE_WIDTH),
                                        0,
                                        Double.MAX_VALUE),
                        b.bernoulli("internalBugDist",
                                        this.getParamD(ParameterType.INTERNAL_BUG_SHARE)),
                        b.exp("bugActivationTimeDeveloperDist",
                                        this.getParamD(ParameterType.BUG_ACTIVATION_TIME_DEVELOPER_EXPECTED_VALUE)),
                        b.exp("bugActivationTimeCustomerDist",
                                        this.getParamD(ParameterType.BUG_ACTIVATION_TIME_CUSTOMER_EXPECTED_VALUE)),
                        this.getParamDi(ParameterType.PLANNING_TIME_DIST).create(b, "planningTimeDist",
                                        this.getParamD(ParameterType.PLANNING_TIME_MEAN)),
                        this.getParamDi(ParameterType.REVIEW_TIME_DIST).create(b, "reviewTimeDist",
                                        this.getParamD(ParameterType.REVIEW_TIME_MODE) + this.getParamD(ParameterType.REVIEW_TIME_MEAN_DIFF),
                                        this.getParamD(ParameterType.REVIEW_TIME_MODE)),
                        this.getParamI(ParameterType.NUMBER_OF_DEVELOPERS),
                        new TimeSpan(this.getParamD(ParameterType.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR), TimeUnit.HOURS),
                        new TimeSpan(this.getParamD(ParameterType.MAX_TASK_SWITCH_OVERHEAD), TimeUnit.HOURS),
                        this.getParamI(ParameterType.BOARD_SEARCH_CUTOFF_LIMIT),
                        this.getParamD(ParameterType.TASK_SWITCH_TIME_BUG_FACTOR),
                        this.getParamD(ParameterType.FIXING_BUG_RATE_FACTOR),
                        this.getParamD(ParameterType.FOLLOW_UP_BUG_SPAWN_PROBABILITY),
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

    public Object getParam(ParameterType param) {
        assert this.parameters.containsKey(param);
        return this.parameters.get(param);
    }

    private double getParamD(ParameterType param) {
        return (Double) this.getParam(param);
    }

    private DistributionFactory getParamDi(ParameterType param) {
        return (DistributionFactory) this.getParam(param);
    }

    private int getParamI(ParameterType param) {
        return (Integer) this.getParam(param);
    }

    private void setParam(ParameterType param, Object newValue) {
        param.checkType(newValue);
        this.parameters.put(param, newValue);
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
