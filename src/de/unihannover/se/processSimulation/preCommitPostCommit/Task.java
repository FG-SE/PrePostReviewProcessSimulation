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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import co.paralleluniverse.fibers.SuspendExecution;
import de.unihannover.se.processSimulation.common.ReviewMode;
import de.unihannover.se.processSimulation.preCommitPostCommit.NormalIssue.IssueType;
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
     * Issues that can still be found in review, i.e. that were neither found in a previous review or fixed for another reason.
     */
    private final List<Issue> lurkingIssues;

    /**
     * Issues that will be fixed with the next commit. In the case of post commit reviews, this are issues found in the current review, while for
     * pre commit reviews, this are all issues found in any review.
     */
    private final List<Issue> issuesFixedInCommit;

    /**
     * Interruptions that occurred during the current implementation session and that will delay it.
     */
    private final List<TimeSpan> implementationInterruptions;

    /**
     * The review that is currently or was last performed.
     */
    private Review currentReview;

    /**
     * The number of reviews that were performed (including the one that is currently performed). Helper for statistics.
     */
    private int reviewRounds;

    /**
     * Issues that have been noticed to belong to this task by other developers while it was in review.
     */
    private List<NormalIssue> issuesFoundByOthersDuringReview;

    /**
     * The time issues (would) become active for developers when doing post commit reviews. Helper for statistics.
     */
    private TimeInstant timeActivePost;

    /**
     * The time issues (would) become active for developers when doing pre commit reviews. Helper for statistics.
     */
    private TimeInstant timeActivePre;


    /**
     * Creates a new task which needs the given time for initial implementation.
     */
    public Task(PrePostModel model, String name, TimeSpan implementationTime) {
        super(model, name);
        this.state = State.OPEN;
        this.lurkingIssues = new ArrayList<>();
        this.issuesFixedInCommit = new ArrayList<>();
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

        final TimeSpan issueTime = TimeOperations.add(
                        this.getTimeRelevantForIssueCreation(),
                        TimeOperations.multiply(taskSwitchTime, this.getModel().getParameters().getTaskSwitchTimeIssueFactor()));
        this.createIssues(issueTime, this instanceof IssueFixTask, -1);
        this.endImplementation();
    }

    /**
     * Returns the time/effort that is used to calculate the number of issues injected while implementing this task.
     */
    protected abstract TimeSpan getTimeRelevantForIssueCreation();

    /**
     * Perform the work that has to be done at the end of implementation:
     * Further waiting (if interruptions occurred)
     * Committing (if the review mode demands it)
     * Changing the state and the board according to the review mode
     */
    private void endImplementation() throws SuspendExecution {
        this.handleAdditionalWaitsForInterruptions();

        if (this.getModel().getReviewMode() == ReviewMode.POST_COMMIT || this.getModel().getReviewMode() == ReviewMode.NO_REVIEW) {
            this.commit(this.implementor);
        }
        this.timeActivePost = this.presentTime();
        assert this.implementationInterruptions.isEmpty();
        if (this.getModel().getReviewMode() == ReviewMode.NO_REVIEW) {
            this.setState(State.DONE);
            this.getModel().updateReviewRoundStatistic(this.reviewRounds);
            this.getBoard().removeTaskFromInImplementation(this);
            this.handleFinishedTask();
        } else {
            this.setState(State.READY_FOR_REVIEW);
            this.getBoard().addTaskReadyForReview(this);
        }
    }

    /**
     * Inject issues into this task. The number of issues depends on the given time/effort that went into
     * implementation, if it was issue fixing or new implementation and the developer.
     */
    private void createIssues(TimeSpan relevantTime, boolean fixing, int reviewRemarkCount) {
        //determine number of issues to create
        double issuesToCreate = this.implementor.getImplementationSkill() * relevantTime.getTimeAsDouble(TimeUnit.HOURS);
        if (fixing) {
            issuesToCreate *= this.getModel().getParameters().getFixingIssueRateFactor();
        }
        if (!fixing) {
            for (final Task t : this.getPrerequisites()) {
                for (final Issue b : t.lurkingIssues) {
                    assert !b.isFixed();
                    final boolean issueSpawnsFollowUpIssue = this.getModel().getRandomBool(
                                    this.getModel().getParameters().getFollowUpIssueSpawnProbability());
                    if (issueSpawnsFollowUpIssue) {
                        issuesToCreate += 1;
                    }
                }
            }
        }

        //create issues
        int normalIssuesCreated = 0;
        while (issuesToCreate > 1) {
            this.createNormalIssue();
            issuesToCreate -= 1.0;
            normalIssuesCreated++;
        }
        final boolean withExtraIssue = this.getModel().getRandomBool(issuesToCreate);
        if (withExtraIssue) {
            this.createNormalIssue();
            normalIssuesCreated++;
        }
        if (reviewRemarkCount > 0) {
            assert fixing;
            this.getModel().dynamicCount("issuesInjectedWhileFixingReviewRemarks", normalIssuesCreated);
            this.getModel().updateIssuesInjectedPerReviewRemark(((double) normalIssuesCreated) / reviewRemarkCount);
        } else if (fixing) {
            this.getModel().dynamicCount("issuesInjectedWhileFixingIssues", normalIssuesCreated);
            this.getModel().updateIssuesInjectedPerIssueTask(normalIssuesCreated);
        } else {
            this.getModel().dynamicCount("issuesInjectedWhileImplementing", normalIssuesCreated);
            this.getModel().updateIssuesInjectedPerImplementationTask(normalIssuesCreated);
        }

        if (this.implementor.makesBlockerIssue()) {
            //Currently, a blocker issue is added regardless of the type of change and even when there already is
            //  another blocker issue lurking. This is correct if the time lost is proportional to the number of
            //  blocker issues inserted. A smaller increase per lurking blocker issue would certainly be more realistic.
            this.lurkingIssues.add(new GlobalBlockerIssue(this));
        }
    }

    private void createNormalIssue() {
        final IssueType type = this.getModel().getParameters().getInternalIssueDist().sample()
                        ? IssueType.DEVELOPER_ONLY : IssueType.DEVELOPER_AND_CUSTOMER;
        this.lurkingIssues.add(new NormalIssue(this, type));
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
     * Suspends the current implementation for the given amount to fix a issue/remark.
     * @pre this.state == State.IN_IMPLEMENTATION
     */
    public void suspendImplementationForFixing(Issue issue) {
        assert issue.wasObserved();
        assert !issue.isFixed();

        //there is no task switch overhead because the fix belongs to the current task
        final TimeSpan timeSpan = issue.getFixEffort();
        this.createIssues(timeSpan, true, 1);
        this.suspendImplementation(timeSpan);
        this.lurkingIssues.remove(issue);
        this.issuesFixedInCommit.add(issue);
    }

    /**
     * Let the given developer perform a review of this task.
     * Returns when the review is done.
     */
    public void performReview(Developer reviewer) throws SuspendExecution {
        assert this.state == State.READY_FOR_REVIEW;
        assert this.implementor != null;
        assert this.implementor != reviewer;
        assert this.issuesFoundByOthersDuringReview == null;
        assert this.getModel().getReviewMode() != ReviewMode.NO_REVIEW;

        this.setState(State.IN_REVIEW);
        this.reviewRounds++;
        this.issuesFoundByOthersDuringReview = new ArrayList<>();
        this.handleTaskSwitchOverhead(reviewer);

        reviewer.hold(this.getModel().getParameters().getReviewTimeDist().sampleTimeSpan(TimeUnit.HOURS));

        final Set<Issue> foundIssues = new LinkedHashSet<>();
        for (final Issue b : this.lurkingIssues) {
            if (reviewer.findsIssue()) {
                foundIssues.add(b);
            }
        }
        foundIssues.addAll(this.issuesFoundByOthersDuringReview);
        this.issuesFoundByOthersDuringReview = null;
        assert this.lurkingIssues.containsAll(foundIssues);
        reviewer.sendTraceNote("ends review of " + this + ", found " + foundIssues.size() + " of " + this.lurkingIssues.size() + " issues " + foundIssues);
        this.currentReview = new Review(foundIssues);
        if (foundIssues.isEmpty()) {
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
        this.timeActivePre = this.presentTime();
        this.setState(State.DONE);
        this.getModel().updateReviewRoundStatistic(this.reviewRounds);
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
            //in post commit mode, the code was already committed and has to be "checked out" again
            this.getSourceRepository().startWork(this);
        }

        final TimeSpan taskSwitchTime = this.handleTaskSwitchOverhead(dev);

        //In reality, it could happen that remarks are not fixed correctly or at all. This is not modeled here,
        //  as these wrong fixes could be regarded as new issues (which are modeled).

        assert !this.currentReview.getRemarks().isEmpty();
        TimeSpan timeForFixing = new TimeSpan(0);
        for (final Issue b : this.currentReview.getRemarks()) {
            timeForFixing = TimeOperations.add(timeForFixing, b.getFixEffort());
        }
        dev.hold(timeForFixing);
        this.lurkingIssues.removeAll(this.currentReview.getRemarks());
        this.issuesFixedInCommit.addAll(this.currentReview.getRemarks());

        final TimeSpan issueTime = TimeOperations.add(
                        timeForFixing,
                        TimeOperations.multiply(taskSwitchTime, this.getModel().getParameters().getTaskSwitchTimeIssueFactor()));
        this.createIssues(issueTime, true, this.currentReview.getRemarks().size());
        this.endImplementation();
    }

    /**
     * Let the given developer have a look at the given issue and decide what to do with it.
     * Returns when issue assessment is finished.
     */
    public void performIssueAssessment(Developer dev, NormalIssue issue) throws SuspendExecution {
        this.handleTaskSwitchOverhead(dev);
        dev.hold(this.getModel().getParameters().getIssueAssessmentTimeDist().sampleTimeSpan(TimeUnit.HOURS));

        if (issue.wasObserved()) {
            //possibly the issue was already found in a review while the developer was busy doing bug assessment
            //  when he finally figures that out, there's nothing more to do
            this.getModel().dynamicCount("issueAssessmentResultAlreadyObserved");
            return;
        }
        this.getModel().dynamicCount("issueAssessmentResult" + this.state + "withStory" + (this.getStory().isFinished() ? "Finished" : "InWork"));
        issue.setWasObserved();
        switch (this.state) {
        case OPEN:
            throw new AssertionError("Should not happen: Issue in open task " + this);
        case IN_IMPLEMENTATION:
            //task is currently in work: fixing is done while the author is at it and delays the implementation
            this.suspendImplementationForFixing(issue);
            break;
        case READY_FOR_REVIEW:
            //tasks is ready for review: issue assessment is seen as a review round (but not counted as one)
            this.getBoard().removeTaskFromReviewQueue(this);
            this.currentReview = new Review(Collections.singletonList(issue));
            this.endReviewWithRemarks();
            break;
        case IN_REVIEW:
            //task is in review: add issue to the review remarks
            this.issuesFoundByOthersDuringReview.add(issue);
            break;
        case REJECTED:
            //task has been reviewed with remarks: add issue to the review remarks
            this.currentReview.addRemark(issue);
            break;
        case DONE:
            //task is already finished: create a separate issuefix task
            this.getBoard().addIssueToBeFixed(new IssueFixTask(issue));
            break;
        }
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
     * After commit, other developers can find issues injected with this task.
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
        assert containsNoDuplicates(this.issuesFixedInCommit);
        for (final Issue b : this.issuesFixedInCommit) {
            b.fix();
        }
        this.issuesFixedInCommit.clear();
        for (final Issue b : this.lurkingIssues) {
            assert !b.isFixed();
            b.handlePublishedForDevelopers();
        }
    }

    private static boolean containsNoDuplicates(List<Issue> l) {
        return l.size() == new HashSet<Issue>(l).size();
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
     * Tell all the issues that are still lurking (not found by reviews or fixed) that they can now be found
     * by customers.
     */
    public void startLurkingIssuesForCustomer() {
        for (final Issue b : this.lurkingIssues) {
            assert !b.isFixed();
            b.handlePublishedForCustomers();
        }
        if (this.getModel().getReviewMode() != ReviewMode.NO_REVIEW) {
            this.getModel().updateTimePostToPreStatistic(TimeOperations.diff(this.timeActivePre, this.timeActivePost));
            this.getModel().updateTimePreToCustStatistic(TimeOperations.diff(this.presentTime(), this.timeActivePre));
        }
    }

    /**
     * Remove the given fixed issue from the set of lurking issues.
     */
    void handleIssueFixed(Issue issue) {
        assert issue.isFixed();
        this.lurkingIssues.remove(issue);
    }

    /**
     * Returns the story this task belongs to.
     */
    public abstract Story getStory();

}
