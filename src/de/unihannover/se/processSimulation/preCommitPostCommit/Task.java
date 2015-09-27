package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.unihannover.se.processSimulation.common.ReviewMode;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeSpan;

abstract class Task extends RealModelEntity implements MemoryItem {

    public enum State {
        OPEN,
        READY_FOR_REVIEW,
        REJECTED,
        DONE
    }

    private State state;
    private Developer implementor;

    private final List<Bug> lurkingBugs;
    private final List<TimeSpan> implementationInterruptions;
    private Review currentReview;
    private boolean commited;

    private final TimeSpan implementationTime;

    public Task(RealProcessingModel model, String name) {
        super(model, name);
        this.state = State.OPEN;
        this.lurkingBugs = new ArrayList<>();
        this.implementationInterruptions = new ArrayList<>();
        this.implementationTime = model.getParameters().getImplementationTimeDist().sampleTimeSpan(TimeUnit.HOURS);
    }

    public void performImplementation(Developer dev) {
        assert this.state == State.OPEN;
        assert this.implementor == null;
        assert this.implementationInterruptions.isEmpty();

        this.implementor = dev;

        this.handleTaskSwitchOverhead(dev);

        //der "Konfliktzeitraum" soll erst beginnen, nachdem die Task-Switch/Einarbeitungsphase durch ist
        this.getSourceRepository().startWork(this);
        this.implementor.hold(this.implementationTime);

        this.createBugs(this.implementationTime);
        this.endImplementation();
    }

    private void endImplementation() {
        this.handleAdditionalWaitsForInterruptions();

        if (this.getModel().getReviewMode() == ReviewMode.POST_COMMIT) {
            this.commit(this.implementor);
        }
        this.handleAdditionalWaitsForInterruptions();
        this.state = State.READY_FOR_REVIEW;
        this.getBoard().addTaskReadyForReview(this);
    }

    private void createBugs(TimeSpan relevantTime) {
        //TODO hier fehlt mir noch ein bisschen Zufall
        double bugsCreated = this.implementor.getImplementationSkill() * relevantTime.getTimeAsDouble(TimeUnit.HOURS);
        while (bugsCreated > 1) {
            this.lurkingBugs.add(new NormalBug(this));
            bugsCreated -= 1.0;
        }
        if (this.getModel().getRandomBool(bugsCreated)) {
            this.lurkingBugs.add(new NormalBug(this));
        }

        if (this.implementor.makesBlockerBug()) {
            this.lurkingBugs.add(new GlobalBug(this.getModel()));
        }
    }

    private void handleAdditionalWaitsForInterruptions() {
        while (!this.implementationInterruptions.isEmpty()) {
            final TimeSpan interruption = this.implementationInterruptions.remove(0);
            this.implementor.hold(interruption);
        }
    }

    public void suspendImplementation(TimeSpan timeSpan) {
        this.getModel().sendTraceNote("suspends implementation of " + this + " for " + timeSpan);
        this.implementationInterruptions.add(timeSpan);
    }

    public void performReview(Developer reviewer) {
        assert this.state == State.READY_FOR_REVIEW;
        assert this.implementor != null;
        assert this.implementor != reviewer;

        this.handleTaskSwitchOverhead(reviewer);

        reviewer.hold(this.getModel().getParameters().getReviewTimeDist().sampleTimeSpan(TimeUnit.HOURS));

        final List<Bug> foundBugs = new ArrayList<>();
        for (final Bug b : this.lurkingBugs) {
            if (reviewer.findsBug(b)) {
                foundBugs.add(b);
            }
        }
        reviewer.sendTraceNote("ends review of " + this + ", found " + foundBugs.size() + " of " + this.lurkingBugs.size() + " bugs");
        this.currentReview = new Review(foundBugs);
        if (foundBugs.isEmpty()) {
            this.endReviewWithoutRemarks(reviewer);
        } else {
            this.endReviewWithRemarks();
        }
    }

