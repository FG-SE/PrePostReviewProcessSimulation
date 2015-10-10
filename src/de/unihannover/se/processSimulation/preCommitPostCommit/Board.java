package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import desmoj.core.simulator.Queue;
import desmoj.core.simulator.TimeInstant;

class Board {

    private final RealProcessingModel model;
    private final Queue<NormalBug> unassessedBugs;
    private final Queue<StoryTask> openStoryTasks;
    private final Queue<BugfixTask> openBugs;
    private final Set<Task> tasksInImplementation;
    private final Queue<Task> tasksReadyForReview;
    private final Queue<Task> tasksWithReviewRemarks;
    private Story storyInPlanning;

    private int startedStoryCount;

    public Board(RealProcessingModel owner) {
        this.model = owner;
        this.unassessedBugs = new Queue<>(owner, "unassessedBugs", true, true);
        this.openStoryTasks = new Queue<>(owner, "openStoryTasks", true, true);
        this.openBugs = new Queue<>(owner, "openBugs", true, true);
        this.tasksInImplementation = new LinkedHashSet<>();
        this.tasksReadyForReview = new Queue<>(owner, "tasksReadyForReview", true, true);
        this.tasksWithReviewRemarks = new Queue<>(owner, "taskWithReviewRemarks", true, true);
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
            this.openStoryTasks.insert(task);
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
        assert this.tasksInImplementation.contains(task);
        this.tasksInImplementation.remove(task);
        this.tasksReadyForReview.insert(task);
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
        for (final T t : possibleTasks) {
            final TimeInstant lastTimeForT = developer.getLastTimeYouHadToDoWith(t);
            if (best == null || (lastTimeForT != null && (bestTime == null || TimeInstant.isAfter(bestTime, lastTimeForT)))) {
                bestTime = lastTimeForT;
                best = t;
            }
        }
        return best;
    }

    public void addTaskWithReviewRemarks(Task task) {
        this.tasksWithReviewRemarks.insert(task);
    }

    public void addFinishedTask(Task task) {
        if (task instanceof StoryTask) {
            final Story story = ((StoryTask) task).getStory();
            if (story.allTasksFinished()) {
                this.model.countFinishedStory(story);
            }
        }
    }

    public void addBugToBeFixed(BugfixTask bugfixTask) {
        this.openBugs.insert(bugfixTask);
    }

    public Set<Task> getAllTasksInImplementation() {
        return this.tasksInImplementation;
    }

    public int getStartedStoryCount() {
        return this.startedStoryCount;
    }

    public void addUnassessedBug(NormalBug normalBug) {
        this.unassessedBugs.insert(normalBug);
    }

    public NormalBug getUnassessedBug() {
        //hier wird nicht nach Task-Zugehörigkeit geguckt, weil man das vor dem Assessment ja nicht unbedingt wissen kann
        return this.unassessedBugs.removeFirst();
    }

}
