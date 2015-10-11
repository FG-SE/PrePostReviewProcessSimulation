package de.unihannover.se.processSimulation.preCommitPostCommit;

import desmoj.core.simulator.ExternalEvent;
import desmoj.core.simulator.Model;
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

    private boolean started;
    private boolean fixed;

    public Bug(RealProcessingModel model, String name) {
        super(model, name);
        model.countBugCreated();
    }

    public final void startTicking() {
        if (this.started || this.fixed) {
            return;
        }
        this.started = true;
        new BugBecomesVisibleEvent(this.getModel(), this.getName()).schedule(this.getActivationTime());
    }

    protected abstract TimeSpan getActivationTime();

    protected abstract void becomeVisible();

    public final void fix() {
        if (!this.fixed) {
            this.fixed = true;
            this.getModel().countBugFixed();
        }
    }

}
