package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.unihannover.se.processSimulation.common.ReviewMode;
import de.unihannover.se.processSimulation.preCommitPostCommit.NormalBug.BugType;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeSpan;

abstract class Task extends RealModelEntity implements MemoryItem {

    public enum State {
        OPEN,
        IN_IMPLEMENTATION,
        READY_FOR_REVIEW,
        IN_REVIEW,
        REJECTED,
        DONE
    }

    private State state;
    private Developer implementor;

    private final List<Bug> lurkingBugs;
    private final List<Bug> bugsFixedInCommit;
    private final List<TimeSpan> implementationInterruptions;
    private Review currentReview;
    private List<NormalBug> bugsFoundByOthersDuringReview;
    private boolean commited;

    private final TimeSpan implementationTime;

    public Task(RealProcessingModel model, String name, TimeSpan implementationTime) {
        super(model, name);
        this.state = State.OPEN;
        this.lurkingBugs = new ArrayList<>();
        this.bugsFixedInCommit = new ArrayList<>();
        this.implementationInterruptions = new ArrayList<>();
        this.implementationTime = implementationTime;
    }

    public void performImplementation(Developer dev) {
        assert this.state == State.OPEN;
        assert this.implementor == null;
        assert this.implementationInterruptions.isEmpty();

        this.setState(State.IN_IMPLEMENTATION);
        this.implementor = dev;

        //mit Jens abgestimmt: erst update, dann Task-Switch-Overhead
        this.getSourceRepository().startWork(this);
        this.handleTaskSwitchOverhead(dev);
        this.implementor.hold(this.implementationTime);

        this.createBugs(this.implementationTime);
        this.endImplementation();
    }

    private void endImplementation() {
        this.handleAdditionalWaitsForInterruptions();

        if (this.getModel().getReviewMode() == ReviewMode.POST_COMMIT) {
            this.commit(this.implementor);
        }
        assert this.implementationInterruptions.isEmpty();
        this.setState(State.READY_FOR_REVIEW);
        this.getBoard().addTaskReadyForReview(this);
    }

    private void createBugs(TimeSpan relevantTime) {
        //TODO hier fehlt mir noch ein bisschen Zufall
        //TODO Bug-Typ zufällig erzeugen
        //TODO   dabei beachten: Bugs die nicht vom Entwickler erkannt werden können hängen nicht vom Entwicklerskill ab
        //TODO Bug-Tasks haben eine geringere Wahrscheinlichkeit, Kundenrelevante Bugs einzubauen (und insb. "nur Kunde"-Bugs)
        double bugsCreated = this.implementor.getImplementationSkill() * relevantTime.getTimeAsDouble(TimeUnit.HOURS);
        while (bugsCreated > 1) {
            this.lurkingBugs.add(new NormalBug(this, BugType.DEVELOPER_AND_CUSTOMER));
            bugsCreated -= 1.0;
        }
        if (this.getModel().getRandomBool(bugsCreated)) {
            this.lurkingBugs.add(new NormalBug(this, BugType.DEVELOPER_AND_CUSTOMER));
        }

        if (this.implementor.makesBlockerBug()) {
            this.lurkingBugs.add(new GlobalBlockerBug(this.getModel()));
        }
    }

    private void handleAdditionalWaitsForInterruptions() {
        while (!this.implementationInterruptions.isEmpty()) {
            final TimeSpan interruption = this.implementationInterruptions.remove(0);
            this.implementor.hold(interruption);
        }
    }

    public void suspendImplementation(TimeSpan timeSpan) {
        assert this.state == State.IN_IMPLEMENTATION;
        this.getModel().sendTraceNote("suspends implementation of " + this + " for " + timeSpan);
        this.implementationInterruptions.add(timeSpan);
    }

    public void performReview(Developer reviewer) {
        assert this.state == State.READY_FOR_REVIEW;
        assert this.implementor != null;
        assert this.implementor != reviewer;
        assert this.bugsFoundByOthersDuringReview == null;

        this.setState(State.IN_REVIEW);
        this.bugsFoundByOthersDuringReview = new ArrayList<>();
        this.handleTaskSwitchOverhead(reviewer);

        reviewer.hold(this.getModel().getParameters().getReviewTimeDist().sampleTimeSpan(TimeUnit.HOURS));

        final List<Bug> foundBugs = new ArrayList<>();
        for (final Bug b : this.lurkingBugs) {
            if (reviewer.findsBug(b)) {
                foundBugs.add(b);
            }
        }
        foundBugs.addAll(this.bugsFoundByOthersDuringReview);
        this.bugsFoundByOthersDuringReview = null;
        reviewer.sendTraceNote("ends review of " + this + ", found " + foundBugs.size() + " of " + this.lurkingBugs.size() + " bugs");
        this.currentReview = new Review(foundBugs);
        if (foundBugs.isEmpty()) {
            this.endReviewWithoutRemarks(reviewer);
        } else {
            this.endReviewWithRemarks();
        }
    }

    private void setState(State newState) {
        this.getModel().sendTraceNote("changes state of task " + this + " from " + this.state + " to " + newState);
        this.state = newState;
    }

    private void endReviewWithRemarks() {
        assert this.currentReview != null;
        this.setState(State.REJECTED);
        this.getBoard().addTaskWithReviewRemarks(this);
    }

