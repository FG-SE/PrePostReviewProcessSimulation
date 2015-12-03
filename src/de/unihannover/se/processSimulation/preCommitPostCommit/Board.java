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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import desmoj.core.simulator.TimeInstant;
import desmoj.core.statistic.Count;

/**
 * Representation of an agile task board. It contains tasks in several "columns" corresponding to the
 * task's states. It also allows to get a new story from the backlog and to check if new bugs occured
 * that need to be taken care of.
 */
class Board {

    private final PrePostModel model;
    private final List<NormalBug> unassessedBugs;
    private final List<StoryTask> openStoryTasks;
    private final List<BugfixTask> openBugs;
    private final Set<Task> tasksInImplementation;
    private final List<Task> tasksReadyForReview;
    private final List<Task> tasksWithReviewRemarks;
    private Story storyInPlanning;

    private final Count startedStoryCount;

    /**
     * Creates a new board with all columns empty.
     */
    public Board(PrePostModel owner) {
        this.model = owner;
        //DESMO-J queues are slow when large, therefore use lists instead
        this.unassessedBugs = new LinkedList<>();
        this.openStoryTasks = new LinkedList<>();
        this.openBugs = new LinkedList<>();
        this.tasksInImplementation = new LinkedHashSet<>();
        this.tasksReadyForReview = new LinkedList<>();
        this.tasksWithReviewRemarks = new LinkedList<>();
        this.startedStoryCount = new Count(owner, "startedStoryCount", true, true);
    }

    /**
     * Returns the story that should be planned next (conceptually the first story in the backlog).
     * The backlog's details are not further modeled, therefore this is just a new random story.
     * Puts the returned story in the "story in planning"-column (which can contain only one story at a time).
     */
    public Story getStoryToPlan() {
        if (this.storyInPlanning == null) {
            this.storyInPlanning = new Story(this.model);
            this.startedStoryCount.update();;
        }
        return this.storyInPlanning;
    }

    /**
     * Moves the given story from the "story in planning"-column to the "story in implementation"-column.
     * All tasks of the story are added to the board in the "open tasks" column.
     */
    public void addPlannedStory(Story story) {
        assert this.storyInPlanning == story;
        this.storyInPlanning = null;
        for (final StoryTask task : story.getTasks()) {
            this.openStoryTasks.add(task);
        }
    }

    /**
     * Returns an open {@link BugfixTask} and moves it from the "open" column to the "in implementation" column.
     * If there are several open bugfix tasks, one that minimizes the task switch overhead for the given
     * developer is chosen. Returns null iff there are no open bugfix tasks.
     */
    public BugfixTask getBugToFix(Developer developer) {
        if (this.openBugs.isEmpty()) {
            return null;
        }
        final BugfixTask best = determineBestFit(this.openBugs, developer);
        this.openBugs.remove(best);
        this.tasksInImplementation.add(best);
        return best;
    }

    /**
     * Returns an open {@link StoryTask} and moves it from the "open" column to the "in implementation" column.
     * Only tasks whose prerequisite tasks have been commited are taken into account.
     * If there are several open story tasks, one that minimizes the task switch overhead for the given
     * developer is chosen. Returns null iff there are no open bugfix tasks that can be implemented.
     */
    public StoryTask getTaskToImplement(Developer developer) {
        final List<StoryTask> possibleTasks = new ArrayList<>();
        for (final StoryTask t : this.openStoryTasks) {
            if (t.arePrerequisitesGiven()) {
                possibleTasks.add(t);
            }
        }
        if (possibleTasks.isEmpty()) {
            return null;
        }
        final StoryTask best = determineBestFit(possibleTasks, developer);
        this.openStoryTasks.remove(best);
        this.tasksInImplementation.add(best);
        return best;
    }

    /**
     * Moves the given task from the "in implemenation" column to the "ready for review" column.
     */
    public void addTaskReadyForReview(Task task) {
        this.removeTaskFromInImplementation(task);
        this.tasksReadyForReview.add(task);
    }

    /**
     * Removes the given task from the "in implemenation" column from the board (conceptually: moves it to the "finished"
     * column)
     */
    public void removeTaskFromInImplementation(Task task) {
        assert this.tasksInImplementation.contains(task);
        this.tasksInImplementation.remove(task);
    }

