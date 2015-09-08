package de.tntinteractive.processSimulation.preCommitPostCommit;

import java.util.List;

import desmoj.core.simulator.TimeSpan;

public class Bug extends DevelopmentSimProcess {

    private boolean fixed;
    private final TimeSpan timeBeforeSurfacing;

    public Bug(DevelopmentProcessModel owner) {
        super(owner, "Bug");
        //TODO: Sofort-Störer-Bugs
        this.timeBeforeSurfacing = owner.getParameters().getBugSurfacingTime();
    }

    public void startTicking() {
        this.activate();
    }

    public void setFixed() {
        this.fixed = true;
    }

    @Override
    public void lifeCycle() {
        this.hold(this.timeBeforeSurfacing);
        if (!this.fixed) {
            this.surface();
        }
    }

    private void surface() {
        //TODO Folgeaufwände etc
        //TODO wenn Task mit dem Bug noch offen ist keinen Folgebug erstellen
        //TODO beachten, wenn Bug zwar schon im Review gefunden wurde, aber noch nicht gefixt

        this.getBoard().addBugfixTask(this);
    }

    public static void startTickingForAll(List<Bug> lurkingBugs) {
        for (final Bug b : lurkingBugs) {
            b.startTicking();
        }
    }

}
