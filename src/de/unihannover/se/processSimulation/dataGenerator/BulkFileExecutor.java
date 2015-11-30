package de.unihannover.se.processSimulation.dataGenerator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import co.paralleluniverse.common.util.Pair;
import de.unihannover.se.processSimulation.common.ReviewMode;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory.ParameterType;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRun.ExperimentRunSummary;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRun.SingleRunCallback;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRunSettings.ExperimentRunParameters;
import de.unihannover.se.processSimulation.preCommitPostCommit.DependencyGraphConstellation;

public class BulkFileExecutor {

    public static void main(String[] args) throws Exception {
        try {
            final List<ParameterType> paramNames = readParamNames("sobolStuff/params.txt");
            System.out.println("Read param names: " + paramNames);
            executeBulk(paramNames, "sobolStuff/sobolParameterSets.txt", "sobolStuff/results.txt");
        } finally {
            StatisticsUtil.close();
        }
    }

    public static List<ParameterType> readParamNames(String filename) throws IOException {
        final List<ParameterType> ret = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = r.readLine()) != null) {
                ret.add(ParameterType.valueOf(line.split(" ")[0]));
            }
        }
        return ret;
    }

    private static void executeBulk(List<ParameterType> paramNames, String inputFile, String outputFile) throws Exception {
        try (BufferedReader r = new BufferedReader(new FileReader(inputFile))) {
            try (Writer output = new FileWriter(outputFile)) {
                String line;
                int inputLineNbr = 1;
                while ((line = r.readLine()) != null) {
                    try {
                        final BulkParameterFactory parameters = parseParametersFromLine(paramNames, line);
                        final ExperimentRun result = executeSingle(parameters, inputLineNbr);
                        writeResult(result, output);
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                    inputLineNbr++;
                }
            }
        }
    }

    private static BulkParameterFactory parseParametersFromLine(List<ParameterType> paramNames, String line) {
        final String[] values = line.split(" ");
        assert paramNames.size() == values.length;
        BulkParameterFactory f = BulkParameterFactory.forCommercial();
        for (int i = 0; i < paramNames.size(); i++) {
            final ParameterType param = paramNames.get(i);
            final Object value = parseParameterValue(values[i], param);
            f = f.copyWithChangedParam(param, value);
        }
        return f;
    }

    public static Object parseParameterValue(final String value, final ParameterType param) {
        if (param.getType().equals(DependencyGraphConstellation.class)) {
            final int idx = (int) Double.parseDouble(value);
            if (idx == 0) {
                return DependencyGraphConstellation.NO_DEPENDENCIES;
            } else if (idx == 1) {
                return DependencyGraphConstellation.REALISTIC;
            } else if (idx == 2) {
                return DependencyGraphConstellation.CHAINS;
            } else {
                throw new RuntimeException("Invalid value " + idx);
            }
        } else if (param.getType().equals(Integer.class)) {
            return (int) Double.parseDouble(value);
        } else if (param.getType().equals(Double.class)) {
            return Double.parseDouble(value);
        } else {
            throw new RuntimeException("Invalid type " + param.getType());
        }
    }

    private static ExperimentRun executeSingle(BulkParameterFactory parameters, int lineNumber) {
        final ExperimentRunSettings runSettings = ExperimentRunSettings.defaultSettings()
                        .copyWithChangedParam(ExperimentRunParameters.MIN_RUNS, 20.0)
                        .copyWithChangedParam(ExperimentRunParameters.MAX_RUNS, 1000.0);
        return ExperimentRun.perform(runSettings, DataGenerator::runExperiment, parameters, new SingleRunCallback() {
            int runCount = 1;
            @Override
            public void handleResult(ExperimentResult no, ExperimentResult pre, ExperimentResult post) {
                System.out.println("Executing run " + this.runCount + " for input line " + lineNumber + " ...");
                System.out.println("story points " + (no == null ? "X" : no.getFinishedStoryPoints())
                                + ", " + pre.getFinishedStoryPoints()
                                + ", " + post.getFinishedStoryPoints());
                this.runCount++;
            }
        });
    }

    private static void writeResult(ExperimentRun result, Writer output) throws IOException {
        write(output, result.getFactorStoryPoints());
        output.write(';');
        write(output, result.getFinishedStoryPointsMedian(ReviewMode.NO_REVIEW));
        output.write(';');
        write(output, result.getFinishedStoryPointsMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getFinishedStoryPointsMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getFactorBugs());
        output.write(';');
        write(output, result.getBugCountMedian(ReviewMode.NO_REVIEW));
        output.write(';');
        write(output, result.getBugCountMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getBugCountMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getFactorCycleTime());
        output.write(';');
        write(output, result.getStoryCycleTimeMeanMedian(ReviewMode.NO_REVIEW));
        output.write(';');
        write(output, result.getStoryCycleTimeMeanMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getStoryCycleTimeMeanMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getShareProductiveWork());
        output.write(';');
        write(output, result.getFactorNoReview());
        output.write(';');
        output.write(result.getSummary().toString());
        output.write(';');
        output.write(Boolean.toString(result.isSummaryStatisticallySignificant()));
        output.write('\n');
        output.flush();
    }

    private static void write(Writer output, MedianWithConfidenceInterval confidenceInterval) throws IOException {
        output.write(Double.toString(confidenceInterval.getMedian()));
        output.write(';');
        output.write(Double.toString(confidenceInterval.getLowerBound()));
        output.write(';');
        output.write(Double.toString(confidenceInterval.getUpperBound()));
    }

    public static List<Pair<String, Class<?>>> getResultAttributes() {
        final List<Pair<String, Class<?>>> ret = new ArrayList<>();
        addMedianAttributes(ret, "FactorStoryPoints");
        addMedianAttributes(ret, "FinishedStoryPointsMedian_NO_REVIEW");
        addMedianAttributes(ret, "FinishedStoryPointsMedian_PRE_COMMIT");
        addMedianAttributes(ret, "FinishedStoryPointsMedian_POST_COMMIT");
        addMedianAttributes(ret, "FactorBugs");
        addMedianAttributes(ret, "RemainingBugCountMedian_NO_REVIEW");
        addMedianAttributes(ret, "RemainingBugCountMedian_PRE_COMMIT");
        addMedianAttributes(ret, "RemainingBugCountMedian_POST_COMMIT");
        addMedianAttributes(ret, "FactorCycleTime");
        addMedianAttributes(ret, "StoryCycleTimeMeanMedian_NO_REVIEW");
        addMedianAttributes(ret, "StoryCycleTimeMeanMedian_PRE_COMMIT");
        addMedianAttributes(ret, "StoryCycleTimeMeanMedian_POST_COMMIT");
        addMedianAttributes(ret, "ShareProductiveWork");
        addMedianAttributes(ret, "FactorNoReview");
        ret.add(new Pair<>("summary", ExperimentRunSummary.class));
        ret.add(new Pair<>("isSummaryStatisticallySignificant", Boolean.class));
        return ret;
    }

    private static void addMedianAttributes(List<Pair<String, Class<?>>> ret, String name) {
        ret.add(new Pair<>("med_" + name, Double.class));
        ret.add(new Pair<>("low_" + name, Double.class));
        ret.add(new Pair<>("upp_" + name, Double.class));
    }

}