    /**
     * Returns a "ready for review" {@link Task} and moves it from the "ready for review" column to the "in review" column
     * (the move is only conceptually, because the "in review" column is not physically modeled).
     * Only tasks that have not been implemented by the given developer are taken into account.
     * If there are several tasks to choose from, one that minimizes the task switch overhead for the given
     * developer is chosen. Returns null iff there are no tasks ready for review for that developer.
     */
    public Task getTaskToReviewFor(Developer developer) {
        final List<Task> possibleTasks = new ArrayList<>();
        for (final Task t : this.tasksReadyForReview) {
            if (!t.wasImplementedBy(developer)) {
                possibleTasks.add(t);
            }
        }
        if (possibleTasks.isEmpty()) {
            return null;
        }
        final Task best = determineBestFit(possibleTasks, developer);
        this.tasksReadyForReview.remove(best);
        return best;
    }

    /**
     * Returns a {@link Task} that has been rejected in a review and moves it from the "rejected" column to the "in implementation" column.
     * Only tasks that have been implemented by the given developer are taken into account.
     * If there are several tasks to choose from, one that minimizes the task switch overhead for the given
     * developer is chosen. Returns null iff there are no tasks with review remarks for that developer.
     */
    public Task getTaskWithReviewRemarksFor(Developer developer) {
        final List<Task> possibleTasks = new ArrayList<>();
        for (final Task t : this.tasksWithReviewRemarks) {
            if (t.wasImplementedBy(developer)) {
                possibleTasks.add(t);
            }
        }
        if (possibleTasks.isEmpty()) {
            return null;
        }
        final Task best = determineBestFit(possibleTasks, developer);
        this.tasksWithReviewRemarks.remove(best);
        this.tasksInImplementation.add(best);
        return best;
    }

    private static <T extends Task> T determineBestFit(Iterable<T> possibleTasks, Developer developer) {
        TimeInstant bestTime = null;
        T best = null;
        int iterCount = 0;
        for (final T t : possibleTasks) {
            final TimeInstant lastTimeForT = developer.getLastTimeYouHadToDoWith(t);
            if (best == null || (lastTimeForT != null && (bestTime == null || TimeInstant.isAfter(bestTime, lastTimeForT)))) {
                bestTime = lastTimeForT;
                best = t;
            }
            if (iterCount > developer.getModel().getParameters().getBoardSearchCutoffLimit()) {
                break;
            }
            iterCount++;
        }
        return best;
    }

    /**
     * Moves a task from the "in review" column to the "rejected" column (conceptually; the "in review column is not physically modeled).
     */
    public void addTaskWithReviewRemarks(Task task) {
        this.tasksWithReviewRemarks.add(task);
    }

    /**
     * Adds a new {@link BugfixTask} to the "open" column of the board.
     */
    public void addBugToBeFixed(BugfixTask bugfixTask) {
        this.openBugs.add(bugfixTask);
    }

    /**
     * Returns all tasks that are currently "in implementation".
     */
    public Set<Task> getAllTasksInImplementation() {
        return this.tasksInImplementation;
    }

    /**
     * Returns the number of stories that have been started (precisely: started planning) since the creation
     * of the board or the last reset.
     */
    public long getStartedStoryCount() {
        return this.startedStoryCount.getValue();
    }

    /**
     * Adds a newly occured bug to the list of bugs that have to be assessed.
     */
    public void addUnassessedBug(NormalBug normalBug) {
        this.unassessedBugs.add(normalBug);
    }

    /**
     * Returns a bug that needs assessment. Returns null iff there are no unassessed bugs.
     */
    public NormalBug getUnassessedBug() {
        //here the current developer is not taken into account because the topic the bug belongs to is possibly not known before assessment
        return this.unassessedBugs.isEmpty() ? null : this.unassessedBugs.remove(0);
    }

    /**
     * Removes a task from the "ready for review" column (and conceptually moves it to the "in review" column).
     */
    public void removeTaskFromReviewQueue(Task task) {
        this.tasksReadyForReview.remove(task);
    }

    /**
     * Returns the number of "open" story tasks at the moment.
     */
    int countOpenStoryTasks() {
        return this.openStoryTasks.size();
    }

    /**
     * Returns the number of "open" bugfix tasks at the moment.
     */
    int countOpenBugfixTasks() {
        return this.openBugs.size();
    }

    /**
     * Returns the number of tasks "ready for review" at the moment.
     */
    int countTasksReadyForReview() {
        return this.tasksReadyForReview.size();
    }

    /**
     * Returns the number of "rejected" tasks at the moment.
     */
    int countTasksWithReviewRemarks() {
        return this.tasksWithReviewRemarks.size();
    }

}
