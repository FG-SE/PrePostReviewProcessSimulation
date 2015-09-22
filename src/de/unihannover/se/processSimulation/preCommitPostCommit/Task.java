package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.TimeSpan;

public class Task extends RealModelProcess {

    public Task(RealProcessingModel owner) {
        super(owner, "task");
    }

    private Developer implementor;

    public void startImplementation(Developer dev) {
        this.implementor = dev;
        this.activate();
    }

    @Override
    public void lifeCycle() {
        //Implementierung
        this.hold(new TimeSpan(4, TimeUnit.HOURS));
        this.passivate();

        // TODO Review

        //Einpflegen der Review-Anmerkungen
        this.hold(new TimeSpan(2, TimeUnit.HOURS));
        this.implementor.activate();

    }

    public void fixReviewRemarks() {
        this.activate();
    }

    public boolean wasImplementedBy(Developer developer) {
        return developer.equals(this.implementor);
    }

}
