package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.TimeSpan;

class NormalBug extends Bug {

    public enum BugType {
        DEVELOPER_ONLY,
        DEVELOPER_AND_CUSTOMER
    }

    private final Task task;
    private final BugType type;

    public NormalBug(Task task, BugType type) {
        super(task.getModel(), "bug");
        this.task = task;
        this.type = type;
    }

    @Override
    protected TimeSpan getActivationTimeForDevelopers() {
        return this.getModel().getParameters().getBugActivationTimeDeveloperDist().sampleTimeSpan(TimeUnit.HOURS);
    }

    @Override
    protected TimeSpan getActivationTimeForCustomers() {
        if (this.type == BugType.DEVELOPER_AND_CUSTOMER) {
            return this.getModel().getParameters().getBugActivationTimeCustomerDist().sampleTimeSpan(TimeUnit.HOURS);
        } else {
            return null;
        }
    }

    @Override
    protected void becomeVisible() {
        this.getBoard().addUnassessedBug(this);
    }

    public Task getTask() {
        return this.task;
    }

}
