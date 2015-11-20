package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.Random;

import desmoj.core.dist.UniformRandomGenerator;

public class MarsagliaKissRandomGenerator implements UniformRandomGenerator {

    private long z;
    private long y;
    private long c;
    private long x;

    private static final long MASK = (1L << 53) - 1;
    private static final double DOUBLE_UNIT = 0x1.0p-53;

    public MarsagliaKissRandomGenerator(long seed) {
        this.setSeed(seed);
    }

    private static long nextNonZero(Random r) {
        while (true) {
            final long randomValue = r.nextLong();
            if (randomValue != 0) {
                return randomValue;
            }
        }
    }

    @Override
    public double nextDouble() {
        return (this.nextLong() & MASK) * DOUBLE_UNIT;
    }

    public long nextLong() {
        long t;

        // Linearer Kongruenzgenerator
        this.z = 6906969069L*this.z+1234567;

        // Xorshift
        this.y ^= (this.y<<13);
        this.y ^= (this.y>>17);
        this.y ^= (this.y<<43);

        // Multiply-with-carry
        t = (this.x<<58)+this.c;
        this.c = (this.x>>6);
        this.x += t;
        this.c += (this.x<t) ? 0 : 1;

        return this.x + this.y + this.z;
    }

    @Override
    public void setSeed(long seed) {
        final Random r = new Random(seed);
        this.z = nextNonZero(r);
        this.y = nextNonZero(r);
        this.c = nextNonZero(r);
        this.x = nextNonZero(r);
    }

}
