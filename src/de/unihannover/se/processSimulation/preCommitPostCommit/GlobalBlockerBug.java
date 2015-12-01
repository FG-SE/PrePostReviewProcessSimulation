package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.TimeSpan;

class GlobalBlockerBug extends Bug {

    public GlobalBlockerBug(Task task) {
        super(task, "global-bug");
    }

    @Override
    protected TimeSpan getActivationTimeForDevelopers() {
        return new TimeSpan(1, TimeUnit.MINUTES);
    }

    @Override
    protected TimeSpan getActivationTimeForCustomers() {
        return null;
    }

    @Override
    protected void becomeVisible(boolean byCustomer) {
        assert !byCustomer;
        for (final Task t : this.getBoard().getAllTasksInImplementation()) {
            t.suspendImplementation(this.getModel().getParameters().getGlobalBugSuspendTimeDist().sampleTimeSpan(TimeUnit.HOURS));
        }
        this.fix();
    }

}
