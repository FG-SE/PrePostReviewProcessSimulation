package de.unihannover.se.processSimulation.preCommitPostCommit;

import desmoj.core.dist.BoolDistBernoulli;
import desmoj.core.simulator.TimeInstant;

class Developer extends RealModelProcess {

    private final BoolDistBernoulli reviewerSkill;

    public Developer(RealProcessingModel owner, double reviewerSkill) {
        super(owner, "developer");
        this.reviewerSkill = new BoolDistBernoulli(owner, "reviewerSkill-" + this, reviewerSkill, true, true);
    }

    @Override
    public void lifeCycle() {
        while (true) {
            final Board board = this.getBoard();

            final Task taskWithReviewRemarks = board.getTaskWithReviewRemarksFor(this);
            if (taskWithReviewRemarks != null) {
                taskWithReviewRemarks.performFixing(this);
                continue;
            }

            final Task taskToReview = board.getTaskToReviewFor(this);
            if (taskToReview != null) {
                taskToReview.performReview(this);
                continue;
            }

            final BugfixTask bugToFix = board.getBugToFix(this);
            if (bugToFix != null) {
                bugToFix.performImplementation(this);
                continue;
            }

            final StoryTask taskToImplement = board.getTaskToImplement(this);
            if (taskToImplement != null) {
                taskToImplement.performImplementation(this);
                continue;
            }

            final Story toPlan = board.getStoryToPlan();
            toPlan.plan(this);
        }
    }

    public boolean findsBug(Bug b) {
        return this.reviewerSkill.sample();
    }

    public TimeInstant getLastTimeYouHadToDoWith(Task task) {
        //TODO Implementieren
        return null;
    }

}
