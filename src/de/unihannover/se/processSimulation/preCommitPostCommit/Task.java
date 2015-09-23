package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.TimeSpan;

class Task extends RealModelEntity {

    public enum State {
        OPEN,
        READY_FOR_REVIEW,
        REJECTED,
        DONE
    }

    private State state;
    private Developer implementor;

    private final List<Bug> lurkingBugs;
    private Review currentReview;

    public Task(RealProcessingModel model, String name) {
        super(model, name);
        this.state = State.OPEN;
        this.lurkingBugs = new ArrayList<>();
    }

    public void performImplementation(Developer dev) {
        assert this.state == State.OPEN;
        assert this.implementor == null;
        this.implementor = dev;
        this.getSourceRepository().startWork(this);
        //TODO Zeit für Implementierung
        this.implementor.hold(new TimeSpan(4, TimeUnit.HOURS));

        //TODO Bug-Anzahl abhängig von Entwicklerskill und Umfang des Tasks; Entwicklerskill gemessen in Bugs/Stunde, ggf. zusätzlich mit Varianz
        if (this instanceof StoryTask) {
            this.lurkingBugs.add(new Bug(this));
        }

        this.endImplementation();
    }

    private void endImplementation() {
        //TODO Mega-Bugs
        //TODO Störungen zwishchendurch

        if (this.getModel().getReviewMode() == ReviewMode.POST_COMMIT) {
            this.commit(this.implementor);
        }
        this.state = State.READY_FOR_REVIEW;
        this.getBoard().addTaskReadyForReview(this);
    }

    public void performReview(Developer reviewer) {
        assert this.state == State.READY_FOR_REVIEW;
        assert this.implementor != null;
        assert this.implementor != reviewer;

        //TODO Zeit für Review
        reviewer.hold(new TimeSpan(2, TimeUnit.HOURS));

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

        if (this.getModel().getReviewMode() == ReviewMode.POST_COMMIT) {
            //im Post-Commit-Fall wurde schon commitet und es muss nochmal "ausgecheckt" werden
            this.getSourceRepository().startWork(this);
        }

        //TODO Zeit für Fixing
        dev.hold(new TimeSpan(1, TimeUnit.HOURS));
        //TODO nicht alle Bugs werden gefixt
        for (final Bug b : this.currentReview.getRemarks()) {
            this.lurkingBugs.remove(b);
            b.fix();
        }

        this.endImplementation();
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
            //TODO Zeit für Konfliktbehebung
            commiter.hold(new TimeSpan(15, TimeUnit.MINUTES));
        }

        for (final Bug b : this.lurkingBugs) {
            b.activate();
        }
    }
}
