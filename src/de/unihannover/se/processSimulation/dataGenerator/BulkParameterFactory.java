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

public class BulkParameterFactory extends ParametersFactory {

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
        IMPLEMENTATION_SKILL_MODE(Double.class, "Die 'Implementierungs-Fähigkeit' der Entwickler, gemessen in Issues pro Stunde. Das Modell geht davon aus, dass Unit-Tests, Continuous Integration etc bereits vor dem Review gelaufen sind, deshalb dürfen dort auftretende Probleme hier nicht mitzählen. Die konkrete Fähigkeit der Entwickler ergibt sich zufällig (Dreiecksverteilung) auf Basis des angegebenen Werts."),
        IMPLEMENTATION_SKILL_TRIANGLE_WIDTH(Double.class, ""),
        REVIEW_SKILL_MODE(Double.class, "Die 'Review-Fähigkeit' der Entwickler, angegeben als Wahrscheinlichkeit, ein Problem im Review zu entdecken. 1,0 heißt also 'jedes Problem wird entdeckt', 0,0 heißt 'kein Problem wird entdeckt'. Die konkrete Fähigkeit der Entwickler ergibt sich zufällig (Dreiecksverteilung) auf Basis des angegebenen Werts."),
        REVIEW_SKILL_TRIANGLE_WIDTH(Double.class, ""),
        GLOBAL_ISSUE_MODE(Double.class, "Die Wahrscheinlichkeit, dass ein 'globales Problem' beim Implementieren eines Tasks eingebaut wird. 1,0 heißt 'es wird immer eingebaut', 0,0 heißt 'es wird nie eingebaut'."),
        GLOBAL_ISSUE_TRIANGLE_WIDTH(Double.class, ""),
        CONFLICT_PROBABILITY(Double.class, "Die Wahrscheinlichkeit, dass es beim Commit zu einem Konflikt mit einem konkreten anderen Task kommt, der zwischen dem eigenen Update und jetzt commitet hat. Mit anderen Worten die Wahrscheinlichkeit, dass zwei zur gleichen Zeit laufende Tasks die gleichen Stellen betreffen."),
        IMPLEMENTATION_TIME_DIST(DistributionFactory.class, ""),
        IMPLEMENTATION_TIME_MODE(Double.class, ""),
        IMPLEMENTATION_TIME_MEAN_DIFF(Double.class, "Die Dauer für die Implementierung eines Story-Tasks in Stunden (ohne Overhead). Angegeben als Differenz zwischen Modus und arithmetischem Mittel der Log-Normal-Verteilung, entspricht bei üblich kleinen Werten für den Modus also grob dem Mittelwert."),
        ISSUEFIX_TASK_OVERHEAD_TIME_DIST(DistributionFactory.class, ""),
        ISSUEFIX_TASK_OVERHEAD_TIME_MODE(Double.class, ""),
        ISSUEFIX_TASK_OVERHEAD_TIME_MEAN_DIFF(Double.class, "Die Dauer für die Analyse eines Issuefix-Tasks in Stunden (ohne Taskwechsel-Overhead). Die Zeit für das eigentliche Fixen ist dann die gleiche wie beim Review-Remark. Angegeben als Differenz zwischen Modus und arithmetischem Mittel der Log-Normal-Verteilung, entspricht bei üblich kleinen Werten für den Modus also grob dem Mittelwert."),
        REVIEW_REMARK_FIX_TIME_DIST(DistributionFactory.class, ""),
        REVIEW_REMARK_FIX_TIME_MODE(Double.class, ""),
        REVIEW_REMARK_FIX_TIME_MEAN_DIFF(Double.class, "Die Dauer für die Korrektur einer einzelnen Review-Anmerkung in Stunden (ohne Overhead). Angegeben als Differenz zwischen Modus und arithmetischem Mittel der Log-Normal-Verteilung, entspricht bei üblich kleinen Werten für den Modus also grob dem Mittelwert."),
        GLOBAL_ISSUE_SUSPEND_TIME_MODE(Double.class, "Dauer der Unterbrechung der Implementierung durch ein 'globales Problem', angegeben in Stunden (Modus einer Dreiecksverteilung)."),
        GLOBAL_ISSUE_SUSPEND_TIME_TRIANGLE_WIDTH(Double.class, ""),
        ISSUE_ASSESSMENT_TIME_MODE(Double.class, ""),
        ISSUE_ASSESSMENT_TIME_MEAN_DIFF(Double.class, ""),
        CONFLICT_RESOLUTION_TIME_MODE(Double.class, "Dauer des Commit-Konfliktauflösung, angegeben in Stunden (Modus einer Dreiecksverteilung)."),
        CONFLICT_RESOLUTION_TIME_TRIANGLE_WIDTH(Double.class, ""),
        INTERNAL_ISSUE_SHARE(Double.class, "Anteil an internen (Wartbarkeits- o.ä.) Problemen an der Gesamtzahl"),
        ISSUE_ACTIVATION_TIME_DEVELOPER_MODE(Double.class, ""),
        ISSUE_ACTIVATION_TIME_DEVELOPER_MEAN_DIFF(Double.class, "Durchschnittliche Zeit in Stunden zwischen Commit eines Problems und (zufälliger) Entdeckung durch einen Entwickler (Mittelwert einer Exponentialverteilung)."),
        ISSUE_ACTIVATION_TIME_CUSTOMER_MODE(Double.class, ""),
        ISSUE_ACTIVATION_TIME_CUSTOMER_MEAN_DIFF(Double.class, "Durchschnittliche Zeit in Stunden zwischen 'Auslieferung' eines Problems und Entdeckung durch einen Kunden (Mittelwert einer Exponentialverteilung)."),
        PLANNING_TIME_DIST(DistributionFactory.class, ""),
        PLANNING_TIME_MEAN(Double.class, ""),
        REVIEW_TIME_DIST(DistributionFactory.class, ""),
        REVIEW_TIME_MODE(Double.class, ""),
        REVIEW_TIME_MEAN_DIFF(Double.class, "Die Dauer für ein Review eines Tasks in Stunden (ohne Overhead). Angegeben als Differenz zwischen Modus und arithmetischem Mittel der Log-Normal-Verteilung, entspricht bei üblich kleinen Werten für den Modus also grob dem Mittelwert."),
        NUMBER_OF_DEVELOPERS(Integer.class, "Anzahl der Entwickler im Team"),
        TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR(Double.class, "Zeit um wieder in ein Thema reinzukommen, nachdem man sich eine Stunde lang mit etwas anderem beschäftigt hat."),
        MAX_TASK_SWITCH_OVERHEAD(Double.class, "Zeit um wieder in ein Thema reinzukommen, nachdem man sich sehr lange mit etwas anderem beschäftigt hat oder sich noch nie mit dem Thema beschäftigt hatte."),
        BOARD_SEARCH_CUTOFF_LIMIT(Integer.class, ""),
        TASK_SWITCH_TIME_ISSUE_FACTOR(Double.class, ""),
        FIXING_ISSUE_RATE_FACTOR(Double.class, "Faktor der festlegt, wie viel 'ungefährlicher' Korrekturen gegenüber Neuentwicklung sind. Wird als Multiplikator des Implementierungs-Skills des Entwicklers aufgefasst. 1,0 heißt 'bei Fixes und Neuentwicklung macht man gleich viel Fehler pro Stunde', Werte kleiner 0 heißen 'bei Fixes macht man weniger Fehler pro Stunde', Werte größer 0 heißen 'bei Fixes macht man mehr Fehler pro Stunde'."),
        FOLLOW_UP_ISSUE_SPAWN_PROBABILITY(Double.class, ""),
        REVIEW_FIX_TO_TASK_FACTOR(Double.class, ""),
        DEPENDENCY_GRAPH_CONSTELLATION(DependencyGraphConstellation.class, "Struktur der Abhängigkeiten zwischen Story-Tasks, d.h. inwiefern andere Tasks erst begonnen werden können, nachdem andere commitet sind. 'REALISTIC' beruht z.B. auf Echtdaten, bei 'NO_DEPENDENCIES' gibt es keine Abhängigkeiten, und bei 'CHAINS' und 'DIAMONDS' gibt es recht viele.");


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
                return (int) Double.parseDouble(param);
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
            return this.setSeed(ContDistLognormal.createWithMeanAndMode(this.owner, name, true, true, mean, mode));
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
        final BulkParameterFactory ret = new BulkParameterFactory();
        ret.parameters.put(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.26);
        ret.parameters.put(ParameterType.IMPLEMENTATION_SKILL_TRIANGLE_WIDTH, 0.1);
        ret.parameters.put(ParameterType.REVIEW_SKILL_MODE, 0.45);
        ret.parameters.put(ParameterType.REVIEW_SKILL_TRIANGLE_WIDTH, 0.05);
        ret.parameters.put(ParameterType.GLOBAL_ISSUE_MODE, 0.001);
        ret.parameters.put(ParameterType.GLOBAL_ISSUE_TRIANGLE_WIDTH, 0.001);
        ret.parameters.put(ParameterType.CONFLICT_PROBABILITY, 0.012);
        ret.parameters.put(ParameterType.IMPLEMENTATION_TIME_DIST, DistributionFactory.LOGNORMAL);
        ret.parameters.put(ParameterType.IMPLEMENTATION_TIME_MODE, 0.2);
        ret.parameters.put(ParameterType.IMPLEMENTATION_TIME_MEAN_DIFF, 9.5);
        ret.parameters.put(ParameterType.ISSUEFIX_TASK_OVERHEAD_TIME_DIST, DistributionFactory.LOGNORMAL);
        ret.parameters.put(ParameterType.ISSUEFIX_TASK_OVERHEAD_TIME_MODE, 0.01317843);
        ret.parameters.put(ParameterType.ISSUEFIX_TASK_OVERHEAD_TIME_MEAN_DIFF, 5.5);
        ret.parameters.put(ParameterType.REVIEW_REMARK_FIX_TIME_DIST, DistributionFactory.LOGNORMAL);
        ret.parameters.put(ParameterType.REVIEW_REMARK_FIX_TIME_MODE, 0.02);
        ret.parameters.put(ParameterType.REVIEW_REMARK_FIX_TIME_MEAN_DIFF, 0.7);
        ret.parameters.put(ParameterType.GLOBAL_ISSUE_SUSPEND_TIME_MODE, 0.15);
        ret.parameters.put(ParameterType.GLOBAL_ISSUE_SUSPEND_TIME_TRIANGLE_WIDTH, 0.1);
        ret.parameters.put(ParameterType.ISSUE_ASSESSMENT_TIME_MODE, 0.1);
        ret.parameters.put(ParameterType.ISSUE_ASSESSMENT_TIME_MEAN_DIFF, 0.35);
        ret.parameters.put(ParameterType.CONFLICT_RESOLUTION_TIME_MODE, 0.3);
        ret.parameters.put(ParameterType.CONFLICT_RESOLUTION_TIME_TRIANGLE_WIDTH, 0.2);
        ret.parameters.put(ParameterType.INTERNAL_ISSUE_SHARE, 0.5);
        ret.parameters.put(ParameterType.ISSUE_ACTIVATION_TIME_DEVELOPER_MODE, 0.5);
        ret.parameters.put(ParameterType.ISSUE_ACTIVATION_TIME_DEVELOPER_MEAN_DIFF, 2000.0);
        ret.parameters.put(ParameterType.ISSUE_ACTIVATION_TIME_CUSTOMER_MODE, 4.0);
        ret.parameters.put(ParameterType.ISSUE_ACTIVATION_TIME_CUSTOMER_MEAN_DIFF, 1000.0);
        ret.parameters.put(ParameterType.PLANNING_TIME_DIST, DistributionFactory.LOGNORMAL);
        ret.parameters.put(ParameterType.PLANNING_TIME_MEAN, 4.0);
        ret.parameters.put(ParameterType.REVIEW_TIME_DIST, DistributionFactory.LOGNORMAL);
        ret.parameters.put(ParameterType.REVIEW_TIME_MODE, 0.03225049);
        ret.parameters.put(ParameterType.REVIEW_TIME_MEAN_DIFF, 1.6254);
        ret.parameters.put(ParameterType.NUMBER_OF_DEVELOPERS, 12);
        ret.parameters.put(ParameterType.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR, new TimeSpan(5, TimeUnit.MINUTES).getTimeAsDouble(TimeUnit.HOURS));
        ret.parameters.put(ParameterType.MAX_TASK_SWITCH_OVERHEAD, new TimeSpan(30, TimeUnit.MINUTES).getTimeAsDouble(TimeUnit.HOURS));
        ret.parameters.put(ParameterType.BOARD_SEARCH_CUTOFF_LIMIT, 100);
        ret.parameters.put(ParameterType.TASK_SWITCH_TIME_ISSUE_FACTOR, 0.0);
        ret.parameters.put(ParameterType.FIXING_ISSUE_RATE_FACTOR, 0.3);
        ret.parameters.put(ParameterType.FOLLOW_UP_ISSUE_SPAWN_PROBABILITY, 0.005);
        ret.parameters.put(ParameterType.REVIEW_FIX_TO_TASK_FACTOR, 1.1);
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
                        b.triangularProbability("globalIssueDist",
                                        this.getParamD(ParameterType.GLOBAL_ISSUE_MODE),
                                        this.getParamD(ParameterType.GLOBAL_ISSUE_TRIANGLE_WIDTH)),
                        b.bernoulli("conflictDist",
                                        this.getParamD(ParameterType.CONFLICT_PROBABILITY)),
                        this.getParamDi(ParameterType.IMPLEMENTATION_TIME_DIST).create(b, "implementationTimeDist",
                                        this.getParamD(ParameterType.IMPLEMENTATION_TIME_MODE) + this.getParamD(ParameterType.IMPLEMENTATION_TIME_MEAN_DIFF),
                                        this.getParamD(ParameterType.IMPLEMENTATION_TIME_MODE)),
                        this.getParamDi(ParameterType.ISSUEFIX_TASK_OVERHEAD_TIME_DIST).create(b, "issuefixTaskOverheadTimeDist",
                                        this.getParamD(ParameterType.ISSUEFIX_TASK_OVERHEAD_TIME_MODE) + this.getParamD(ParameterType.ISSUEFIX_TASK_OVERHEAD_TIME_MEAN_DIFF),
                                        this.getParamD(ParameterType.ISSUEFIX_TASK_OVERHEAD_TIME_MODE)),
                        this.getParamDi(ParameterType.REVIEW_REMARK_FIX_TIME_DIST).create(b, "reviewRemarkFixTimeDist",
                                        this.getParamD(ParameterType.REVIEW_REMARK_FIX_TIME_MODE) + this.getParamD(ParameterType.REVIEW_REMARK_FIX_TIME_MEAN_DIFF),
                                        this.getParamD(ParameterType.REVIEW_REMARK_FIX_TIME_MODE)),
                        b.triangular("globalIssueSuspendTimeDist",
                                        this.getParamD(ParameterType.GLOBAL_ISSUE_SUSPEND_TIME_MODE),
                                        this.getParamD(ParameterType.GLOBAL_ISSUE_SUSPEND_TIME_TRIANGLE_WIDTH),
                                        0,
                                        Double.MAX_VALUE),
                        b.logNormal("issueAssessmentTimeDist",
                                        this.getParamD(ParameterType.ISSUE_ASSESSMENT_TIME_MODE) + this.getParamD(ParameterType.ISSUE_ASSESSMENT_TIME_MEAN_DIFF),
                                        this.getParamD(ParameterType.ISSUE_ASSESSMENT_TIME_MODE)),
                        b.triangular("conflictResolutionTimeDist",
                                        this.getParamD(ParameterType.CONFLICT_RESOLUTION_TIME_MODE),
                                        this.getParamD(ParameterType.CONFLICT_RESOLUTION_TIME_TRIANGLE_WIDTH),
                                        0,
                                        Double.MAX_VALUE),
                        b.bernoulli("internalIssueDist",
                                        this.getParamD(ParameterType.INTERNAL_ISSUE_SHARE)),
                        b.expShift("issueActivationTimeDeveloperDist",
                                        this.getParamD(ParameterType.ISSUE_ACTIVATION_TIME_DEVELOPER_MODE) + this.getParamD(ParameterType.ISSUE_ACTIVATION_TIME_DEVELOPER_MEAN_DIFF),
                                        this.getParamD(ParameterType.ISSUE_ACTIVATION_TIME_DEVELOPER_MODE)),
                        b.expShift("issueActivationTimeCustomerDist",
                                        this.getParamD(ParameterType.ISSUE_ACTIVATION_TIME_CUSTOMER_MODE) + this.getParamD(ParameterType.ISSUE_ACTIVATION_TIME_CUSTOMER_MEAN_DIFF),
                                        this.getParamD(ParameterType.ISSUE_ACTIVATION_TIME_CUSTOMER_MODE)),
                        this.getParamDi(ParameterType.PLANNING_TIME_DIST).create(b, "planningTimeDist",
                                        this.getParamD(ParameterType.PLANNING_TIME_MEAN)),
                        this.getParamDi(ParameterType.REVIEW_TIME_DIST).create(b, "reviewTimeDist",
                                        this.getParamD(ParameterType.REVIEW_TIME_MODE) + this.getParamD(ParameterType.REVIEW_TIME_MEAN_DIFF),
                                        this.getParamD(ParameterType.REVIEW_TIME_MODE)),
                        this.getParamI(ParameterType.NUMBER_OF_DEVELOPERS),
                        new TimeSpan(this.getParamD(ParameterType.TASK_SWITCH_OVERHEAD_AFTER_ONE_HOUR), TimeUnit.HOURS),
                        new TimeSpan(this.getParamD(ParameterType.MAX_TASK_SWITCH_OVERHEAD), TimeUnit.HOURS),
                        this.getParamI(ParameterType.BOARD_SEARCH_CUTOFF_LIMIT),
                        this.getParamD(ParameterType.TASK_SWITCH_TIME_ISSUE_FACTOR),
                        this.getParamD(ParameterType.FIXING_ISSUE_RATE_FACTOR),
                        this.getParamD(ParameterType.FOLLOW_UP_ISSUE_SPAWN_PROBABILITY),
                        this.getParamD(ParameterType.REVIEW_FIX_TO_TASK_FACTOR),
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
        final BulkParameterFactory copy = new BulkParameterFactory();
        copy.seed = this.seed;
        copy.parameters.putAll(this.parameters);
        return copy;
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
