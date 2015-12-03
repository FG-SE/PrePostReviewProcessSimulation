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
import java.util.List;
import java.util.concurrent.TimeUnit;

class StoryTask extends Task {

    private final Story story;

    private final List<StoryTask> prerequisites;

    public StoryTask(RealProcessingModel model, Story story) {
        super(model, "story-task", model.getParameters().getImplementationTimeDist().sampleTimeSpan(TimeUnit.HOURS));
        this.story = story;
        this.prerequisites = new ArrayList<>();
        story.addTaskHelper(this);
    }

    @Override
    public Story getStory() {
        return this.story;
    }

    @Override
    public String getMemoryKey() {
        return this.story.getMemoryKey();
    }

    public boolean arePrerequisitesGiven() {
        for (final Task t : this.prerequisites) {
            if (!t.isCommited()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return super.toString() + " (" + this.story.toString() + ")";
    }

    void addPrerequisite(StoryTask t) {
        this.prerequisites.add(t);
    }

    @Override
    public List<? extends Task> getPrerequisites() {
        return this.prerequisites;
    }

    @Override
    protected void handleCommited() {
    }

    @Override
    protected void handleFinishedTask() {
        if (this.story.canBeFinished()) {
            this.story.finish();
        }
    }

}
