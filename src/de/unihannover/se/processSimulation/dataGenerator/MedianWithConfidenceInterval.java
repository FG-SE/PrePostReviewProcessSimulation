package de.unihannover.se.processSimulation.dataGenerator;

import java.util.Arrays;

public class MedianWithConfidenceInterval {

    private static final double EPSILON = 0.00000001;

    private final double median;
    private final double lowerConfidence;
    private final double upperConfidence;

    MedianWithConfidenceInterval(double median, double[] confidence) {
        assert confidence.length == 2;
        this.median = median;
        this.lowerConfidence = confidence[0];
        this.upperConfidence = confidence[1];
        assert this.lowerConfidence <= this.median : "Invalid lower confidence " + median + " " + Arrays.toString(confidence);
        assert this.median <= this.upperConfidence : "Invalid upper confidence " + median + " " + Arrays.toString(confidence);
    }

    @Override
    public String toString() {
        return String.format("%f (%f .. %f)", this.median, this.lowerConfidence, this.upperConfidence);
    }

    public String toHtml() {
        return String.format("%.2f (%.2f&nbsp;..&nbsp;%.2f)", this.median, this.lowerConfidence, this.upperConfidence);
    }

    public String toHtmlPercent() {
        return String.format("%.2f%% (%.2f%%&nbsp;..&nbsp;%.2f%%)", this.median * 100, this.lowerConfidence * 100, this.upperConfidence * 100);
    }

    public double getMedian() {
        return this.median;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(this.median);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MedianWithConfidenceInterval)) {
            return false;
        }
        final MedianWithConfidenceInterval m = (MedianWithConfidenceInterval) o;
        return Math.abs(this.median - m.median) < EPSILON
            && Math.abs(this.lowerConfidence - m.lowerConfidence) < EPSILON
            && Math.abs(this.upperConfidence - m.upperConfidence) < EPSILON;
    }

    public boolean smallerThan(double d, boolean onlySignificant) {
        return (onlySignificant ? this.upperConfidence : this.median) <= d;
    }

    public boolean largerThan(double d, boolean onlySignificant) {
        return (onlySignificant ? this.lowerConfidence : this.median) >= d;
    }

    public double getLowerBound() {
        return this.lowerConfidence;
    }

    public double getUpperBound() {
        return this.upperConfidence;
    }

}
