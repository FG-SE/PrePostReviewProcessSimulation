package de.unihannover.se.processSimulation.postprocessing;

public class MathUtil {

    public static double determineMean(double[] values) {
        double sum = 0.0;
        for (final double d : values) {
            sum += d;
        }
        return sum / values.length;
    }

    public static double determineStdDev(double[] values) {
        if (values.length <= 1) {
            return 0.0;
        }
        final double mean = determineMean(values);
        double sumOfSquares = 0.0;
        for (final double value : values) {
            final double diff = value - mean;
            sumOfSquares += diff * diff;
        }
        return Math.sqrt(sumOfSquares / (values.length - 1));
    }

    public static double determineWeightedMean(double[] weights, double[] values) {
        assert weights.length == values.length;
        double sum = 0.0;
        double weightSum = 0.0;
        for (int i = 0; i < weights.length; i++) {
            sum += weights[i] * values[i];
            weightSum += weights[i];
        }
        return sum / weightSum;
    }

    public static double determinePooledStdDev(double[] sampleSizes, double[] stdDev) {
        assert sampleSizes.length == stdDev.length;
        double sumOfWeightedVariances = 0.0;
        double sumOfSampleSizes = 0.0;
        for (int i = 0; i < sampleSizes.length; i++) {
            if (sampleSizes[i] <= 1.0) {
                continue;
            }
            sumOfWeightedVariances += (sampleSizes[i] - 1) * stdDev[i] * stdDev[i];
            sumOfSampleSizes += sampleSizes[i];
        }
        return Math.sqrt(sumOfWeightedVariances / sumOfSampleSizes);
    }

}