    private void endReviewWithoutRemarks(Developer reviewer) {
        if (this.getModel().getReviewMode() == ReviewMode.PRE_COMMIT) {
            this.commit(reviewer);
        }
        this.setState(State.DONE);
        this.handleFinishedTask();
    }

    protected abstract void handleFinishedTask();

    public void performFixing(Developer dev) {
        assert this.state == State.REJECTED;
        assert this.implementor == dev;
        assert this.implementationInterruptions.isEmpty() : this + " contains interruptions " + this.implementationInterruptions;

        this.setState(State.IN_IMPLEMENTATION);

        if (this.getModel().getReviewMode() == ReviewMode.POST_COMMIT) {
            //im Post-Commit-Fall wurde schon commitet und es muss nochmal "ausgecheckt" werden
            this.getSourceRepository().startWork(this);
        }

        this.handleTaskSwitchOverhead(dev);

        //An sich kann es neben dem Einbauen neuer Bugs auch vorkommen, dass bestehende und bereits angemerkte
        //  Bugs nicht wirklich gefixt werden. Das wird hier vernachlässigt.

        double hoursForFixing = 0.0;
        for (final Bug b : this.currentReview.getRemarks()) {
            hoursForFixing += this.getModel().getParameters().getReviewRemarkFixTimeDist().sample();
        }
        final TimeSpan fixingTime = new TimeSpan(hoursForFixing, TimeUnit.HOURS);
        dev.hold(fixingTime);
        this.lurkingBugs.removeAll(this.currentReview.getRemarks());
        this.bugsFixedInCommit.addAll(this.currentReview.getRemarks());

        this.createBugs(fixingTime);
        this.endImplementation();
    }

    public void performBugAssessment(Developer dev, NormalBug bug) {
        this.handleTaskSwitchOverhead(dev);
        dev.hold(this.getModel().getParameters().getBugAssessmentTimeDist().sampleTimeSpan(TimeUnit.HOURS));

        switch (this.state) {
        case OPEN:
            throw new RuntimeException("Should not happen: Bug in open task " + this);
        case IN_IMPLEMENTATION:
            //TEST
            System.out.println("task in implementation");
            //Task ist gerade in Arbeit: Problem wird gleich miterledigt und verlängert die Implementierung
            this.suspendImplementation(this.getModel().getParameters().getReviewRemarkFixTimeDist().sampleTimeSpan(TimeUnit.HOURS));
            break;
        case READY_FOR_REVIEW:
            //TEST
            System.out.println("task ready for review");
            //Task ist bereit für Review: Bug-Assessment zählt als ein Review-Durchlauf
            this.getBoard().removeTaskFromReviewQueue(this);
            this.currentReview = new Review(Collections.singletonList(bug));
            this.endReviewWithRemarks();
            break;
        case IN_REVIEW:
            //TEST
            System.out.println("task in review");
            //Task wird gerade gereviewt: Bug als zusätzliche Anmerkung aufnehmen
            this.bugsFoundByOthersDuringReview.add(bug);
            break;
        case REJECTED:
            //TEST
            System.out.println("task rejected");
            //Task wurde bereits /wird gerade gereviewt: Bug als zusätzliche Anmerkung aufnehmen
            this.currentReview.addRemark(bug);
            break;
        case DONE:
            //TEST
            System.out.println("task done");
            //Task ist bereits abgeschlossen: Fixing geschieht im Rahmen eines separaten Bugfix-Tickets
            this.getBoard().addBugToBeFixed(new BugfixTask(bug));
            break;
        }

        //TODO Abarbeitung dauert länger, wenn auf dem Buggy-Task andere Dinge aufgebaut haben (propabilistisch)
        //TODO noch in Arbeit befindliche abhängige Tasks verzögern sich (propabilitisch)
        //z.B.: Bug hat Auswirkungen auf Nachfolger-Task:
        //wenn der Nachfolger bereits vollständig abgeschlossen ist, verlängert sich der Bugfix-Task
        //wenn der Nachfolger gerade in der Implementierung ist, verlängert sich die Implementierung
        //wenn der Nachfolger gerade vor der Implementierung ist, passiert nichts
        //wenn der Nachfolger schon

    }

    private void handleTaskSwitchOverhead(Developer dev) {
        final TimeSpan taskSwitchOverhead = this.determineTaskSwitchOverhead(dev);
        assert taskSwitchOverhead.getTimeAsDouble(TimeUnit.HOURS) < 8.0 :
            "more than a day? something must be wrong " + taskSwitchOverhead;

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
            this.handleAdditionalWaitsForInterruptions();
        }

        this.handleCommited();
        this.commited = true;
        for (final Bug b : this.bugsFixedInCommit) {
            b.fix();
        }
        this.bugsFixedInCommit.clear();
        for (final Bug b : this.lurkingBugs) {
            b.handlePublishedForDevelopers();
        }
    }

    protected abstract void handleCommited();

    public boolean isCommited() {
        return this.commited;
    }

    public abstract List<? extends Task> getPrerequisites();

    public TimeSpan getImplementationTime() {
        return this.implementationTime;
    }

    public State getState() {
        return this.state;
    }

    public void startLurkingBugsForCustomer() {
        for (final Bug b : this.lurkingBugs) {
            b.handlePublishedForCustomers();
        }
    }

}
