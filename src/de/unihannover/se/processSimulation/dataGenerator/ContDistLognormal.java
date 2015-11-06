package de.unihannover.se.processSimulation.dataGenerator;

import desmoj.core.dist.ContDist;
import desmoj.core.dist.ContDistNormal;
import desmoj.core.simulator.Model;

/**
 * Implementierung der Lognormalverteilung.
 */
public class ContDistLognormal extends ContDist {

    private ContDistNormal r;
    private final double location;
    private final double scale;

    public ContDistLognormal(Model owner, String name, boolean showInReport, boolean showInTrace, double location, double scale) {
        super(owner, name, showInReport, showInTrace);
        this.location = location;
        this.scale = scale;
    }

    @Override
    public void setSeed(long seed) {
        super.setSeed(seed);
        if (this.r == null) {
            this.r = new ContDistNormal(this.getModel(), this.getName() + "-NormHelper", 0.0, 1.0, false, false);
        } else {
            this.r.setSeed(seed);
        }
    }

    @Override
    public Double sample() {
        this.incrementObservations();

        final Double sample = Math.exp(this.location + this.scale * this.r.sample());

        if (this.currentlySendDebugNotes()) {
            this.traceLastSample(sample+"");
        }

        return sample;
    }

    @Override
    public Double getInverseOfCumulativeProbabilityFunction(double p) {
        throw new UnsupportedOperationException();
    }

}
