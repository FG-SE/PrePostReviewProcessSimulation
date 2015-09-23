package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.TimeSpan;

class Bug extends RealModelProcess {

    private boolean fixed;
    private final Task task;

    public Bug(Task task) {
        super(task.getModel(), "bug");
        this.task = task;
    }

    @Override
    public void lifeCycle() {
        if (this.fixed) {
            return;
        }

        //TODO Zeit bis zur Aktivierung
        this.hold(new TimeSpan(40, TimeUnit.HOURS));

        if (!this.fixed) {
            this.explode();
        }
    }

    private void explode() {
        //TODO Abarbeitung dauert länger, wenn auf dem Buggy-Task andere Dinge aufgebaut haben (propabilistisch)
        //TODO noch in Arbeit befindliche abhängige Tasks verzögern sich (propabilitisch)
        this.getBoard().addBugToBeFixed(new BugfixTask(this));
    }

    public void fix() {
        this.fixed = true;
    }

    public Task getTask() {
        return this.task;
    }

}
