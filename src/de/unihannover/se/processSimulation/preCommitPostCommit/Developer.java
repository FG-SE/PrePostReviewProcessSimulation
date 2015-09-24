package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.HashMap;
import java.util.Map;

import desmoj.core.dist.BoolDistBernoulli;
import desmoj.core.simulator.TimeInstant;

class Developer extends RealModelProcess {

    private final BoolDistBernoulli reviewerSkill;
    private final BoolDistBernoulli globalBugDist;
    private final Map<String, TimeInstant> memory;
    private final double implementationSkill;

    public Developer(RealProcessingModel owner, double reviewerSkill, double globalBugPropability, double implementationSkill) {
        super(owner, "developer");
        this.reviewerSkill = new BoolDistBernoulli(owner, "reviewerSkill-" + this, reviewerSkill, true, true);
        this.globalBugDist = new BoolDistBernoulli(owner, "globalBugDist-" + this, globalBugPropability, true, true);
        this.implementationSkill = implementationSkill;
        this.memory = new HashMap<>();
    }

    @Override
    public void lifeCycle() {
        while (true) {
            final Board board = this.getBoard();

            final Task taskWithReviewRemarks = board.getTaskWithReviewRemarksFor(this);
            if (taskWithReviewRemarks != null) {
                taskWithReviewRemarks.performFixing(this);
                this.saveLastTimeIHadToDoWith(taskWithReviewRemarks);
                continue;
            }

            final Task taskToReview = board.getTaskToReviewFor(this);
            if (taskToReview != null) {
                taskToReview.performReview(this);
                this.saveLastTimeIHadToDoWith(taskToReview);
                continue;
            }

            final BugfixTask bugToFix = board.getBugToFix(this);
            if (bugToFix != null) {
                bugToFix.performImplementation(this);
                this.saveLastTimeIHadToDoWith(bugToFix);
                continue;
            }

            final StoryTask taskToImplement = board.getTaskToImplement(this);
            if (taskToImplement != null) {
                taskToImplement.performImplementation(this);
                this.saveLastTimeIHadToDoWith(taskToImplement);
                continue;
            }

            final Story toPlan = board.getStoryToPlan();
            toPlan.plan(this);
            this.saveLastTimeIHadToDoWith(toPlan);
        }
    }

    public boolean findsBug(Bug b) {
        return this.reviewerSkill.sample();
    }

    public TimeInstant getLastTimeYouHadToDoWith(MemoryItem item) {
        return this.memory.get(item.getMemoryKey());
    }

    private void saveLastTimeIHadToDoWith(MemoryItem task) {
        this.memory.put(task.getMemoryKey(), this.presentTime());
    }

    public boolean makesBlockerBug() {
        return this.globalBugDist.sample();
    }

    public double getImplementationSkill() {
        return this.implementationSkill;
    }

}
