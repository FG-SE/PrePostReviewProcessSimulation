package de.unihannover.se.processSimulation.preCommitPostCommit;

public class Developer extends RealModelProcess {

    public Developer(RealProcessingModel owner) {
        super(owner, "developer");
    }

    @Override
    public void lifeCycle() {
        final Board board = this.getBoard();
        final Task taskWithReviewRemarks = board.getTaskWithReviewRemarksFor(this);
        if (taskWithReviewRemarks != null) {
            taskWithReviewRemarks.fixReviewRemarks();
            this.passivate();
        }
        // TODO Auto-generated method stub

    }

}
