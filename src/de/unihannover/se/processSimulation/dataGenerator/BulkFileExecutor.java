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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import co.paralleluniverse.common.util.Pair;
import de.unihannover.se.processSimulation.common.ReviewMode;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory.DistributionFactory;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory.ParameterType;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRun.ExperimentRunSummary;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRun.PrePostComparison;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRun.RealismCheckResult;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRun.ReviewNoReviewComparison;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRun.SingleRunCallback;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRunSettings.ExperimentRunParameters;
import de.unihannover.se.processSimulation.preCommitPostCommit.DependencyGraphConstellation;
import desmoj.core.simulator.CoroutineModel;
import desmoj.core.simulator.Experiment;

public class BulkFileExecutor {

    public static void main(String[] args) throws Exception {
        final List<ParameterType> paramNames = readParamNames(new File("sobolStuff/params.txt"));
        System.out.println("Read param names: " + paramNames);
        executeBulk(paramNames, new File("sobolStuff/sobolParameterSets.txt"), new File("sobolStuff/results.txt"), e -> {});
    }

    public static List<ParameterType> readParamNames(File filename) throws IOException {
        final List<ParameterType> ret = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = r.readLine()) != null) {
                ret.add(ParameterType.valueOf(line.split(" ")[0]));
            }
        }
        if (new HashSet<>(ret).size() != ret.size()) {
            throw new AssertionError("There are duplicate parameters!!");
        }
        return ret;
    }

    public static void executeBulk(List<ParameterType> paramNames, File inputFile, File outputFile, Consumer<Exception> exceptionCallback) throws Exception {
        Experiment.setCoroutineModel(CoroutineModel.FIBERS);

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
                        exceptionCallback.accept(e);
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
            if (value.matches("[+\\-0-9.]+")) {
                final int idx = (int) Double.parseDouble(value);
                if (idx == 0) {
                    return DependencyGraphConstellation.NO_DEPENDENCIES;
                } else if (idx == 1) {
                    return DependencyGraphConstellation.REALISTIC;
                } else if (idx == 2) {
                    return DependencyGraphConstellation.DIAMONDS;
                } else if (idx == 3) {
                    return DependencyGraphConstellation.CHAINS;
                } else {
                    throw new RuntimeException("Invalid value " + idx);
                }
            } else {
                return DependencyGraphConstellation.valueOf(value);
            }
        } else if (param.getType().equals(DistributionFactory.class)) {
            if (value.matches("[+\\-0-9.]+")) {
                final int idx = (int) Double.parseDouble(value);
                if (idx == 0) {
                    return DistributionFactory.POSNORMAL;
                } else if (idx == 1) {
                    return DistributionFactory.LOGNORMAL;
                } else if (idx == 2) {
                    return DistributionFactory.EXPSHIFT;
                } else {
                    throw new RuntimeException("Invalid value " + idx);
                }
            } else {
                return DistributionFactory.valueOf(value);
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
                        .copyWithChangedParam(ExperimentRunParameters.MAX_RUNS, 2000.0);
        return ExperimentRun.perform(runSettings, DataGenerator::runExperiment, parameters, new SingleRunCallback() {
            int runCount = 1;
            @Override
            public void handleResult(ExperimentResult no, ExperimentResult pre, ExperimentResult post) {
                System.out.println("Executing run " + this.runCount + " for input line " + lineNumber + " ... " + new Date());
                System.out.println("story points " + (no == null ? "X" : no.getFinishedStoryPoints())
                                + ", " + pre.getFinishedStoryPoints()
                                + ", " + post.getFinishedStoryPoints());
                this.runCount++;
            }
        });
    }

    private static void writeResult(ExperimentRun result, Writer output) throws IOException {
        final ExperimentRunSummary summary = result.getSignificantSummary();

        write(output, result.getFactorStoryPoints());
        output.write(';');
        write(output, result.getFinishedStoryPointsMedian(ReviewMode.NO_REVIEW));
        output.write(';');
        write(output, result.getFinishedStoryPointsMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getFinishedStoryPointsMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        output.write(summary.getStoryPointsResult().toString());
        output.write(';');
        output.write(Integer.toString(result.getCountFinishedStoryPointsPreLarger()));
        output.write(';');
        write(output, result.getFactorIssues());
        output.write(';');
        write(output, result.getIssueCountMedian(ReviewMode.NO_REVIEW));
        output.write(';');
        write(output, result.getIssueCountMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getIssueCountMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getIssueCountPerStoryPointMedian(ReviewMode.NO_REVIEW));
        output.write(';');
        write(output, result.getIssueCountPerStoryPointMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getIssueCountPerStoryPointMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        output.write(summary.getIssuesResult().toString());
        output.write(';');
        output.write(Integer.toString(result.getCountIssueCountPerStoryPointPreLarger()));
        output.write(';');
        write(output, result.getFactorCycleTime());
        output.write(';');
        write(output, result.getStoryCycleTimeMeanMedian(ReviewMode.NO_REVIEW));
        output.write(';');
        write(output, result.getStoryCycleTimeMeanMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getStoryCycleTimeMeanMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        output.write(summary.getCycleTimeResult().toString());
        output.write(';');
        output.write(Integer.toString(result.getCountCycleTimePreLarger()));
        output.write(';');
        write(output, result.getShareProductiveWork());
        output.write(';');
        output.write(summary.getRealismCheckResult().toString());
        output.write(';');
        write(output, result.getFactorNoReview());
        output.write(';');
        output.write(summary.getNoReviewResult().toString());
        output.write(';');
        write(output, result.getAvgImplementationTimeMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getAvgImplementationTimeMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getAvgReviewTimeMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getAvgReviewTimeMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getAvgRemarkFixingTimeMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getAvgRemarkFixingTimeMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getAvgIssueFixingTimeMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getAvgIssueFixingTimeMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getAvgIssueAssessmentTimeMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getAvgIssueAssessmentTimeMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getAvgPlanningTimeMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getAvgPlanningTimeMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getTotalImplementationTimeMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getTotalImplementationTimeMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getTotalReviewTimeMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getTotalReviewTimeMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getTotalRemarkFixingTimeMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getTotalRemarkFixingTimeMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getTotalIssueFixingTimeMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getTotalIssueFixingTimeMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getTotalIssueAssessmentTimeMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getTotalIssueAssessmentTimeMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getTotalPlanningTimeMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getTotalPlanningTimeMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getAvgReviewRoundCountMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getAvgReviewRoundCountMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getAvgTimePostToPreMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getAvgTimePostToPreMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getAvgTimePreToCustMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getAvgTimePreToCustMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getAvgIssuesInjectedPerReviewRemarkMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getAvgIssuesInjectedPerReviewRemarkMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getAvgIssuesInjectedPerIssueTaskMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getAvgIssuesInjectedPerIssueTaskMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getAvgIssuesInjectedPerImplementationTaskMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getAvgIssuesInjectedPerImplementationTaskMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getWastedTimeTaskSwitchMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getWastedTimeTaskSwitchMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getConflictCountMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getConflictCountMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        write(output, result.getGlobalIssueCountMedian(ReviewMode.PRE_COMMIT));
        output.write(';');
        write(output, result.getGlobalIssueCountMedian(ReviewMode.POST_COMMIT));
        output.write(';');
        output.write(Integer.toString(result.getNumberOfTrials()));
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
        ret.add(new Pair<>("summaryStoryPoints", PrePostComparison.class));
        ret.add(new Pair<>("storyPointsPreLargerCount", Integer.class));
        addMedianAttributes(ret, "FactorIssues");
        addMedianAttributes(ret, "IssueCountMedian_NO_REVIEW");
        addMedianAttributes(ret, "IssueCountMedian_PRE_COMMIT");
        addMedianAttributes(ret, "IssueCountMedian_POST_COMMIT");
        addMedianAttributes(ret, "IssueCountPerStoryPointMedian_NO_REVIEW");
        addMedianAttributes(ret, "IssueCountPerStoryPointMedian_PRE_COMMIT");
        addMedianAttributes(ret, "IssueCountPerStoryPointMedian_POST_COMMIT");
        ret.add(new Pair<>("summaryIssues", PrePostComparison.class));
        ret.add(new Pair<>("issuesPreLargerCount", Integer.class));
        addMedianAttributes(ret, "FactorCycleTime");
        addMedianAttributes(ret, "StoryCycleTimeMeanMedian_NO_REVIEW");
        addMedianAttributes(ret, "StoryCycleTimeMeanMedian_PRE_COMMIT");
        addMedianAttributes(ret, "StoryCycleTimeMeanMedian_POST_COMMIT");
        ret.add(new Pair<>("summaryCycleTime", PrePostComparison.class));
        ret.add(new Pair<>("cycleTimePreLargerCount", Integer.class));
        addMedianAttributes(ret, "ShareProductiveWork");
        ret.add(new Pair<>("summaryProductiveWork", RealismCheckResult.class));
        addMedianAttributes(ret, "FactorNoReview");
        ret.add(new Pair<>("summaryNoReview", ReviewNoReviewComparison.class));
        addMedianAttributes(ret, "avgImplementationTime_PRE_COMMIT");
        addMedianAttributes(ret, "avgImplementationTime_POST_COMMIT");
        addMedianAttributes(ret, "avgReviewTime_PRE_COMMIT");
        addMedianAttributes(ret, "avgReviewTime_POST_COMMIT");
        addMedianAttributes(ret, "avgRemarkFixingTime_PRE_COMMIT");
        addMedianAttributes(ret, "avgRemarkFixingTime_POST_COMMIT");
        addMedianAttributes(ret, "avgIssueFixingTime_PRE_COMMIT");
        addMedianAttributes(ret, "avgIssueFixingTime_POST_COMMIT");
        addMedianAttributes(ret, "avgIssueAssessmentTime_PRE_COMMIT");
        addMedianAttributes(ret, "avgIssueAssessmentTime_POST_COMMIT");
        addMedianAttributes(ret, "avgPlanningTime_PRE_COMMIT");
        addMedianAttributes(ret, "avgPlanningTime_POST_COMMIT");
        addMedianAttributes(ret, "totalImplementationTime_PRE_COMMIT");
        addMedianAttributes(ret, "totalImplementationTime_POST_COMMIT");
        addMedianAttributes(ret, "totalReviewTime_PRE_COMMIT");
        addMedianAttributes(ret, "totalReviewTime_POST_COMMIT");
        addMedianAttributes(ret, "totalRemarkFixingTime_PRE_COMMIT");
        addMedianAttributes(ret, "totalRemarkFixingTime_POST_COMMIT");
        addMedianAttributes(ret, "totalIssueFixingTime_PRE_COMMIT");
        addMedianAttributes(ret, "totalIssueFixingTime_POST_COMMIT");
        addMedianAttributes(ret, "totalIssueAssessmentTime_PRE_COMMIT");
        addMedianAttributes(ret, "totalIssueAssessmentTime_POST_COMMIT");
        addMedianAttributes(ret, "totalPlanningTime_PRE_COMMIT");
        addMedianAttributes(ret, "totalPlanningTime_POST_COMMIT");
        addMedianAttributes(ret, "avgReviewRoundCount_PRE_COMMIT");
        addMedianAttributes(ret, "avgReviewRoundCount_POST_COMMIT");
        addMedianAttributes(ret, "avgTimePostToPre_PRE_COMMIT");
        addMedianAttributes(ret, "avgTimePostToPre_POST_COMMIT");
        addMedianAttributes(ret, "avgTimePreToCust_PRE_COMMIT");
        addMedianAttributes(ret, "avgTimePreToCust_POST_COMMIT");
        addMedianAttributes(ret, "avgIssuesInjectedPerReviewRemark_PRE_COMMIT");
        addMedianAttributes(ret, "avgIssuesInjectedPerReviewRemark_POST_COMMIT");
        addMedianAttributes(ret, "avgIssuesInjectedPerIssueTask_PRE_COMMIT");
        addMedianAttributes(ret, "avgIssuesInjectedPerIssueTask_POST_COMMIT");
        addMedianAttributes(ret, "avgIssuesInjectedPerImplementationTask_PRE_COMMIT");
        addMedianAttributes(ret, "avgIssuesInjectedPerImplementationTask_POST_COMMIT");
        addMedianAttributes(ret, "wastedTimeTaskSwitch_PRE_COMMIT");
        addMedianAttributes(ret, "wastedTimeTaskSwitch_POST_COMMIT");
        addMedianAttributes(ret, "conflictCount_PRE_COMMIT");
        addMedianAttributes(ret, "conflictCount_POST_COMMIT");
        addMedianAttributes(ret, "globalIssueCount_PRE_COMMIT");
        addMedianAttributes(ret, "globalIssueCount_POST_COMMIT");
        ret.add(new Pair<>("numberOfTrials", Integer.class));
        return ret;
    }

    private static void addMedianAttributes(List<Pair<String, Class<?>>> ret, String name) {
        ret.add(new Pair<>("med_" + name, Double.class));
        ret.add(new Pair<>("low_" + name, Double.class));
        ret.add(new Pair<>("upp_" + name, Double.class));
    }

}
