package de.unihannover.se.processSimulation.common;

import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.Model;

public abstract class ParametersFactory {

    static {
        Experiment.setEpsilon(TimeUnit.MINUTES);
        Experiment.setReferenceUnit(TimeUnit.HOURS);
    }

    public abstract Parameters create(Model model);

    public abstract long getSeed();

    public abstract int getNumberOfDevelopers();

}
