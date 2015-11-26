package de.unihannover.se.processSimulation.dataGenerator;

import java.lang.reflect.InvocationTargetException;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngine;
import org.rosuda.REngine.REngineException;

public class StatisticsUtil {

    private static REngine rengine;

    static {
        try {
            rengine = REngine.engineForClass("org.rosuda.REngine.JRI.JRIEngine");
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                        | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static MedianWithConfidenceInterval median(double[] data, double p) {
        try {
            rengine.assign("x", data);
            final double median = rengine.parseAndEval("median(x)").asDouble();
            final double lower = p / 2.0;
            final double upper = 1.0 - lower;
            final REXP confIntv = rengine.parseAndEval("sort(x)[qbinom(c(" + lower + "," + upper + "), length(x), 0.5)]");
            return new MedianWithConfidenceInterval(median, confIntv.asDoubles());
        } catch (final REngineException | REXPMismatchException e) {
            throw new RuntimeException(e);
        }
    }

    public static void close() {
        rengine.close();
    }

}
