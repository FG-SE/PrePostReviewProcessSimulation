/**
    This file is part of LUH PrePostReview Process Simulation.

    LUH PrePostReview Process Simulation is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    LUH PrePostReview Process Simulation is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with LUH PrePostReview Process Simulation. If not, see <http://www.gnu.org/licenses/>.
 */

package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import co.paralleluniverse.fibers.SuspendExecution;
import de.unihannover.se.processSimulation.preCommitPostCommit.GraphGenerator.GraphItemFactory;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

/**
 * Represenation of an user story. A story is comprised of several tasks. A story has a simple lifecycle (planning -> implementation -> finished).
 * It is deployed to the customer as a whole and as soon as it is finished.
 */
class Story extends PrePostEntity implements MemoryItem {

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
    private Set<BugfixTask> bugsBeforeFinish = new HashSet<>();

    /**
     * Creates a new story in state "in planning", with a random planning time.
     */
    public Story(PrePostModel owner) {
        super(owner, "story");
        this.tasks = new ArrayList<>();
        this.planningTime = owner.getParameters().getPlanningTimeDist().sampleTimeSpan(TimeUnit.HOURS);
        this.state = State.IN_PLANNING;
    }

    /**
     * Performs planning of this story with the given developer. If there is already someone planning this story,
     * the developers joins planning, otherwise he becomes responsible for planning. This method holds until
     * planning is finished.
     */
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

    /**
     * Perform planning of the story: Hold for the planning time, create the tasks for the story
     * and change the stories state to "im implementation".
     */
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

    /**
     * Helper method for the bidirectional association between story and task.
     */
    void addTaskHelper(StoryTask task) {
        assert task.getStory() == this;
        this.tasks.add(task);
    }

    /**
     * Is called when a bug in this story has been identified. When this happens
     * before the story is finished, it keeps the story from finishing until the bugfix
     * task is finished.
     */
    void registerBug(BugfixTask task) {
        if (this.state != State.FINISHED) {
            this.bugsBeforeFinish.add(task);
        }
    }

    /**
     * Returns true iff this story can be finished, i.e. every task and every known bug is finished.
     * @pre this.state != State.FINISHED
     */
    public boolean canBeFinished() {
        for (final StoryTask t : this.tasks) {
            if (!t.isFinished()) {
                return false;
            }
        }
        //when a bug occured before the story was declared finished, it blocks finishing
        for (final BugfixTask t : this.bugsBeforeFinish) {
            if (!t.isFinished()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the time elapsed between start and finish of this story.
     */
    public TimeSpan getCycleTime(TimeInstant finishTime) {
        return TimeOperations.diff(finishTime, this.startTime);
    }

    /**
     * Returns the "story points" of this story. Because there is no real notion of "value" or "size" in this
     * model, the number of story points is simply the number of hours the implemenentation would take without
     * any waste, i.e. the sum of the planning time and all implementation times.
     * @pre The story has been planned.
     */
    public int getStoryPoints() {
        assert !this.tasks.isEmpty();
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

    /**
     * Returns all tasks belonging to this story.
     * @pre The story has been planned.
     */
    public List<StoryTask> getTasks() {
        assert !this.tasks.isEmpty();
        return this.tasks;
    }

    /**
     * Returns true iff this story is finished.
     */
    public boolean isFinished() {
        return this.state == State.FINISHED;
    }

    /**
     * Finishes this story (conceptually includes delivery to the customer).
     * Changes its state, updates the statistics and notifies all contained bugs that they can now be found by customers.
     */
    public void finish() {
        assert this.state == State.IN_IMPLEMENTATION;

        this.state = State.FINISHED;
        this.getModel().countFinishedStory(this);
        for (final StoryTask t : this.getTasks()) {
            t.startLurkingBugsForCustomer();
        }
        for (final BugfixTask t : this.bugsBeforeFinish) {
            t.startLurkingBugsForCustomer();
        }
        this.bugsBeforeFinish = null;
    }

}
