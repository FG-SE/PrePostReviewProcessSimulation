package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import desmoj.core.simulator.TimeInstant;

class Board {

    private final RealProcessingModel model;
    private final List<NormalBug> unassessedBugs;
    private final List<StoryTask> openStoryTasks;
    private final List<BugfixTask> openBugs;
    private final Set<Task> tasksInImplementation;
    private final List<Task> tasksReadyForReview;
    private final List<Task> tasksWithReviewRemarks;
    private Story storyInPlanning;

    private int startedStoryCount;

    public Board(RealProcessingModel owner) {
        this.model = owner;
        //DESMO-J queues are slow when large, therefore use lists instead
        this.unassessedBugs = new LinkedList<>();
        this.openStoryTasks = new LinkedList<>();
        this.openBugs = new LinkedList<>();
        this.tasksInImplementation = new LinkedHashSet<>();
        this.tasksReadyForReview = new LinkedList<>();
        this.tasksWithReviewRemarks = new LinkedList<>();
    }

    public Story getStoryToPlan() {
        if (this.storyInPlanning == null) {
            this.storyInPlanning = new Story(this.model, 5);
            this.startedStoryCount++;
        }
        return this.storyInPlanning;
    }

    public void addPlannedStory(Story story) {
        assert this.storyInPlanning == story;
        this.storyInPlanning = null;
        for (final StoryTask task : story.getTasks()) {
            this.openStoryTasks.add(task);
        }
    }

    public BugfixTask getBugToFix(Developer developer) {
        if (this.openBugs.isEmpty()) {
            return null;
        }
        final BugfixTask best = determineBestFit(this.openBugs, developer);
        this.openBugs.remove(best);
        this.tasksInImplementation.add(best);
        return best;
    }

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

    public void addTaskReadyForReview(Task task) {
        this.removeTaskFromInImplementation(task);
        this.tasksReadyForReview.add(task);
    }

    public void removeTaskFromInImplementation(Task task) {
        assert this.tasksInImplementation.contains(task);
        this.tasksInImplementation.remove(task);
    }

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
            //TODO parametrisierbar machen oder weghauen
            if (iterCount > 100) {
                break;
            }
            iterCount++;
        }
        return best;
    }

    public void addTaskWithReviewRemarks(Task task) {
        this.tasksWithReviewRemarks.add(task);
    }

    public void addBugToBeFixed(BugfixTask bugfixTask) {
        this.openBugs.add(bugfixTask);
    }

    public Set<Task> getAllTasksInImplementation() {
        return this.tasksInImplementation;
    }

    public int getStartedStoryCount() {
        return this.startedStoryCount;
    }

    public void addUnassessedBug(NormalBug normalBug) {
        this.unassessedBugs.add(normalBug);
    }

    public NormalBug getUnassessedBug() {
        //hier wird nicht nach Task-Zugehörigkeit geguckt, weil man das vor dem Assessment ja nicht unbedingt wissen kann
        return this.unassessedBugs.isEmpty() ? null : this.unassessedBugs.remove(0);
    }

    public void removeTaskFromReviewQueue(Task task) {
        this.tasksReadyForReview.remove(task);
    }

    int countOpenStoryTasks() {
        return this.openStoryTasks.size();
    }

    int countOpenBugfixTasks() {
        return this.openBugs.size();
    }

    int countTasksReadyForReview() {
        return this.tasksReadyForReview.size();
    }

    int countTasksWithReviewRemarks() {
        return this.tasksWithReviewRemarks.size();
    }

}
