package de.tntinteractive.processSimulation.preCommitPostCommit;

import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.TimeSpan;

public class Parameters {

    public TimeSpan getPlanningTime() {
        // TODO Auto-generated method stub
        return new TimeSpan(2, TimeUnit.HOURS);
    }

    public TimeSpan getImplementationTime() {
        // TODO Auto-generated method stub
        return new TimeSpan(8, TimeUnit.HOURS);
    }

    public TimeSpan getBugfixTime() {
        // TODO Auto-generated method stub
        return new TimeSpan(4, TimeUnit.HOURS);
    }

    public TimeSpan getReviewTime() {
        // TODO Auto-generated method stub
        return new TimeSpan(1, TimeUnit.HOURS);
    }

    public TimeSpan getRemarkFixTime(int remarkCount) {
        // TODO Auto-generated method stub
        return new TimeSpan(15 * remarkCount, TimeUnit.MINUTES);
    }

    public TimeSpan getBugSurfacingTime() {
        // TODO Auto-generated method stub
        return new TimeSpan(24, TimeUnit.HOURS);
    }

}
