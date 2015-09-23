package de.unihannover.se.processSimulation.preCommitPostCommit;

import desmoj.core.simulator.Entity;

class RealModelEntity extends Entity {

    public RealModelEntity(RealProcessingModel owner, String name) {
        super(owner, name, true);
    }

    protected Board getBoard() {
        return this.getModel().getBoard();
    }

    protected SourceRepository getSourceRepository() {
        return this.getModel().getSourceRepository();
    }

    @Override
    public RealProcessingModel getModel() {
        return (RealProcessingModel) super.getModel();
    }

}
