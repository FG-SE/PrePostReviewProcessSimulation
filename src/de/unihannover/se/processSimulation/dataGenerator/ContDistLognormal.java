package de.unihannover.se.processSimulation.dataGenerator;

import java.util.Random;

import desmoj.core.dist.ContDist;
import desmoj.core.simulator.Model;

/**
 * Implementierung der Lognormalverteilung.
 */
public class ContDistLognormal extends ContDist {

    private Random r;
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
            this.r = new Random(seed);
        } else {
            this.r.setSeed(seed);
        }
    }

    @Override
    public Double sample() {
        this.incrementObservations();

        final Double sample = Math.exp(this.location + this.scale * this.r.nextGaussian());

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
