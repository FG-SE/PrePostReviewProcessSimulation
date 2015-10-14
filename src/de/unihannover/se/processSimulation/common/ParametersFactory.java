package de.unihannover.se.processSimulation.common;

import desmoj.core.simulator.Model;

public abstract class ParametersFactory {

    public abstract Parameters create(Model model);

    public abstract long getSeed();

}
