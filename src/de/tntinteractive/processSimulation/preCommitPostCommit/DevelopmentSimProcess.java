package de.tntinteractive.processSimulation.preCommitPostCommit;

import desmoj.core.simulator.SimProcess;

public abstract class DevelopmentSimProcess extends SimProcess {

    public DevelopmentSimProcess(DevelopmentProcessModel owner, String name) {
        super(owner, name, true);
    }

    @Override
    public DevelopmentProcessModel getModel() {
        return (DevelopmentProcessModel) super.getModel();
    }

    protected Board getBoard() {
        return this.getModel().getBoard();
    }

}
