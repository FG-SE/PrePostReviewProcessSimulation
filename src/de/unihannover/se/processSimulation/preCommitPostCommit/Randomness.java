package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.concurrent.TimeUnit;

import desmoj.core.dist.NumericalDist;
import desmoj.core.dist.UniformRandomGenerator;
import desmoj.core.simulator.TimeSpan;

public class Randomness {

    private final MarsagliaKissRandomGenerator generator;

    public Randomness(long seed) {
        this(new MarsagliaKissRandomGenerator(seed));
    }

    public Randomness(MarsagliaKissRandomGenerator generator) {
        this.generator = generator;
    }

    public TimeSpan sampleTimeSpan(NumericalDist<Double> dist) {
        this.changeGeneratorFor(dist);
        return dist.sampleTimeSpan(TimeUnit.HOURS);
    }

    public double sampleDouble(NumericalDist<Double> dist) {
        this.changeGeneratorFor(dist);
        return dist.sample();
    }

    private void changeGeneratorFor(NumericalDist<Double> dist) {
        //NumericalDist.changeRandomGenerator resets the seed. We don't want that, so we wrap the real generator.
        final UniformRandomGenerator noResetGenerator = new UniformRandomGenerator() {
            @Override
            public void setSeed(long arg0) {
            }
            @Override
            public double nextDouble() {
                return Randomness.this.generator.nextDouble();
            }
        };
        dist.changeRandomGenerator(noResetGenerator);
    }

    public boolean sampleBoolean(double propabilityForTrue) {
        return this.generator.nextDouble() < propabilityForTrue;
    }

    public Randomness forkRandomNumberStream() {
        return new Randomness(this.sampleLong() + this.sampleLong());
    }

    public long sampleLong() {
        return this.generator.nextLong();
    }

}
