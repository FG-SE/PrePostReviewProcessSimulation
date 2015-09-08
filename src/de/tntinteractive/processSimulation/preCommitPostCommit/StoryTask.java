package de.tntinteractive.processSimulation.preCommitPostCommit;

import java.util.List;

public class StoryTask extends Task {

    private final Story story;

    public StoryTask(Story story, List<StoryTask> prerequisites) {
        super(story.getModel(), story.getName() + " - Task", story.getModel().getParameters().getImplementationTime(), prerequisites);
        this.sendTraceNote(this + " has prerequisites " + prerequisites);
        this.story = story;
    }

    public Story getStory() {
        return this.story;
    }

}
