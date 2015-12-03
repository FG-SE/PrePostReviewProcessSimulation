/**
    This file is part of LUH PrePostReview Process Simulation.

    LUH PrePostReview Process Simulation is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    LUH PrePostReview Process Simulation is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with LUH PrePostReview Process Simulation. If not, see <http://www.gnu.org/licenses/>.
 */

package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.LinkedHashMap;
import java.util.Map;

import co.paralleluniverse.fibers.SuspendExecution;
import desmoj.core.dist.BoolDistBernoulli;
import desmoj.core.dist.ContDist;
import desmoj.core.dist.ContDistConstant;
import desmoj.core.simulator.TimeInstant;

/**
 * Representation of a software developer. Software developers are the main active processes of the model.
 * They interact with the {@link Board} to know what to do and act accordingly.
 *
 * Every developer has certain skills: For implementation (bugs injected/hour and chance to insert a global bug
 * during implementation) and for reviewing (chance to detect a bug in review).
 * A developer also has a "memory" where he knows when he last had contact with a certain topic.
 */
class Developer extends PrePostProcess {

    private final BoolDistBernoulli reviewerSkill;
    private final BoolDistBernoulli globalBugDist;
    private final Map<String, TimeInstant> memory;
    private final ContDist implementationSkill;

    /**
     * Creates a developer with the given skills.
     */
    public Developer(PrePostModel owner, double reviewerSkill, double globalBugProbability, double implementationSkill) {
        super(owner, "developer");
        this.reviewerSkill = new BoolDistBernoulli(owner, "reviewerSkill-" + this, reviewerSkill, true, true);
        this.globalBugDist = new BoolDistBernoulli(owner, "globalBugDist-" + this, globalBugProbability, true, true);
        //as distribution, so that it can be seen in the DESMO report
        this.implementationSkill = new ContDistConstant(owner, "implementationSkill-" + this, implementationSkill, true, true);
        this.memory = new LinkedHashMap<>();
    }

    /**
     * Perform the developers work: Look at the board what to do next, do it, and repeat until infinity (which is actually quite finite).
     * The possible things to do have a strict priority order, with "bug assessment" being the most important and "help another developer
     * in story planning" the least important.
     */
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
                taskWithReviewRemarks.performFixingOfReviewRemarks(this);
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

    private void saveLastTimeIHadToDoWith(MemoryItem task) {
        this.memory.put(task.getMemoryKey(), this.presentTime());
    }

    /**
     * Returns the last time the developer had to do with the given item's topic.
     * Returns null iff he never had contact with that topic before.
     */
    public TimeInstant getLastTimeYouHadToDoWith(MemoryItem item) {
        return this.memory.get(item.getMemoryKey());
    }

    /**
     * Samples a value from the underlying random distribution to determine if
     * the developer will inject a global blocker into his current task.
     * Returns true iff this is the case.
     */
    public boolean makesBlockerBug() {
        return this.globalBugDist.sample();
    }

    /**
     * Returns this developer's implementation skill.
     */
    public double getImplementationSkill() {
        return this.implementationSkill.sample();
    }

    /**
     * Samples a value from the underlying random distribution to determine if
     * the developer will find a bug in review. Returns true iff this is the case.
     */
    public boolean findsBug() {
        return this.reviewerSkill.sample();
    }

}
