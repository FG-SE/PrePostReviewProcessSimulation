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
    protected void explode() {
        //TODO Abarbeitung dauert länger, wenn auf dem Buggy-Task andere Dinge aufgebaut haben (propabilistisch)
        //TODO noch in Arbeit befindliche abhängige Tasks verzögern sich (propabilitisch)
        //z.B.: Bug hat Auswirkungen auf Nachfolger-Task:
        //wenn der Nachfolger bereits vollständig abgeschlossen ist, verlängert sich der Bugfix-Task
        //wenn der Nachfolger gerade in der Implementierung ist, verlängert sich die Implementierung
        //wenn der Nachfolger gerade vor der Implementierung ist, passiert nichts
        //wenn der Nachfolger schon
        this.getBoard().addBugToBeFixed(new BugfixTask(this));
    }

    public Task getTask() {
        return this.task;
    }

}
