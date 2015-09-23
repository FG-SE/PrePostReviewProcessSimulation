package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

class Story extends RealModelEntity implements MemoryItem {

    private final int storyPoints;
    private TimeInstant startTime;
    private final List<StoryTask> tasks;
    private final List<Developer> additionalPlanners = new ArrayList<>();

    public Story(RealProcessingModel owner, int storyPoints) {
        super(owner, "story");
        this.storyPoints = storyPoints;
        this.tasks = new ArrayList<>();
    }

    public void plan(Developer developer) {
        if (this.startTime == null) {
            this.doMainPlanning(developer);
        } else {
            this.joinPlanning(developer);
        }
    }

    private void joinPlanning(Developer developer) {
        this.additionalPlanners.add(developer);
        developer.sendTraceNote("joins planning of " + this);
        developer.passivate();
    }

    private void doMainPlanning(Developer developer) {
        this.startTime = this.presentTime();
        developer.sendTraceNote("starts planning of " + this);
        //TODO Zeit für Planung
        developer.hold(new TimeSpan(2, TimeUnit.HOURS));

        new StoryTask(this.getModel(), this);

        this.getBoard().addPlannedStory(this);

        for (final Developer helper : this.additionalPlanners) {
            helper.activate();
        }
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

    @Override
    public String getMemoryKey() {
        return this.getName();
    }

    public List<StoryTask> getTasks() {
        return this.tasks;
    }

}
