package de.unihannover.se.processSimulation.preCommitPostCommit;

import desmoj.core.simulator.SimProcess;

public abstract class RealModelProcess extends SimProcess {

    private Board board;

    public RealModelProcess(RealProcessingModel owner, String name) {
        super(owner, name, true);
    }

    protected Board getBoard() {
        return this.getModel().getBoard();
    }

    @Override
    public RealProcessingModel getModel() {
        return (RealProcessingModel) super.getModel();
    }

}
