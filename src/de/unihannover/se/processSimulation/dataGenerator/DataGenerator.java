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

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import de.unihannover.se.processSimulation.common.ParametersFactory;
import de.unihannover.se.processSimulation.common.ReviewMode;
import de.unihannover.se.processSimulation.preCommitPostCommit.RealProcessingModel;
import desmoj.core.dist.MersenneTwisterRandomGenerator;
import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.TimeInstant;

public class DataGenerator {

    public static ExperimentResult runExperiment(
                    final ParametersFactory p, ReviewMode mode, boolean report, String runId) {
        final RealProcessingModel model = new RealProcessingModel("RealProcessingModel", mode, p, report);
        final Experiment exp;
        if (report) {
            exp = new Experiment("Experiment" + mode + "_" + runId,
                        ".\\experimentResults", null, Experiment.DEFAULT_REPORT_OUTPUT_TYPE,
                        Experiment.DEFAULT_TRACE_OUTPUT_TYPE, Experiment.DEFAULT_ERROR_OUTPUT_TYPE,
                        Experiment.DEFAULT_DEBUG_OUTPUT_TYPE);
        } else {
            exp = new Experiment("Experiment" + mode + "_" + runId,
                        ".\\experimentResults", null, noOutputs(), noOutputs(), noOutputs(), noOutputs());
        }
        exp.setRandomNumberGenerator(MersenneTwisterRandomGenerator.class);
        exp.setSeedGenerator(p.getSeed());
        model.connectToExperiment(exp);

        final long expStartTime = System.currentTimeMillis();
        exp.setSilent(!report);
        exp.getOutputPath();
        exp.setShowProgressBar(false);
        if (report) {
            exp.tracePeriod(new TimeInstant(0), new TimeInstant(160, TimeUnit.HOURS));
        }
        final int relevantRunningHours = 8 * 600;
        exp.stop(new TimeInstant(RealProcessingModel.HOURS_TO_RESET + relevantRunningHours, TimeUnit.HOURS));
        exp.start();
        if (report) {
            exp.report();
        }
        exp.finish();
        final long expDuration = System.currentTimeMillis() - expStartTime;

        return new ExperimentResult(
                        model.getFinishedStoryPoints(),
                        model.getStoryCycleTimeMean(),
                        model.getStoryCycleTimeStdDev(),
                        model.getStartedStoryCount(),
                        model.getFinishedStoryCount(),
                        model.getBugCountFoundByCustomers(),
                        p.getNumberOfDevelopers() * relevantRunningHours,
                        expDuration);

    }

    private static ArrayList<String> noOutputs() {
        final ArrayList<String> noOutputs = new ArrayList<>();
        return noOutputs;
    }

}
