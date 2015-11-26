package de.unihannover.se.processSimulation.dataGenerator;

import java.util.Arrays;

import de.unihannover.se.processSimulation.common.ReviewMode;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory.ParameterType;

public class VarianceCheck {

    private static final int LEN = 100;

    public static void main(String[] args) {
        final long[] resultsPost = new long[LEN];
        final long[] resultsPre = new long[LEN];
        BulkParameterFactory f = BulkParameterFactory.forCommercial();
        f = f.copyWithChangedParam(ParameterType.IMPLEMENTATION_SKILL_MODE, 0.0);
        for (int i = 1; i <= LEN; i++) {
            final ExperimentResult resultPost = DataGenerator.runExperiment(f, ReviewMode.POST_COMMIT, false, "post_vc" + f.getSeed());
            final ExperimentResult resultPre = DataGenerator.runExperiment(f, ReviewMode.PRE_COMMIT, false, "pre_vc" + f.getSeed());
            final ExperimentResult resultNo = DataGenerator.runExperiment(f, ReviewMode.NO_REVIEW, false, "no_vc" + f.getSeed());
            final CombinedResult combined = new CombinedResult(resultPost, resultPre);

            System.out.print(resultPost.getFinishedStoryPoints());
            System.out.print('\t');
            System.out.print(resultPre.getFinishedStoryPoints());
            System.out.print('\t');
            System.out.print(combined.getFinishedStoryPointDiff());
            System.out.print('\t');
            System.out.print(resultNo.getFinishedStoryPoints());
            System.out.print('\t');
            System.out.println(i);
            resultsPost[i - 1] = resultPost.getFinishedStoryPoints();
            resultsPre[i - 1] = resultPre.getFinishedStoryPoints();

            f = f.copyWithChangedSeed();
        }
        System.out.println();
        final long[] diffs = calculateDifferences(resultsPost, resultsPre);
        System.out.println("Mean=" + calculateMean(diffs));
        System.out.println("Median=" + calculateMedian(diffs));
        final long[] allDiffs = calculateAllDiffs(resultsPost, resultsPre);
        System.out.println("Hodges-Lehmann diff=" + calculateMedian(allDiffs));
//        System.out.println("Conf intv=" + Arrays.toString(calculateConfidenceInterval(diffs)));
        System.out.println("Moses intv=" + Arrays.toString(calculateMosesInterval(allDiffs)));
    }

    private static long[] calculateMosesInterval(long[] allDiffs) {
        assert allDiffs.length == LEN * LEN;
        assert LEN > 30;
        final double alpha = 0.05;
        //TODO keine Ahnung ob der Wert stimmt
        final double f = 1.5;
        final double z = 0.5 * Math.log(f);
        final double c = (LEN * LEN / 2.0) - z * Math.sqrt(LEN * LEN * (LEN + LEN + 1) / 12);

        Arrays.sort(allDiffs);
        final int lower = (int) (Math.round(c) - 1);
        final int upper = (int) (LEN * LEN - Math.round(c));
        return new long[] {allDiffs[lower], allDiffs[upper]};
    }

//    private static long[] calculateConfidenceInterval(long[] diffs) {
//        final TDistribution tDist = new TDistributionImpl(statistics.getN() - 1);
//        final double a = tDist.inverseCumulativeProbability(1.0 - significance / 2);
//        double marginOfError = a * statistics.getStandardDeviation() / Math.sqrt(statistics.getN());
//    }

    private static long[] calculateAllDiffs(long[] resultsPost, long[] resultsPre) {
        final long[] ret = new long[resultsPost.length * resultsPre.length];
        int i = 0;
        for (final long resultPost : resultsPost) {
            for (final long resultPre : resultsPre) {
                ret[i++] = resultPost - resultPre;
            }
        }
        return ret;
    }

    private static long[] calculateDifferences(long[] resultsPost, long[] resultsPre) {
        final long[] ret = new long[resultsPost.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = resultsPost[i] - resultsPre[i];
        }
        return ret;
    }

    private static double calculateMean(long[] diffs) {
        double sum = 0.0;
        for (final long l : diffs) {
            sum += l;
        }
        return sum / diffs.length;
    }

    private static long calculateMedian(long[] diffs) {
        Arrays.sort(diffs);
        final int middle = diffs.length / 2;
        if (diffs.length % 2 == 0) {
            return (diffs[middle - 1] + diffs[middle]) / 2;
        } else {
            return diffs[middle];
        }
    }

}