    private void endReviewWithRemarks() {
        assert this.currentReview != null;
        this.state = State.REJECTED;
        this.getBoard().addTaskWithReviewRemarks(this);
    }

    private void endReviewWithoutRemarks(Developer reviewer) {
        if (this.getModel().getReviewMode() == ReviewMode.PRE_COMMIT) {
            this.commit(reviewer);
        }
        this.state = State.DONE;
        this.getBoard().addFinishedTask(this);
    }

    public void performFixing(Developer dev) {
        assert this.state == State.REJECTED;
        assert this.implementor == dev;
        assert this.implementationInterruptions.isEmpty() : this + " contains interruptions " + this.implementationInterruptions;

        this.handleTaskSwitchOverhead(dev);

        if (this.getModel().getReviewMode() == ReviewMode.POST_COMMIT) {
            //im Post-Commit-Fall wurde schon commitet und es muss nochmal "ausgecheckt" werden
            this.getSourceRepository().startWork(this);
        }

        //An sich kann es neben dem Einbauen neuer Bugs auch vorkommen, dass bestehende und bereits angemerkte
        //  Bugs nicht wirklich gefixt werden. Das wird hier vernachlässigt.

        double hoursForFixing = 0.0;
        for (final Bug b : this.currentReview.getRemarks()) {
            hoursForFixing += this.getModel().getParameters().getFixTimeDist().sample();
        }
        final TimeSpan fixingTime = new TimeSpan(hoursForFixing, TimeUnit.HOURS);
        dev.hold(fixingTime);
        for (final Bug b : this.currentReview.getRemarks()) {
            this.lurkingBugs.remove(b);
            b.fix();
        }

        this.createBugs(fixingTime);
        this.endImplementation();
    }

    private void handleTaskSwitchOverhead(Developer dev) {
        final TimeSpan taskSwitchOverhead = this.determineTaskSwitchOverhead(dev);
        if (taskSwitchOverhead.getTimeInEpsilon() != 0) {
            this.sendTraceNote("has task switch overhead switching to " + this);
            dev.hold(taskSwitchOverhead);
        }
    }

    private TimeSpan determineTaskSwitchOverhead(Developer dev) {
        final TimeInstant lastTime = dev.getLastTimeYouHadToDoWith(this);
        final TimeSpan max = this.getModel().getParameters().getMaxTaskSwitchOverhead();
        if (lastTime == null) {
            return max;
        }

        if (max.getTimeInEpsilon() == 0) {
            return max;
        }

        final TimeSpan forOneHour = this.getModel().getParameters().getTaskSwitchOverheadAfterOneHourInterruption();
        final double s = - new TimeSpan(1, TimeUnit.HOURS).getTimeAsDouble() / Math.log(1.0 - forOneHour.getTimeAsDouble() / max.getTimeAsDouble());

        final double timeSinceLastTime = this.presentTime().getTimeAsDouble() - lastTime.getTimeAsDouble();
        final double overhead = max.getTimeAsDouble() * (1.0 - Math.exp(- timeSinceLastTime / s));
        return new TimeSpan(overhead);
    }

    public boolean wasImplementedBy(Developer developer) {
        return developer.equals(this.implementor);
    }

    public boolean isFinished() {
        return this.state == State.DONE;
    }

    private void commit(Developer commiter) {

        while (!this.getSourceRepository().tryCommit(this)) {
            //Konflikt vorhanden => Update ziehen, anpassen, und nochmal probieren
            this.getSourceRepository().restartWork(this);
            commiter.hold(this.getModel().getParameters().getConflictResolutionTimeDist().sampleTimeSpan(TimeUnit.HOURS));
        }

        this.commited = true;
        for (final Bug b : this.lurkingBugs) {
            b.startTicking();
        }
    }

    public boolean isCommited() {
        return this.commited;
    }

    public abstract List<? extends Task> getPrerequisites();

    public TimeSpan getImplementationTime() {
        return this.implementationTime;
    }

}
