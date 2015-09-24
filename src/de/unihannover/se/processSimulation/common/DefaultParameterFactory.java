package de.unihannover.se.processSimulation.common;

import java.util.concurrent.TimeUnit;

import desmoj.core.dist.BoolDistBernoulli;
import desmoj.core.dist.ContDistConstant;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeSpan;

public class DefaultParameterFactory extends ParametersFactory {

    @Override
    public Parameters create(Model owner) {
        return new Parameters(
                        new ContDistConstant(owner, "implementationSkillDist", 0.2, true, true),
                        new ContDistConstant(owner, "reviewSkillDist", 0.6, true, true),
                        new ContDistConstant(owner, "globalBugDist", 0.01, true, true),
                        new BoolDistBernoulli(owner, "conflictDist", 0.05, true, true),
                        new ContDistConstant(owner, "implementationTimeDist", 8.0, true, true),
                        new ContDistConstant(owner, "fixTimeDist", 0.15, true, true),
                        new ContDistConstant(owner, "globalBugSuspendTimeDist", 0.25, true, true),
                        new ContDistConstant(owner, "conflictResolutionTimeDist", 0.25, true, true),
                        new ContDistConstant(owner, "bugActivationTimeDist", 160, true, true),
                        new ContDistConstant(owner, "planningTimeDist", 8.0, true, true),
                        new ContDistConstant(owner, "reviewTimeDist", 1.0, true, true),
                        2,
                        new TimeSpan(5, TimeUnit.MINUTES),
                        new TimeSpan(1, TimeUnit.HOURS),
                        8654);
    }

}