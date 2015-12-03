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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StatisticsUtilTest {

    private static final double DELTA = 0.00001;

    private static double[] createNumbers(int count) {
        final double[] ret = new double[count];
        for (int i = 1; i <= count; i++) {
            ret[i - 1] = i;
        }
        return ret;
    }

    private static MedianWithConfidenceInterval median(double median, double lower, double upper) {
        return new MedianWithConfidenceInterval(median, lower, upper);
    }

    @Test
    public void testQBinom() {
        assertEquals(0, StatisticsUtil.qBinom(0.0, 4, 0.5), DELTA);
        assertEquals(1, StatisticsUtil.qBinom(0.1, 4, 0.5), DELTA);
        assertEquals(1, StatisticsUtil.qBinom(0.2, 4, 0.5), DELTA);
        assertEquals(1, StatisticsUtil.qBinom(0.3, 4, 0.5), DELTA);
        assertEquals(2, StatisticsUtil.qBinom(0.4, 4, 0.5), DELTA);
        assertEquals(2, StatisticsUtil.qBinom(0.5, 4, 0.5), DELTA);
        assertEquals(2, StatisticsUtil.qBinom(0.6, 4, 0.5), DELTA);
        assertEquals(3, StatisticsUtil.qBinom(0.7, 4, 0.5), DELTA);
        assertEquals(3, StatisticsUtil.qBinom(0.8, 4, 0.5), DELTA);
        assertEquals(3, StatisticsUtil.qBinom(0.9, 4, 0.5), DELTA);
        assertEquals(4, StatisticsUtil.qBinom(1.0, 4, 0.5), DELTA);

        assertEquals(0, StatisticsUtil.qBinom(0.0, 9, 0.5), DELTA);
        assertEquals(3, StatisticsUtil.qBinom(0.1, 9, 0.5), DELTA);
        assertEquals(3, StatisticsUtil.qBinom(0.2, 9, 0.5), DELTA);
        assertEquals(4, StatisticsUtil.qBinom(0.3, 9, 0.5), DELTA);
        assertEquals(4, StatisticsUtil.qBinom(0.4, 9, 0.5), DELTA);
        assertEquals(4, StatisticsUtil.qBinom(0.5, 9, 0.5), DELTA);
        assertEquals(5, StatisticsUtil.qBinom(0.6, 9, 0.5), DELTA);
        assertEquals(5, StatisticsUtil.qBinom(0.7, 9, 0.5), DELTA);
        assertEquals(6, StatisticsUtil.qBinom(0.8, 9, 0.5), DELTA);
        assertEquals(6, StatisticsUtil.qBinom(0.9, 9, 0.5), DELTA);
        assertEquals(9, StatisticsUtil.qBinom(1.0, 9, 0.5), DELTA);
    }

    @Test
    public void testMedian() {
        assertEquals(median(5.0, 1.0, 9.0), StatisticsUtil.median(createNumbers(9), 0.01));
        assertEquals(median(5.5, 1.0, 10.0), StatisticsUtil.median(createNumbers(10), 0.01));
        assertEquals(median(50.0, 37.0, 63.0), StatisticsUtil.median(createNumbers(99), 0.01));
        assertEquals(median(500.0, 459.0, 541.0), StatisticsUtil.median(createNumbers(999), 0.01));

        assertEquals(median(5.0, 2.0, 8.0), StatisticsUtil.median(createNumbers(9), 0.05));
        assertEquals(median(5.5, 2.0, 9.0), StatisticsUtil.median(createNumbers(10), 0.05));
        assertEquals(median(50.0, 40.0, 60.0), StatisticsUtil.median(createNumbers(99), 0.05));
        assertEquals(median(500.0, 469.0, 531.0), StatisticsUtil.median(createNumbers(999), 0.05));
    }
}
