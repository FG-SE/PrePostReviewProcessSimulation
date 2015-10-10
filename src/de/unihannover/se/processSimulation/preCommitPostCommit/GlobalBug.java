package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.TimeSpan;

class GlobalBug extends Bug {

    public GlobalBug(RealProcessingModel model) {
        super(model, "global-bug");
    }

    @Override
    protected TimeSpan getActivationTime() {
        return new TimeSpan(0, TimeUnit.HOURS);
    }

    @Override
    protected void becomeVisible() {
        for (final Task t : this.getBoard().getAllTasksInImplementation()) {
            t.suspendImplementation(this.getModel().getParameters().getGlobalBugSuspendTimeDist().sampleTimeSpan(TimeUnit.HOURS));
        }
    }

}
