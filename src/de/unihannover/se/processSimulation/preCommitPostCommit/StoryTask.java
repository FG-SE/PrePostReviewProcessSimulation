package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.List;

class StoryTask extends Task {

    private final Story story;

    private final List<StoryTask> prerequisites;

    public StoryTask(RealProcessingModel model, Story story) {
        super(model, "story-task");
        this.story = story;
        this.prerequisites = new ArrayList<>();
        story.addTaskHelper(this);
    }

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

}
