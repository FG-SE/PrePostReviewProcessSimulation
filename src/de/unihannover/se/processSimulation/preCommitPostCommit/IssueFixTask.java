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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

/**
 * A {@link Task} in which a certain {@link NormalIssue} shall be fixed.
 */
class IssueFixTask extends Task {

    private final NormalIssue issue;
    private final Story cachedStory;

    /**
     * Creates a new fix task for the given issue.
     */
    public IssueFixTask(NormalIssue issue) {
        super(issue.getModel(), "issue",
                        TimeOperations.add(
                                        issue.getFixEffort(),
                                        issue.getModel().getParameters().getIssuefixTaskOverheadTimeDist().sampleTimeSpan(TimeUnit.HOURS)));
        this.issue = issue;
        this.cachedStory = this.issue.getTask().getStory();
        this.cachedStory.registerIssue(this);
    }

    /**
     * Belongs to the same topic as the task during which the issue was injected.
     */
    @Override
    public String getMemoryKey() {
        return this.cachedStory.getMemoryKey();
    }

    /**
     * Returns the story that belongs to the task during which the issue was injected.
     */
    @Override
    public Story getStory() {
        return this.cachedStory;
    }

    /**
     * Issue fixes don't have prerequisites.
     */
    @Override
    public List<? extends Task> getPrerequisites() {
        return Collections.emptyList();
    }

    /**
     * When the issue fix task is commited, the issue is fixed.
     */
    @Override
    protected void handleCommited() {
        this.issue.fix();
    }

    /**
     * When this task is finished, there are two possibilites:
     * 1. The story is not finished yet (possibly because this fix kept it from finishing). The story will be finished if this
     *      is now possible. Issues injected during this fix's implementation can only become visible to the customer as soon
     *      as the story is finished.
     * 2. The story is finished (i.e. the issue occured after the story had beed finished). Issues injected during this fix's implementation
     *      can immediately become visible to the customer.
     */
    @Override
    protected void handleFinishedTask() {
        if (this.cachedStory.isFinished()) {
            this.startLurkingIssuesForCustomer();
        } else {
            if (this.cachedStory.canBeFinished()) {
                this.cachedStory.finish();
            }
        }
    }

    @Override
    protected TimeSpan getTimeRelevantForIssueCreation() {
        return TimeOperations.multiply(this.issue.getFixEffort(), this.getModel().getParameters().getReviewFixToTaskFactor());
    }

}
