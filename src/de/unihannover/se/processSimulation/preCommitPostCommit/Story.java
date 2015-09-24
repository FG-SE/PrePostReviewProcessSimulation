package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

class Story extends RealModelEntity implements MemoryItem {

    private TimeInstant startTime;
    private final List<StoryTask> tasks;
    private final List<Developer> additionalPlanners = new ArrayList<>();

    public Story(RealProcessingModel owner, int storyPoints) {
        super(owner, "story");
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
        developer.hold(this.getModel().getParameters().getPlanningTimeDist().sampleTimeSpan(TimeUnit.HOURS));

        final StoryTask t1 = new StoryTask(this.getModel(), this, Collections.emptyList());
        new StoryTask(this.getModel(), this, Collections.singletonList(t1));
        new StoryTask(this.getModel(), this, Collections.singletonList(t1));

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
        assert !this.tasks.isEmpty();
        //Story-Points werden der Einfachheit halber über die Summe der Netto-Implementierungszeit bestimmt
        double totalTime = 0.0;
        for (final Task t : this.tasks) {
            totalTime += t.getImplementationTime().getTimeAsDouble(TimeUnit.HOURS);
        }
        return (int) Math.ceil(totalTime);
    }

    @Override
    public String getMemoryKey() {
        return this.getName();
    }

    public List<StoryTask> getTasks() {
        return this.tasks;
    }

}
