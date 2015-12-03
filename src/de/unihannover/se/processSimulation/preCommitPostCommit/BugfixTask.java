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

class BugfixTask extends Task {

    private final NormalBug bug;
    private final Story cachedStory;

    public BugfixTask(NormalBug bug) {
        super(bug.getModel(), "bug", bug.getModel().getParameters().getBugfixTaskTimeDist().sampleTimeSpan(TimeUnit.HOURS));
        this.bug = bug;
        this.cachedStory = this.bug.getTask().getStory();
        this.cachedStory.registerBug(this);
    }

    public NormalBug getBug() {
        return this.bug;
    }

    @Override
    public String getMemoryKey() {
        return this.cachedStory.getMemoryKey();
    }

    @Override
    public Story getStory() {
        return this.cachedStory;
    }

    @Override
    public List<? extends Task> getPrerequisites() {
        return Collections.emptyList();
    }

    @Override
    protected void handleCommited() {
        this.bug.fix();
    }

    @Override
    protected void handleFinishedTask() {
        this.startLurkingBugsForCustomer();
        this.cachedStory.unregisterBug(this);
        if (!this.cachedStory.isFinished() && this.cachedStory.canBeFinished()) {
            this.cachedStory.finish();
        }
    }

}
