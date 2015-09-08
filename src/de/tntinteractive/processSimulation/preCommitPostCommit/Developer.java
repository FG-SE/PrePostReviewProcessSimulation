package de.tntinteractive.processSimulation.preCommitPostCommit;

import desmoj.core.dist.BoolDistBernoulli;

public class Developer extends DevelopmentSimProcess {

    private final BoolDistBernoulli reviewEffectivenessDist;

    public Developer(DevelopmentProcessModel owner, String name, double reviewEffectiveness) {
        super(owner, name);
        this.reviewEffectivenessDist = new BoolDistBernoulli(owner, this.getName() + " review effectiveness", reviewEffectiveness, true, true);
    }

    @Override
    public void lifeCycle() {
        while (true) {
            final Board board = this.getBoard();

            final Task remarksToFix = board.getTaskWithReviewRemarksToFixFor(this);
            if (remarksToFix != null) {
                remarksToFix.fixReviewRemarks();
                this.passivate();
                continue;
            }

            final Task toReview = board.getTaskToReview(this);
            if (toReview != null) {
                toReview.startReview(this);
                this.passivate();
                continue;
            }

            final Task bugToFix = board.getBugToFix();
            if (bugToFix != null) {
                bugToFix.startImplementation(this);
                this.passivate();
                continue;
            }

            final Task toImplement = board.getOpenTaskWithSatisfiedPreconditionsFromStory();
            if (toImplement != null) {
                toImplement.startImplementation(this);
                this.passivate();
                continue;
            }

            //TODO: realistischer machen? WIP-Limit für Stories in Planung?
            final Story toJoin = board.getStoryToJoinPlanning();
            if (toJoin != null) {
                toJoin.joinPlanning(this);
                this.passivate();
                continue;
            }

            final Story toPlan = board.getStoryToPlan();
            toPlan.startPlanning(this, this.getModel());
            this.passivate();
        }
    }

    public boolean findsBugInReview(Bug b) {
        return this.reviewEffectivenessDist.sample();
    }

}
