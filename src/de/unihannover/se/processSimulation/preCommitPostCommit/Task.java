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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import co.paralleluniverse.fibers.SuspendExecution;
import de.unihannover.se.processSimulation.common.ReviewMode;
import de.unihannover.se.processSimulation.preCommitPostCommit.NormalBug.BugType;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

/**
 * Abstract represenation of a task, i.e. something that has to be implemented by a {@link Developer}.
 */
abstract class Task extends PrePostEntity implements MemoryItem {

    private enum State {
        /**
         * The task was newly created and nothing has been done yet.
         */
        OPEN,
        /**
         * Someone is currently implementation this task (initially, or fixing remarks).
         */
        IN_IMPLEMENTATION,
        /**
         * An implementation round has been finished and the task should now be reviewed.
         */
        READY_FOR_REVIEW,
        /**
         * Someone is currently reviewing this task.
         */
        IN_REVIEW,
        /**
         * There were review remarks found. Another implementation round is needed.
         */
        REJECTED,
        /**
         * The task is finished.
         */
        DONE
    }

    private final TimeSpan implementationTime;
    private boolean commited;
    private State state;

    /**
     * The developer that performed the initial implementation (aka author).
     */
    private Developer implementor;

    /**
     * Bugs that can still be found in review, i.e. that were neither found in a previous review or fixed for another reason.
     */
    private final List<Bug> lurkingBugs;

    /**
     * Bugs that will be fixed with the next commit. In the case of post commit reviews, this are bugs found in the current review, while for
     * pre commit reviews, this are all bugs found in any review.
     */
    private final List<Bug> bugsFixedInCommit;

    /**
     * Interruptions that occurred during the current implementation session and that will delay it.
     */
    private final List<TimeSpan> implementationInterruptions;

    /**
     * The review that is currently or was last performed.
     */
    private Review currentReview;

    /**
     * Bugs that have been noticed to belong to this task by other developers while it was in review.
     */
    private List<NormalBug> bugsFoundByOthersDuringReview;


    /**
     * Creates a new task which needs the given time for initial implementation.
     */
    public Task(PrePostModel model, String name, TimeSpan implementationTime) {
        super(model, name);
        this.state = State.OPEN;
        this.lurkingBugs = new ArrayList<>();
        this.bugsFixedInCommit = new ArrayList<>();
        this.implementationInterruptions = new ArrayList<>();
        this.implementationTime = implementationTime;
    }

    /**
     * Let the given developer perform the implementation of this task.
     * Returns when implementation is done.
     */
    public void performImplementation(Developer dev) throws SuspendExecution {
        assert this.state == State.OPEN;
        assert this.implementor == null;
        assert this.implementationInterruptions.isEmpty();

        this.setState(State.IN_IMPLEMENTATION);
        this.implementor = dev;

        //mit Jens abgestimmt: erst update, dann Task-Switch-Overhead
        this.getSourceRepository().startWork(this);
        final TimeSpan taskSwitchTime = this.handleTaskSwitchOverhead(dev);
        final TimeSpan implementationTime = this.getImplementationTime();
        this.implementor.hold(implementationTime);

        final TimeSpan bugTime = TimeOperations.add(
                        implementationTime,
                        TimeOperations.multiply(taskSwitchTime, this.getModel().getParameters().getTaskSwitchTimeBugFactor()));
        this.createBugs(bugTime, this instanceof BugfixTask, false);
        this.endImplementation();
    }

    /**
     * Perform the work that has to be done at the end of implementation:
     * Further waiting (if interruptions occured)
     * Commiting (if the review mode demands it)
     * Changing the state and the board according to the review mode
     */
    private void endImplementation() throws SuspendExecution {
        this.handleAdditionalWaitsForInterruptions();

        if (this.getModel().getReviewMode() == ReviewMode.POST_COMMIT || this.getModel().getReviewMode() == ReviewMode.NO_REVIEW) {
            this.commit(this.implementor);
        }
        assert this.implementationInterruptions.isEmpty();
        if (this.getModel().getReviewMode() == ReviewMode.NO_REVIEW) {
            this.setState(State.DONE);
            this.getBoard().removeTaskFromInImplementation(this);
            this.handleFinishedTask();
        } else {
            this.setState(State.READY_FOR_REVIEW);
            this.getBoard().addTaskReadyForReview(this);
        }
    }

