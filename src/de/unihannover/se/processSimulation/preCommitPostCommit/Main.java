package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.concurrent.TimeUnit;

import de.unihannover.se.processSimulation.common.Parameters;
import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.TimeInstant;

public class Main {

    public static void main(String[] args) {

        final Parameters p = new Parameters();
        runExperiment(p, ReviewMode.POST_COMMIT);
        runExperiment(p, ReviewMode.PRE_COMMIT);
    }

    private static void runExperiment(final Parameters p, ReviewMode mode) {
        final RealProcessingModel model = new RealProcessingModel("RealProcessingModel", mode, p);
        final Experiment exp = new Experiment("DevelopmentProcessModelTestExperiment" + mode,
                        TimeUnit.MINUTES, TimeUnit.HOURS, null);
        model.connectToExperiment(exp);

        exp.setShowProgressBar(false);
        exp.stop(new TimeInstant(8 * 400, TimeUnit.HOURS));
        exp.tracePeriod(new TimeInstant(0), new TimeInstant(80, TimeUnit.HOURS));

        exp.start();
        exp.report();
        exp.finish();
    }
}
