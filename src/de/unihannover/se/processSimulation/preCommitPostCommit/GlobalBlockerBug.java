package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.TimeSpan;

class GlobalBlockerBug extends Bug {

    public GlobalBlockerBug(RealProcessingModel model) {
        super(model, "global-bug");
    }

    @Override
    protected TimeSpan getActivationTimeForDevelopers() {
        return new TimeSpan(0, TimeUnit.HOURS);
    }

    @Override
    protected TimeSpan getActivationTimeForCustomers() {
        return null;
    }

    @Override
    protected void becomeVisible() {
        for (final Task t : this.getBoard().getAllTasksInImplementation()) {
            t.suspendImplementation(this.getModel().getParameters().getGlobalBugSuspendTimeDist().sampleTimeSpan(TimeUnit.HOURS));
        }
    }

}
