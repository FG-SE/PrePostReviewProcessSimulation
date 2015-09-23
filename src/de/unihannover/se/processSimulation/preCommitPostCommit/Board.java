package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.List;

import desmoj.core.simulator.Queue;
import desmoj.core.simulator.TimeInstant;

class Board {

    private final RealProcessingModel model;
    private final Queue<StoryTask> openStoryTasks;
    private final Queue<BugfixTask> openBugs;
    private final Queue<Task> tasksReadyForReview;
    private final Queue<Task> tasksWithReviewRemarks;
    private Story storyInPlanning;

    public Board(RealProcessingModel owner) {
        this.model = owner;
        this.openStoryTasks = new Queue<>(owner, "openStoryTasks", true, true);
        this.openBugs = new Queue<>(owner, "openBugs", true, true);
        this.tasksReadyForReview = new Queue<>(owner, "tasksReadyForReview", true, true);
        this.tasksWithReviewRemarks = new Queue<>(owner, "taskWithReviewRemarks", true, true);
    }

    public Story getStoryToPlan() {
        if (this.storyInPlanning == null) {
            this.storyInPlanning = new Story(this.model, 5);
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
        return best;
    }

    public StoryTask getTaskToImplement(Developer developer) {
        //TODO: Vorbedingungs-Tasks beachten
        if (this.openStoryTasks.isEmpty()) {
            return null;
        }
        final StoryTask best = determineBestFit(this.openStoryTasks, developer);
        this.openStoryTasks.remove(best);
        return best;
    }

    public void addTaskReadyForReview(Task task) {
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
        return best;
    }

    private static <T extends Task> T determineBestFit(Iterable<T> possibleTasks, Developer developer) {
        TimeInstant bestTime = null;
        T best = null;
        for (final T t : possibleTasks) {
            final TimeInstant lastTimeForT = developer.getLastTimeYouHadToDoWith(t);
            if (bestTime == null || TimeInstant.isAfter(bestTime, lastTimeForT)) {
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

}
