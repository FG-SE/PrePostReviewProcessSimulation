package de.tntinteractive.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.List;

import desmoj.core.simulator.TimeSpan;

public abstract class Task extends DevelopmentSimProcess {

    private static enum State {
        OPEN,
        IMPLEMENTED,
        FINISHED;

        public boolean isImplementedOrLater() {
            return this != OPEN;
        }

        public boolean isFinished() {
            return this == State.FINISHED;
        }
    }

    private Developer implementer;
    private Review mostRecentReview;
    private final TimeSpan implementationTime;
    private State state;

    private final List<Task> prerequisites;

    private List<Bug> lurkingBugs;

    public Task(DevelopmentProcessModel owner, String name, TimeSpan implementationTime, List<StoryTask> prerequisites) {
        super(owner, name);
        this.implementationTime = implementationTime;
        this.prerequisites = new ArrayList<>(prerequisites);
        this.state = State.OPEN;
    }

    public void startImplementation(Developer developer) {
        assert this.implementer == null;
        this.implementer = developer;
        this.activate();
    }

    public void startReview(Developer reviewer) {
        this.mostRecentReview = new Review(reviewer);
        this.activate();
    }

    public void fixReviewRemarks() {
        assert this.mostRecentReview.hasRemarks();
        this.activate();
    }

    @Override
    public void lifeCycle() {
        //TODO Zeitverlust durch Taskwechsel/Wartezeit zwischen Tasks einbauen?
        this.hold(this.implementationTime);
        this.endImplementation();

        while (true) {
            this.passivate();
            this.hold(this.getModel().getParameters().getReviewTime());
            this.endReview();

            if (this.isFinished()) {
                break;
            }

            this.passivate();
            this.hold(this.getModel().getParameters().getRemarkFixTime(this.mostRecentReview.getRemarks().size()));
            this.endFixReviewRemarks();
        }
    }

    private void endImplementation() {
        assert this.lurkingBugs == null;
        assert this.state == State.OPEN;

        this.lurkingBugs = new ArrayList<>();
        //TODO unterschiedliche Bugs einbauen
        //TODO Bugs mit unterschiedlicher Wahrscheinlichkeit einbauen, je nachdem ob Bugfix oder normaler Task? Oder ergibt sich das,
        //      weil die Bug-Wahrscheinlichkeit vom Task-Umfang abhängt, und dieser bei Bugfixes kleiner ist?
        this.lurkingBugs.add(new Bug(this.getModel()));
        if (this.getModel().getReviewMode() == ReviewMode.POST_COMMIT) {
            this.performCommit();
        }

        this.implementer.activate();
        this.sendTraceNote("implementation finished, lurking bugs: " + this.lurkingBugs.size());
        this.getBoard().addImplementedTask(this);
    }

    private void performCommit() {
        this.state = State.IMPLEMENTED;
        Bug.startTickingForAll(this.lurkingBugs);
    }

    private void endFixReviewRemarks() {
        //TODO Fehler beim Fixen
        this.lurkingBugs.removeAll(this.mostRecentReview.getRemarks());
        for (final Bug b : this.mostRecentReview.getRemarks()) {
            b.setFixed();
        }
        this.implementer.activate();
        this.sendTraceNote("fixing finished, still lurking bugs: " + this.lurkingBugs.size());
        this.getBoard().addImplementedTask(this);
    }

    private void endReview() {
        this.mostRecentReview.perform(this.lurkingBugs);

        this.mostRecentReview.getReviewer().activate();

        this.sendTraceNote("review finished, remark count: " + this.mostRecentReview.getRemarks().size());
        if (this.mostRecentReview.hasRemarks()) {
            this.getBoard().addTaskWithReviewRemarks(this);
        } else {
            if (this.getModel().getReviewMode() == ReviewMode.PRE_COMMIT) {
                this.performCommit();
            }
            this.state = State.FINISHED;
            this.getBoard().addFinishedTask(this);
        }
    }

    public TimeSpan getEstimatedDuration() {
        return this.implementationTime;
    }

    public boolean isFinished() {
        return this.state.isFinished();
    }

    public Developer getImplementer() {
        return this.implementer;
    }

    public boolean prerequisitesImplemented() {
        for (final Task pre : this.prerequisites) {
            if (!pre.state.isImplementedOrLater()) {
                return false;
            }
        }
        return true;
    }

}
