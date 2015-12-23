package de.unihannover.se.processSimulation.dataGenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import desmoj.core.dist.UniformRandomGenerator;
import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.Model;

public class ContDistLognormalTest {

    private static Model dummyModel() {
        final Model ret = new Model(null, "name", false, false) {
            @Override
            public String description() {
                return "desc";
            }
            @Override
            public void doInitialSchedules() {
            }
            @Override
            public void init() {
            }
        };
        final Experiment exp = new Experiment("Experiment",
                        ".\\dummy", null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        ret.connectToExperiment(exp);
        return ret;
    }

    @Test
    public void testMean() {
        final double mean1 = mean(ContDistLognormal.createWithMeanAndMode(dummyModel(), "test", false, false, 10, 5));
        assertEquals(mean1, 10.0, 0.1);
        final double mean2 = mean(ContDistLognormal.createWithMeanAndMode(dummyModel(), "test", false, false, 9, 5));
        assertEquals(mean2, 9.0, 0.1);
        final double mean3 = mean(ContDistLognormal.createWithMeanAndMode(dummyModel(), "test", false, false, 20, 2));
        assertEquals(mean3, 20.0, 0.1);
    }

    private static double mean(ContDistLognormal dist) {
        double sum = 0.0;
        for (int i = 0; i < 2000; i++) {
            sum += dist.sample();
        }
        return sum / 2000.0;
    }

    @Test
    public void testMode() {
        final long mode1 = mode(ContDistLognormal.createWithMeanAndMode(dummyModel(), "test", false, false, 10, 5));
        assertEquals(mode1, 5);
        final long mode2 = mode(ContDistLognormal.createWithMeanAndMode(dummyModel(), "test", false, false, 9, 5));
        assertEquals(mode2, 5);
        final long mode3 = mode(ContDistLognormal.createWithMeanAndMode(dummyModel(), "test", false, false, 20, 2));
        assertEquals(mode3, 2);
    }

    private static long mode(ContDistLognormal dist) {
        final Map<Long, Integer> counts = new HashMap<>();
        for (int i = 0; i < 70000; i++) {
            final long bucket = Math.round(dist.sample());
            if (counts.containsKey(bucket)) {
                counts.put(bucket, counts.get(bucket) + 1);
            } else {
                counts.put(bucket, 1);
            }
        }

        long maxBucket = -1;
        int maxBucketSize = 0;
        for (final Entry<Long, Integer> e : counts.entrySet()) {
            if (e.getValue() > maxBucketSize) {
                maxBucketSize = e.getValue();
                maxBucket = e.getKey();
            }
        }
        return maxBucket;
    }

    @Test
    public void testSetSeed() {
        final ContDistLognormal dist = ContDistLognormal.createWithMeanAndMode(dummyModel(), "test", false, false, 20, 2);
        dist.setSeed(500);
        final List<Double> samples1 = sampleNValues(dist, 100);
        final List<Double> samples2 = sampleNValues(dist, 100);
        dist.setSeed(600);
        final List<Double> samples3 = sampleNValues(dist, 100);
        dist.setSeed(500);
        final List<Double> samples4 = sampleNValues(dist, 100);

        assertEquals(samples1, samples4);
        assertNotEquals(samples1, samples2);
        assertNotEquals(samples2, samples3);
        assertNotEquals(samples1, samples3);
    }

    private static List<Double> sampleNValues(ContDistLognormal dist, int n) {
        final List<Double> ret = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ret.add(dist.sample());
        }
        return ret;
    }

    private static final class TestException extends RuntimeException {
    }

    @Test
    public void testChangeRandomGenerator() {
        final ContDistLognormal dist = ContDistLognormal.createWithMeanAndMode(dummyModel(), "test", false, false, 20, 2);
        dist.changeRandomGenerator(new UniformRandomGenerator() {
            @Override
            public void setSeed(long arg0) {
            }

            @Override
            public double nextDouble() {
                throw new TestException();
            }
        });

        try {
            dist.sample();
            fail("call to nextDouble with exception expected");
        } catch (final TestException e) {
            //everything OK
        }
    }
}
