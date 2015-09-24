package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.List;

class StoryTask extends Task {

    private final Story story;

    private final List<? extends Task> prerequisites;

    public StoryTask(RealProcessingModel model, Story story, List<? extends Task> prerequisites) {
        super(model, "story-task");
        this.story = story;
        this.prerequisites = prerequisites;
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

    @Override
    public List<? extends Task> getPrerequisites() {
        return this.prerequisites;
    }

}
