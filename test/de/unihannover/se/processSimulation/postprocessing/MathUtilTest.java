package de.unihannover.se.processSimulation.postprocessing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MathUtilTest {

    private static final double EPSILON = 0.000001;

    @Test
    public void testMean() {
        assertEquals(5.0, MathUtil.determineMean(new double[] {5.0}), EPSILON);
        assertEquals(3.0, MathUtil.determineMean(new double[] {5.0, 1.0, 3.0}), EPSILON);
        assertEquals(3.5, MathUtil.determineMean(new double[] {0.0, 7.0}), EPSILON);
        assertEquals(4.0, MathUtil.determineMean(new double[] {1.0, 1.0, 10.0}), EPSILON);
    }

    @Test
    public void testStdDev() {
        assertEquals(0.0, MathUtil.determineStdDev(new double[] {5.0}), EPSILON);
        assertEquals(2.0, MathUtil.determineStdDev(new double[] {5.0, 1.0, 3.0}), EPSILON);
        assertEquals(5.196152423, MathUtil.determineStdDev(new double[] {1.0, 1.0, 10.0}), EPSILON);
    }

    @Test
    public void testWeightedMean() {
        assertEquals(6.0, MathUtil.determineWeightedMean(new double[] {3.0}, new double[] {6.0}), EPSILON);
        assertEquals(5.0/3.0, MathUtil.determineWeightedMean(new double[] {0.0, 4.0, 2.0}, new double[] {5.0, 1.0, 3.0}), EPSILON);
        assertEquals(4.2, MathUtil.determineWeightedMean(new double[] {2.0, 3.0}, new double[] {0.0, 7.0}), EPSILON);
    }
}