    /**
     * Inject bugs into this task. The number of bugs depends on the given time/effort that went into
     * implementation, if it was bug fixing or new implementation and the developer.
     */
    private void createBugs(TimeSpan relevantTime, boolean fixing, boolean reviewRemark) {
        //determine number of bugs to create
        double bugsToCreate = this.implementor.getImplementationSkill() * relevantTime.getTimeAsDouble(TimeUnit.HOURS);
        if (fixing) {
            bugsToCreate *= this.getModel().getParameters().getFixingBugRateFactor();
        }
        for (final Task t : this.getPrerequisites()) {
            for (final Bug b : t.lurkingBugs) {
                assert !b.isFixed();
                final boolean bugSpawnsFollowUpBug = this.getModel().getRandomBool(
                                this.getModel().getParameters().getFollowUpBugSpawnProbability());
                if (bugSpawnsFollowUpBug) {
                    bugsToCreate *= 1;
                }
            }
        }

        //create bugs
        int normalBugsCreated = 0;
        while (bugsToCreate > 1) {
            this.createNormalBug();
            bugsToCreate -= 1.0;
            normalBugsCreated++;
        }
        final boolean withExtraBug = this.getModel().getRandomBool(bugsToCreate);
        if (withExtraBug) {
            this.createNormalBug();
            normalBugsCreated++;
        }
        if (reviewRemark) {
            assert fixing;
            this.getModel().dynamicCount("bugsInjectedWhileFixingReviewRemarks", normalBugsCreated);
        } else if (fixing) {
            this.getModel().dynamicCount("bugsInjectedWhileFixingBugs", normalBugsCreated);
        } else {
            this.getModel().dynamicCount("bugsInjectedWhileImplementing", normalBugsCreated);
        }

        if (this.implementor.makesBlockerBug()) {
            this.lurkingBugs.add(new GlobalBlockerBug(this));
        }
    }

    private void createNormalBug() {
        final BugType type = this.getModel().getParameters().getInternalBugDist().sample()
                        ? BugType.DEVELOPER_ONLY : BugType.DEVELOPER_AND_CUSTOMER;
        this.lurkingBugs.add(new NormalBug(this, type));
    }

    private void handleAdditionalWaitsForInterruptions() throws SuspendExecution {
        final TimeInstant startTime = this.presentTime();
        while (!this.implementationInterruptions.isEmpty()) {
            final TimeSpan interruption = this.implementationInterruptions.remove(0);
            this.implementor.hold(interruption);
        }
        this.getModel().countTime("timeWasted_interruptions", startTime);
    }

    /**
     * Suspends the current implementation for the given amount.
     * Technically, the time to wait is saved and the wait is performed when implementation would normally end
     * (see {@link #handleAdditionalWaitsForInterruptions()}.
     * @pre this.state == State.IN_IMPLEMENTATION
     */
    public void suspendImplementation(TimeSpan timeSpan) {
        assert this.state == State.IN_IMPLEMENTATION;
        this.getModel().sendTraceNote("suspends implementation of " + this + " for " + timeSpan);
        this.implementationInterruptions.add(timeSpan);
    }

    /**
     * Let the given developer perform a review of this task.
     * Returns when the review is done.
     */
    public void performReview(Developer reviewer) throws SuspendExecution {
        assert this.state == State.READY_FOR_REVIEW;
        assert this.implementor != null;
        assert this.implementor != reviewer;
        assert this.bugsFoundByOthersDuringReview == null;
        assert this.getModel().getReviewMode() != ReviewMode.NO_REVIEW;

        this.setState(State.IN_REVIEW);
        this.bugsFoundByOthersDuringReview = new ArrayList<>();
        this.handleTaskSwitchOverhead(reviewer);

        reviewer.hold(this.getModel().getParameters().getReviewTimeDist().sampleTimeSpan(TimeUnit.HOURS));

        final List<Bug> foundBugs = new ArrayList<>();
        for (final Bug b : this.lurkingBugs) {
            if (reviewer.findsBug()) {
                foundBugs.add(b);
            }
        }
        foundBugs.addAll(this.bugsFoundByOthersDuringReview);
        this.bugsFoundByOthersDuringReview = null;
        reviewer.sendTraceNote("ends review of " + this + ", found " + foundBugs.size() + " of " + this.lurkingBugs.size() + " bugs");
        this.currentReview = new Review(foundBugs);
        if (foundBugs.isEmpty()) {
            this.endReviewWithoutRemarks(reviewer);
        } else {
            this.endReviewWithRemarks();
        }
    }

