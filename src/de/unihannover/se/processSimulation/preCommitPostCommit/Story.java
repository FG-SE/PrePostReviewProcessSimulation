package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import co.paralleluniverse.fibers.SuspendExecution;
import de.unihannover.se.processSimulation.preCommitPostCommit.GraphGenerator.GraphItemFactory;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeSpan;

class Story extends RealModelEntity implements MemoryItem {

    private final TimeSpan planningTime;
    private TimeInstant startTime;
    private boolean planned;
    private final List<StoryTask> tasks;
    private final List<Developer> additionalPlanners = new ArrayList<>();
    private final Randomness randomness;

    public Story(RealProcessingModel owner, Randomness randomness) {
        super(owner, "story");
        this.tasks = new ArrayList<>();
        this.planningTime = randomness.sampleTimeSpan(owner.getParameters().getPlanningTimeDist());
        this.randomness = randomness;
    }

    public void plan(Developer developer) throws SuspendExecution {
        assert !this.planned;
        if (this.startTime == null) {
            this.doMainPlanning(developer);
        } else {
            this.joinPlanning(developer);
        }
    }

    private void joinPlanning(Developer developer) throws SuspendExecution {
        this.additionalPlanners.add(developer);
        developer.sendTraceNote("joins planning of " + this);
        assert !this.planned;
        developer.passivate();
    }

    private void doMainPlanning(Developer developer) throws SuspendExecution {
        this.startTime = this.presentTime();
        developer.sendTraceNote("starts planning of " + this);
        developer.hold(this.planningTime);

        this.getModel().getGraphGenerator().generateGraph(new GraphItemFactory<StoryTask>() {
            @Override
            public StoryTask createNode() {
                return new StoryTask(Story.this.getModel(), Story.this, Story.this.randomness.forkRandomNumberStream());
            }
            @Override
            public void connect(StoryTask from, StoryTask to) {
                to.addPrerequisite(from);
            }
        });

        this.planned = true;
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
        return Util.timeBetween(this.startTime, finishTime);
    }

    public int getStoryPoints() {
        assert !this.tasks.isEmpty();
        //Story-Points werden der Einfachheit halber über die Summe der Netto-Implementierungszeit bestimmt
        double totalTime = this.planningTime.getTimeAsDouble(TimeUnit.HOURS);
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

    public void finish() {
        this.getModel().countFinishedStory(this);
        for (final StoryTask t : this.getTasks()) {
            t.startLurkingBugsForCustomer();
        }
    }

}
