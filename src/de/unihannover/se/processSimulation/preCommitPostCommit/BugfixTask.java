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

/**
 * A {@link Task} in which a certain {@link NormalBug} shall be fixed.
 */
class BugfixTask extends Task {

    private final NormalBug bug;
    private final Story cachedStory;

    /**
     * Creates a new bugfix task for the given bug.
     */
    public BugfixTask(NormalBug bug) {
        super(bug.getModel(), "bug", bug.getModel().getParameters().getBugfixTaskTimeDist().sampleTimeSpan(TimeUnit.HOURS));
        this.bug = bug;
        this.cachedStory = this.bug.getTask().getStory();
        this.cachedStory.registerBug(this);
    }

    /**
     * Belongs to the same topic as the task during which the bug was injected.
     */
    @Override
    public String getMemoryKey() {
        return this.cachedStory.getMemoryKey();
    }

    /**
     * Returns the story that belongs to the task during which the bug was injected.
     */
    @Override
    public Story getStory() {
        return this.cachedStory;
    }

    /**
     * Bugfixes don't have prerequisites.
     */
    @Override
    public List<? extends Task> getPrerequisites() {
        return Collections.emptyList();
    }

    /**
     * When the bugfix task is commited, the bug is fixed.
     */
    @Override
    protected void handleCommited() {
        this.bug.fix();
    }

    /**
     * When this task is finished, there are two possibilites:
     * 1. The story is not finished yet (possibly because this bugfix kept it from finishing). The story will be finished if this
     *      is now possible. Bugs injected during this bugfix's implementation can only become visible to the customer as soon
     *      as the story is finished.
     * 2. The story is finished (i.e. the bug occured after it had beed finished). Bugs injected during this bugfix's implementation
     *      can immediately become visible to the customer.
     */
    @Override
    protected void handleFinishedTask() {
        if (this.cachedStory.isFinished()) {
            this.startLurkingBugsForCustomer();
        } else {
            if (this.cachedStory.canBeFinished()) {
                this.cachedStory.finish();
            }
        }
    }

}
