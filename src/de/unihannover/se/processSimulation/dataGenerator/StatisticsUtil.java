package de.unihannover.se.processSimulation.dataGenerator;

import java.util.Arrays;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.BinomialDistribution;
import org.apache.commons.math.distribution.BinomialDistributionImpl;

public class StatisticsUtil {

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
