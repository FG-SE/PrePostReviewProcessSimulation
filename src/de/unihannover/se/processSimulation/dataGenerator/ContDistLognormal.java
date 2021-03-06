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

import desmoj.core.dist.ContDist;
import desmoj.core.dist.ContDistNormal;
import desmoj.core.dist.UniformRandomGenerator;
import desmoj.core.simulator.Model;

/**
 * Implementation of the log-normal distribution.
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
    public void changeRandomGenerator(UniformRandomGenerator rg) {
        super.changeRandomGenerator(rg);
        this.r.changeRandomGenerator(rg);
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

    /**
     * Creates a log-normal distribution with the given mean and mode.
     */
    public static ContDistLognormal createWithMeanAndMode(Model owner, String name, boolean showInReport, boolean showInTrace, double mean, double mode) {
        assert mean >= mode;
        final double mu = (2 * Math.log(mean) + Math.log(mode)) / 3;
        final double sigma = Math.sqrt(2.0 * (Math.log(mean) - mu));
        return new ContDistLognormal(owner, name, showInReport, showInTrace, mu, sigma);
    }

}
