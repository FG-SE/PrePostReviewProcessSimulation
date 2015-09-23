package de.unihannover.se.processSimulation.preCommitPostCommit;

class StoryTask extends Task {

    private final Story story;

    public StoryTask(RealProcessingModel model, Story story) {
        super(model, "story-task");
        this.story = story;
        story.addTaskHelper(this);
    }

    public Story getStory() {
        return this.story;
    }

    @Override
    public String getMemoryKey() {
        return this.story.getMemoryKey();
    }

}
