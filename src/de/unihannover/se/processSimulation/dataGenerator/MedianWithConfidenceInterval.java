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

/**
 * Combines a median value and its confidence interval.
 * Provides formatting to a String in several forms.
 */
public class MedianWithConfidenceInterval {

    private static final double EPSILON = 0.00000001;

    private final double median;
    private final double lowerConfidence;
    private final double upperConfidence;

    MedianWithConfidenceInterval(double median, double lower, double upper) {
        this.median = median;
        this.lowerConfidence = lower;
        this.upperConfidence = upper;
        assert this.lowerConfidence <= this.median : "Invalid lower confidence " + median + " " + lower + " " + upper;
        assert this.median <= this.upperConfidence : "Invalid upper confidence " + median + " " + lower + " " + upper;
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
