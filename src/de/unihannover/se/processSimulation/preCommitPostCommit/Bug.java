package de.unihannover.se.processSimulation.preCommitPostCommit;

import desmoj.core.simulator.ExternalEvent;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

abstract class Bug extends RealModelEntity {

    private final class BugBecomesVisibleEvent extends ExternalEvent {

        public BugBecomesVisibleEvent(Model owner, String name) {
            super(owner, name, true);
        }

        @Override
        public void eventRoutine() {
            if (!Bug.this.fixed) {
                Bug.this.becomeVisible();
            }
        }

    }

    private boolean startedForDevelopers;
    private boolean startedForCustomers;
    private boolean fixed;
    private final TimeSpan fixTime;
    private final Randomness reviewFindabilitySequence;

    public Bug(RealProcessingModel model, String name, Randomness randomness) {
        super(model, name);
        model.countBugCreated();
        this.fixTime = randomness.sampleTimeSpan(this.getModel().getParameters().getBugfixTaskTimeDist());
        this.reviewFindabilitySequence = randomness.forkRandomNumberStream();
    }

    public final void handlePublishedForDevelopers() {
        if (this.startedForDevelopers || this.fixed) {
            return;
        }
        this.startedForDevelopers = true;
        final TimeSpan t = this.getActivationTimeForDevelopers();
        if (t != null) {
            new BugBecomesVisibleEvent(this.getModel(), this.getName()).scheduleNoPreempt(t);
        }
    }

    public final void handlePublishedForCustomers() {
        if (this.startedForCustomers || this.fixed) {
            return;
        }
        this.startedForCustomers = true;
        final TimeSpan t = this.getActivationTimeForCustomers();
        if (t != null) {
            new BugBecomesVisibleEvent(this.getModel(), this.getName()).scheduleNoPreempt(t);
        }
    }

    protected abstract TimeSpan getActivationTimeForDevelopers();
    protected abstract TimeSpan getActivationTimeForCustomers();

    protected abstract void becomeVisible();

    public final void fix() {
        if (!this.fixed) {
            this.fixed = true;
            this.getModel().countBugFixed();
        }
    }

    public TimeSpan getFixTaskTime() {
        return this.fixTime;
    }

    public TimeSpan getRemarkFixTime() {
        return TimeOperations.multiply(this.fixTime, this.getModel().getParameters().getReviewRemarkFixFactor());
    }

    public boolean isFoundBy(Developer reviewer) {
        return this.reviewFindabilitySequence.sampleBoolean(reviewer.getReviewSkill());
    }

}
