package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.TimeInstant;

public class Main {

    public static void main(String[] args) {

        final RealProcessingModel model = new RealProcessingModel("RealProcessingModel", ReviewMode.PRE_COMMIT);
        final Experiment exp = new Experiment("DevelopmentProcessModelTestExperiment",
                        TimeUnit.MINUTES, TimeUnit.HOURS, null);
        model.connectToExperiment(exp);

        // set experiment parameters
        exp.setShowProgressBar(false);
        exp.stop(new TimeInstant(8 * 400, TimeUnit.HOURS));
        exp.tracePeriod(new TimeInstant(0), new TimeInstant(80, TimeUnit.HOURS));
//        exp.debugPeriod(new TimeInstant(0), new TimeInstant(24, TimeUnit.HOURS));

        exp.start();
        exp.report();
        exp.finish();
    }
}
