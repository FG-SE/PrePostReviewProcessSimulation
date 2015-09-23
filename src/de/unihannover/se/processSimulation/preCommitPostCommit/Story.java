package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

class Story extends RealModelEntity {

    private final int storyPoints;
    private TimeInstant startTime;
    private final List<StoryTask> tasks;

    public Story(RealProcessingModel owner, int storyPoints) {
        super(owner, "story");
        this.storyPoints = storyPoints;
        this.tasks = new ArrayList<>();
    }

    public void plan(Developer developer) {
        this.startTime = this.presentTime();
        developer.sendTraceNote("starts planning of " + this);
        //TODO Zeit f�r Planung
        developer.hold(new TimeSpan(2, TimeUnit.HOURS));

        //TODO: Entwickler als "im Thema" bei dieser Story kennzeichnen
        this.getBoard().addTaskToBeImplemented(new StoryTask(this.getModel(), this));
    }

    void addTaskHelper(StoryTask task) {
        assert task.getStory() == this;
        this.tasks.add(task);
    }

    public boolean allTasksFinished() {
        for (final StoryTask t : this.tasks) {
            if (!t.isFinished()) {
                return false;
            }
        }
        return true;
    }

    public TimeSpan getCycleTime(TimeInstant finishTime) {
        return new TimeSpan(finishTime.getTimeInEpsilon() - this.startTime.getTimeInEpsilon(), TimeOperations.getEpsilon());
    }

    public int getStoryPoints() {
        return this.storyPoints;
    }

}
