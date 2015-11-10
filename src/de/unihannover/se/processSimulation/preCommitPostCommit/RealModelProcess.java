package de.unihannover.se.processSimulation.preCommitPostCommit;

import desmoj.core.simulator.SimProcess;

abstract class RealModelProcess extends SimProcess {
//abstract class RealModelProcess extends QuasarContinuationProcess {

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
