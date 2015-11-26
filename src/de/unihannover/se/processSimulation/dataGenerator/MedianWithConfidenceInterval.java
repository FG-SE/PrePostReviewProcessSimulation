package de.unihannover.se.processSimulation.dataGenerator;

public class MedianWithConfidenceInterval {

    private final double median;
    private final double lowerConfidence;
    private final double upperConfidence;

    MedianWithConfidenceInterval(double median, double[] confidence) {
        assert confidence.length == 2;
        this.median = median;
        this.lowerConfidence = confidence[0];
        this.upperConfidence = confidence[1];
        assert this.lowerConfidence <= this.median;
        assert this.median <= this.upperConfidence;
    }

    @Override
    public String toString() {
        return this.median + " (" + this.lowerConfidence + " .. " + this.upperConfidence + ")";
    }

}
