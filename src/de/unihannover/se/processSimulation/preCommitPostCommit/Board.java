package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.Iterator;

import desmoj.core.simulator.Queue;

class Board {

    private final Queue<StoryTask> openStoryTasks;
    private final Queue<BugfixTask> openBugs;
    private final Queue<Task> tasksReadyForReview;
    private final Queue<Task> tasksWithReviewRemarks;
    private final RealProcessingModel model;

    public Board(RealProcessingModel owner) {
        this.model = owner;
        this.openStoryTasks = new Queue<>(owner, "openStoryTasks", true, true);
        this.openBugs = new Queue<>(owner, "openBugs", true, true);
        this.tasksReadyForReview = new Queue<>(owner, "tasksReadyForReview", true, true);
        this.tasksWithReviewRemarks = new Queue<>(owner, "taskWithReviewRemarks", true, true);
    }

    public Story getStoryToPlan() {
        return new Story(this.model, 5);
    }

    public void addTaskToBeImplemented(StoryTask task) {
        this.openStoryTasks.insert(task);
    }

    public BugfixTask getBugToFix(Developer developer) {
        return this.openBugs.removeFirst();
    }

    public StoryTask getTaskToImplement(Developer developer) {
        //TODO einen Task auswählen, der zum zuletzte bearbeiteten Thema passt
        return this.openStoryTasks.removeFirst();
    }

    public void addTaskReadyForReview(Task task) {
        this.tasksReadyForReview.insert(task);
    }

    public Task getTaskToReviewFor(Developer developer) {
        final Iterator<Task> iter = this.tasksReadyForReview.iterator();
        while(iter.hasNext()) {
            final Task t = iter.next();
            if (!t.wasImplementedBy(developer)) {
                iter.remove();
                return t;
            }
        }
        return null;
    }

    public Task getTaskWithReviewRemarksFor(Developer developer) {
        final Iterator<Task> iter = this.tasksWithReviewRemarks.iterator();
        while(iter.hasNext()) {
            final Task t = iter.next();
            if (t.wasImplementedBy(developer)) {
                iter.remove();
                return t;
            }
        }
        return null;
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
