package de.unihannover.se.processSimulation.preCommitPostCommit;

import desmoj.core.simulator.ExternalEvent;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeSpan;

abstract class Bug extends RealModelEntity {

    private final class BugBecomesVisibleEvent extends ExternalEvent {

        private final boolean byCustomer;

        public BugBecomesVisibleEvent(Model owner, String name, boolean byCustomer) {
            super(owner, name, true);
            this.byCustomer = byCustomer;
        }

        @Override
        public void eventRoutine() {
            if (!Bug.this.fixed) {
                Bug.this.becomeVisible(this.byCustomer);
            }
        }

    }

    private boolean startedForDevelopers;
    private boolean startedForCustomers;
    private boolean fixed;

    public Bug(RealProcessingModel model, String name) {
        super(model, name);
    }

    public final void handlePublishedForDevelopers() {
        if (this.startedForDevelopers || this.fixed) {
            return;
        }
        this.startedForDevelopers = true;
        final TimeSpan t = this.getActivationTimeForDevelopers();
        if (t != null) {
            new BugBecomesVisibleEvent(this.getModel(), this.getName(), false).schedule(t);
        }
    }

    public final void handlePublishedForCustomers() {
        if (this.startedForCustomers || this.fixed) {
            return;
        }
        this.startedForCustomers = true;
        final TimeSpan t = this.getActivationTimeForCustomers();
        if (t != null) {
            new BugBecomesVisibleEvent(this.getModel(), this.getName(), true).schedule(t);
        }
    }

    protected abstract TimeSpan getActivationTimeForDevelopers();
    protected abstract TimeSpan getActivationTimeForCustomers();

    protected abstract void becomeVisible(boolean byCustomer);

    public final void fix() {
        if (!this.fixed) {
            this.fixed = true;
        }
    }

}