    private void setState(State newState) {
        this.getModel().sendTraceNote("changes state of task " + this + " from " + this.state + " to " + newState);
        this.state = newState;
    }

    private void endReviewWithRemarks() {
        assert this.currentReview != null;
        this.setState(State.REJECTED);
        this.getBoard().addTaskWithReviewRemarks(this);
    }

    private void endReviewWithoutRemarks(Developer reviewer) throws SuspendExecution {
        if (this.getModel().getReviewMode() == ReviewMode.PRE_COMMIT) {
            this.commit(reviewer);
        }
        this.setState(State.DONE);
        this.handleFinishedTask();
    }

    /**
     * Is called when this task is finished (moved to state DONE) and allows the subclasses to perform additional work in this case.
     */
    protected abstract void handleFinishedTask();

    /**
     * Let the given developer (must be the tasks author) fix the remarks found in the last review.
     * Returns when fixing is done.
     */
    public void performFixingOfReviewRemarks(Developer dev) throws SuspendExecution {
        assert this.state == State.REJECTED;
        assert this.getModel().getReviewMode() != ReviewMode.NO_REVIEW;
        assert this.implementor == dev;
        assert this.implementationInterruptions.isEmpty() : this + " contains interruptions " + this.implementationInterruptions;

        this.setState(State.IN_IMPLEMENTATION);

        if (this.getModel().getReviewMode() == ReviewMode.POST_COMMIT) {
            //in post commit mode, the code was already commited and has to be "checked out" again
            this.getSourceRepository().startWork(this);
        }

        final TimeSpan taskSwitchTime = this.handleTaskSwitchOverhead(dev);

        //In reality, it could happen that remarks are not fixed correctly or at all. This is not modeled here,
        //  as these wrong fixes could be regarded as new bugs (which are modeled).

        TimeSpan timeForFixing = new TimeSpan(0);
        for (final Bug b : this.currentReview.getRemarks()) {
            timeForFixing = TimeOperations.add(timeForFixing, this.sampleRemarkFixTime());
        }
        dev.hold(timeForFixing);
        this.lurkingBugs.removeAll(this.currentReview.getRemarks());
        this.bugsFixedInCommit.addAll(this.currentReview.getRemarks());

        final TimeSpan bugTime = TimeOperations.add(
                        timeForFixing,
                        TimeOperations.multiply(taskSwitchTime, this.getModel().getParameters().getTaskSwitchTimeBugFactor()));
        this.createBugs(bugTime, true, true);
        this.endImplementation();
    }

    /**
     * Let the given developer have a look at the given bug and decide what to do with it.
     * Returns when bug assessment is finished.
     */
    public void performBugAssessment(Developer dev, NormalBug bug) throws SuspendExecution {
        this.handleTaskSwitchOverhead(dev);
        dev.hold(this.getModel().getParameters().getBugAssessmentTimeDist().sampleTimeSpan(TimeUnit.HOURS));

        this.getModel().dynamicCount("bugAssessmentResult" + this.state);
        switch (this.state) {
        case OPEN:
            throw new AssertionError("Should not happen: Bug in open task " + this);
        case IN_IMPLEMENTATION:
            //task is currently in work: fixing is done while the author is at it and delays the implementation
            this.suspendImplementation(this.sampleRemarkFixTime());
            break;
        case READY_FOR_REVIEW:
            //tasks is ready for review: bug assessment is seen as a review round
            this.getBoard().removeTaskFromReviewQueue(this);
            this.currentReview = new Review(Collections.singletonList(bug));
            this.endReviewWithRemarks();
            break;
        case IN_REVIEW:
            //task is in review: add bug to the review remarks
            this.bugsFoundByOthersDuringReview.add(bug);
            break;
        case REJECTED:
            //task has been reviewed with remarks: add bug to the review remarks
            this.currentReview.addRemark(bug);
            break;
        case DONE:
            //task is already finished: create a separate bugfix task
            this.getBoard().addBugToBeFixed(new BugfixTask(bug));
            break;
        }
    }

    private TimeSpan sampleRemarkFixTime() {
        return this.getModel().getParameters().getReviewRemarkFixDist().sampleTimeSpan(TimeUnit.HOURS);
    }

