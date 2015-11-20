package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.List;

import desmoj.core.simulator.TimeSpan;

class StoryTask extends Task {

    private final Story story;

    private final List<StoryTask> prerequisites;
    private final TimeSpan implementationTime;

    public StoryTask(RealProcessingModel model, Story story, Randomness randomness) {
        super(model, "story-task", randomness);
        this.story = story;
        this.prerequisites = new ArrayList<>();
        this.implementationTime = randomness.sampleTimeSpan(model.getParameters().getImplementationTimeDist());
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

    @Override
    protected void handleCommited() {
    }

    @Override
    protected void handleFinishedTask() {
        if (this.story.allTasksFinished()) {
            this.story.finish();
        }
    }

    @Override
    public TimeSpan getImplementationTime() {
        return this.implementationTime;
    }

}
