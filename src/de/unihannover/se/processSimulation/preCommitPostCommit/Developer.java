package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.LinkedHashMap;
import java.util.Map;

import co.paralleluniverse.fibers.SuspendExecution;
import desmoj.core.dist.BoolDistBernoulli;
import desmoj.core.dist.ContDist;
import desmoj.core.dist.ContDistConstant;
import desmoj.core.simulator.TimeInstant;


//TODO offener Punkt: Beim Mergen können auch Fehler entstehen
class Developer extends RealModelProcess {

    private final BoolDistBernoulli reviewerSkill;
    private final BoolDistBernoulli globalBugDist;
    private final Map<String, TimeInstant> memory;
    private final ContDist implementationSkill;

    public Developer(RealProcessingModel owner, double reviewerSkill, double globalBugProbability, double implementationSkill) {
        super(owner, "developer");
        this.reviewerSkill = new BoolDistBernoulli(owner, "reviewerSkill-" + this, reviewerSkill, true, true);
        this.globalBugDist = new BoolDistBernoulli(owner, "globalBugDist-" + this, globalBugProbability, true, true);
        //as distribution, so that it can be seen in the DESMO report
        this.implementationSkill = new ContDistConstant(owner, "implementationSkill-" + this, implementationSkill, true, true);
        this.memory = new LinkedHashMap<>();
    }

    @Override
    public void lifeCycle() throws SuspendExecution {
        while (true) {
            final Board board = this.getBoard();

            final NormalBug unassessedBug = board.getUnassessedBug();
            final TimeInstant startTime = this.presentTime();
            if (unassessedBug != null) {
                final Task buggyTask = unassessedBug.getTask();
                buggyTask.performBugAssessment(this, unassessedBug);
                this.saveLastTimeIHadToDoWith(buggyTask);
                this.getModel().countTime("assessingBugs", startTime);
                continue;
            }

            final Task taskWithReviewRemarks = board.getTaskWithReviewRemarksFor(this);
            if (taskWithReviewRemarks != null) {
                taskWithReviewRemarks.performFixing(this);
                this.saveLastTimeIHadToDoWith(taskWithReviewRemarks);
                this.getModel().countTime("fixingReviewRemarks", startTime);
                continue;
            }

            final Task taskToReview = board.getTaskToReviewFor(this);
            if (taskToReview != null) {
                taskToReview.performReview(this);
                this.saveLastTimeIHadToDoWith(taskToReview);
                this.getModel().countTime("reviewing", startTime);
                continue;
            }

            final BugfixTask bugToFix = board.getBugToFix(this);
            if (bugToFix != null) {
                bugToFix.performImplementation(this);
                this.saveLastTimeIHadToDoWith(bugToFix);
                this.getModel().countTime("fixingBugs", startTime);
                continue;
            }

            final StoryTask taskToImplement = board.getTaskToImplement(this);
            if (taskToImplement != null) {
                taskToImplement.performImplementation(this);
                this.saveLastTimeIHadToDoWith(taskToImplement);
                this.getModel().countTime("implementing", startTime);
                continue;
            }

            final Story toPlan = board.getStoryToPlan();
            toPlan.plan(this);
            this.saveLastTimeIHadToDoWith(toPlan);
            this.getModel().countTime("planning", startTime);
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
        return this.implementationSkill.sample();
    }

}