    /**
     * Determine the time it takes the developer to switch to this task's topic and hold for
     * this time span.
     */
    private TimeSpan handleTaskSwitchOverhead(Developer dev) throws SuspendExecution {
        final TimeSpan taskSwitchOverhead = this.determineTaskSwitchOverhead(dev);
        assert taskSwitchOverhead.getTimeAsDouble(TimeUnit.HOURS) < 8.0 :
            "more than a day? something must be wrong " + taskSwitchOverhead;

        if (taskSwitchOverhead.getTimeInEpsilon() != 0) {
            final TimeInstant startTime = this.presentTime();
            this.sendTraceNote("has task switch overhead switching to " + this);
            dev.hold(taskSwitchOverhead);
            this.getModel().countTime("timeWasted_taskSwitch", startTime);
        }
        return taskSwitchOverhead;
    }

    private TimeSpan determineTaskSwitchOverhead(Developer dev) {
        final TimeInstant lastTime = dev.getLastTimeYouHadToDoWith(this);
        final TimeSpan max = this.getModel().getParameters().getMaxTaskSwitchOverhead();
        if (lastTime == null) {
            return max;
        }

        if (max.getTimeInEpsilon() == 0) {
            return max;
        }

        final TimeSpan forOneHour = this.getModel().getParameters().getTaskSwitchOverheadAfterOneHourInterruption();
        final double s = - new TimeSpan(1, TimeUnit.HOURS).getTimeAsDouble() / Math.log(1.0 - forOneHour.getTimeAsDouble() / max.getTimeAsDouble());

        final double timeSinceLastTime = this.presentTime().getTimeAsDouble() - lastTime.getTimeAsDouble();
        final double overhead = max.getTimeAsDouble() * (1.0 - Math.exp(- timeSinceLastTime / s));
        return new TimeSpan(overhead);
    }

    /**
     * Returns true iff the given developer is this task's author.
     */
    public boolean wasImplementedBy(Developer developer) {
        return developer.equals(this.implementor);
    }

    /**
     * Returns true iff this task is finished/done.
     */
    public boolean isFinished() {
        return this.state == State.DONE;
    }

    /**
     * Perform a commit. In case of conflict, retry until success.
     * After commit, other developers can find bugs injected with this task.
     */
    private void commit(Developer commiter) throws SuspendExecution {

        while (!this.getSourceRepository().tryCommit(this)) {
            //conflict found => update, resolve conflict, retry
            final TimeInstant resolveStartTime = this.presentTime();
            this.getSourceRepository().restartWork(this);
            commiter.hold(this.getModel().getParameters().getConflictResolutionTimeDist().sampleTimeSpan(TimeUnit.HOURS));
            this.getModel().countTime("timeWasted_resolvingConflicts", resolveStartTime);
            this.handleAdditionalWaitsForInterruptions();
        }

        this.handleCommited();
        this.commited = true;
        for (final Bug b : this.bugsFixedInCommit) {
            b.fix();
        }
        this.bugsFixedInCommit.clear();
        for (final Bug b : this.lurkingBugs) {
            assert !b.isFixed();
            b.handlePublishedForDevelopers();
        }
    }

    /**
     * Is called when this task is committed to allow subclass specific behavior.
     */
    protected abstract void handleCommited();

    /**
     * Returns true iff this tasks main implementation is committed (so that dependent tasks can start).
     */
    public boolean isCommited() {
        return this.commited;
    }

    /**
     * Returns all tasks this task depends on.
     */
    public abstract List<? extends Task> getPrerequisites();

    /**
     * Returns the time needed for the initial implementation of this task (without waste such as
     * task switch, conflicts, ...).
     */
    public final TimeSpan getImplementationTime() {
        return this.implementationTime;
    }

    /**
     * Tell all the bugs that are still lurking (not found by reviews or fixed) that they can now be found
     * by customers.
     */
    public void startLurkingBugsForCustomer() {
        for (final Bug b : this.lurkingBugs) {
            assert !b.isFixed();
            b.handlePublishedForCustomers();
        }
    }

    /**
     * Remove the given fixed bug from the set of lurking bugs.
     */
    void handleBugFixed(Bug bug) {
        assert bug.isFixed();
        this.lurkingBugs.remove(bug);
    }

    /**
     * Returns the story this task belongs to.
     */
    public abstract Story getStory();

}
