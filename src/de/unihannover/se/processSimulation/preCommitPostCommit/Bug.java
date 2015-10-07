package de.unihannover.se.processSimulation.preCommitPostCommit;

import desmoj.core.simulator.TimeSpan;

abstract class Bug extends RealModelProcess {

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
        this.activate();
    }

    @Override
    public void lifeCycle() {
        assert this.started;
        if (this.fixed) {
            return;
        }

        this.hold(this.getActivationTime());

        if (!this.fixed) {
            this.explode();
        }
    }

    protected abstract TimeSpan getActivationTime();

    protected abstract void explode();

    public final void fix() {
        if (!this.fixed) {
            this.fixed = true;
            this.getModel().countBugFixed();
        }
    }

}
