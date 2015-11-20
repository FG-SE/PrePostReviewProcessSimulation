package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.TimeSpan;

class GlobalBlockerBug extends Bug {

    private final TimeSpan suspendTime;

    public GlobalBlockerBug(RealProcessingModel model, Randomness randomness) {
        super(model, "global-bug", randomness);
        this.suspendTime = randomness.sampleTimeSpan(this.getModel().getParameters().getGlobalBugSuspendTimeDist());
    }

    @Override
    protected TimeSpan getActivationTimeForDevelopers() {
        //TODO ggf. auch als Parameter?
        return new TimeSpan(1, TimeUnit.MINUTES);
    }

    @Override
    protected TimeSpan getActivationTimeForCustomers() {
        return null;
    }

    @Override
    protected void becomeVisible() {
        for (final Task t : this.getBoard().getAllTasksInImplementation()) {
            t.suspendImplementation(this.suspendTime);
        }
    }

}
