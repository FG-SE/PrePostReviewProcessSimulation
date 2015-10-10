package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.TimeSpan;

class NormalBug extends Bug {

    private final Task task;

    public NormalBug(Task task) {
        super(task.getModel(), "bug");
        this.task = task;
    }

    @Override
    protected TimeSpan getActivationTime() {
        return this.getModel().getParameters().getBugActivationTimeDist().sampleTimeSpan(TimeUnit.HOURS);
    }

    @Override
    protected void becomeVisible() {
        this.getBoard().addUnassessedBug(this);
    }

    public Task getTask() {
        return this.task;
    }

}
