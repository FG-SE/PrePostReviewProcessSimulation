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

import java.util.Arrays;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.BinomialDistribution;
import org.apache.commons.math.distribution.BinomialDistributionImpl;

/**
 * Helper class with statistics functions.
 */
public class StatisticsUtil {

    /**
     * Computes the median and its confidence interval for the given data and p-value.
     * When there is enough data, the confidence interval is conservative. The caller
     * has to ensure that there is enough data (the minimum length depens only on p, not
     * on the data).
     */
    public static MedianWithConfidenceInterval median(double[] data, double p) {
        Arrays.sort(data);
        //When the array is really short, the intended p value can possibly not be reached. This is not checked here
        //  and has to be taken care of by the researcher.
        return new MedianWithConfidenceInterval(
                data.length % 2 == 0 ? ((data[data.length / 2 - 1] + data[data.length / 2]) / 2.0) : data[data.length / 2],
                data[qBinom(p / 2.0, data.length, 0.5) - 1],
                data[Math.min(qBinom(1.0 - p / 2.0, data.length, 0.5), data.length - 1)]);
    }

    static int qBinom(double q, int trials, double p) {
        try {
            final BinomialDistribution d = new BinomialDistributionImpl(trials, p);
            final int result = d.inverseCumulativeProbability(q);
            if (result == -1) {
                return 0;
            } else if (result == Integer.MAX_VALUE) {
                return trials;
            } else {
                return result + 1;
            }
        } catch (final MathException e) {
            throw new RuntimeException(e);
        }
    }

}
