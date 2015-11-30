package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import co.paralleluniverse.fibers.SuspendExecution;
import de.unihannover.se.processSimulation.preCommitPostCommit.GraphGenerator.GraphItemFactory;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeSpan;

class Story extends RealModelEntity implements MemoryItem {

    private static enum State {
        IN_PLANNING,
        IN_IMPLEMENTATION,
        FINISHED
    }

    private final TimeSpan planningTime;
    private TimeInstant startTime;
    private State state;
    private final List<StoryTask> tasks;
    private final List<Developer> additionalPlanners = new ArrayList<>();
    private final Set<BugfixTask> openBugsBeforeFinish = new HashSet<>();

    public Story(RealProcessingModel owner, int storyPoints) {
        super(owner, "story");
        this.tasks = new ArrayList<>();
        this.planningTime = owner.getParameters().getPlanningTimeDist().sampleTimeSpan(TimeUnit.HOURS);
        this.state = State.IN_PLANNING;
    }

    public void plan(Developer developer) throws SuspendExecution {
        assert this.state == State.IN_PLANNING;
        if (this.startTime == null) {
            this.doMainPlanning(developer);
        } else {
            this.joinPlanning(developer);
        }
    }

    private void joinPlanning(Developer developer) throws SuspendExecution {
        this.additionalPlanners.add(developer);
        developer.sendTraceNote("joins planning of " + this);
        assert this.state == State.IN_PLANNING;
        developer.passivate();
    }

    private void doMainPlanning(Developer developer) throws SuspendExecution {
        this.startTime = this.presentTime();
        developer.sendTraceNote("starts planning of " + this);
        developer.hold(this.planningTime);

        this.getModel().getGraphGenerator().generateGraph(new GraphItemFactory<StoryTask>() {
            @Override
            public StoryTask createNode() {
                return new StoryTask(Story.this.getModel(), Story.this);
            }
            @Override
            public void connect(StoryTask from, StoryTask to) {
                to.addPrerequisite(from);
            }
        });

        this.state = State.IN_IMPLEMENTATION;
        this.getBoard().addPlannedStory(this);

        for (final Developer helper : this.additionalPlanners) {
            helper.activate();
        }
    }

    void addTaskHelper(StoryTask task) {
        assert task.getStory() == this;
        this.tasks.add(task);
    }

    void registerBug(BugfixTask task) {
        if (this.state != State.FINISHED) {
            this.openBugsBeforeFinish.add(task);
        }
    }

    void unregisterBug(BugfixTask task) {
        final boolean found = this.openBugsBeforeFinish.remove(task);
        assert found == (this.state != State.FINISHED);
    }

    public boolean canBeFinished() {
        for (final StoryTask t : this.tasks) {
            if (!t.isFinished()) {
                return false;
            }
        }
        //when a bug occured before the story was declared finished, it blocks finishing
        return this.openBugsBeforeFinish.isEmpty();
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

    public boolean isFinished() {
        return this.state == State.FINISHED;
    }

    public void finish() {
        assert this.state == State.IN_IMPLEMENTATION;
        assert this.openBugsBeforeFinish.isEmpty();

        this.state = State.FINISHED;
        this.getModel().countFinishedStory(this);
        for (final StoryTask t : this.getTasks()) {
            t.startLurkingBugsForCustomer();
        }
    }

}
