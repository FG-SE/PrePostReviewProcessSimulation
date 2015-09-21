package de.tntinteractive.processSimulation.preCommitPostCommitPrototype;

import desmoj.core.simulator.ProcessQueue;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;
import desmoj.core.statistic.Count;
import desmoj.core.statistic.Tally;

public class Board {

    private final DevelopmentProcessModel model;

    private final ProcessQueue<Story> storiesInPlanning;
    private final ProcessQueue<StoryTask> openTasks;
    private final ProcessQueue<BugfixTask> openBugs;
    private final ProcessQueue<Task> reviewRemarkTasks;
    private final ProcessQueue<Task> reviewableTasks;

    private final Count completedWorkStatistic;
    private final Tally storyCycleTimeStatistic;

    public Board(DevelopmentProcessModel model) {
        this.model = model;

        this.storiesInPlanning = new ProcessQueue<>(model, "Stories in planning", true, true);
        this.openTasks = new ProcessQueue<>(model, "Open tasks", true, true);
        this.openBugs = new ProcessQueue<>(model, "Open bugs", true, true);
        this.reviewableTasks = new ProcessQueue<>(model, "Reviewable tasks", true, true);
        this.reviewRemarkTasks = new ProcessQueue<>(model, "Tasks with review remarks", true, true);

        this.completedWorkStatistic = new Count(model, "completed work", true, true);
        this.completedWorkStatistic.setUnit("story points (ideal hours)");
        this.storyCycleTimeStatistic = new Tally(model, "Story cycle time", true, true);
    }

    public Story getStoryToPlan() {
        final Story s = new Story(this.model, "Story");
        this.storiesInPlanning.insert(s);
        return s;
    }

    public Story getStoryToJoinPlanning() {
        //TODO realistischer machen?
        return this.storiesInPlanning.first();
    }

    public void addPlannedStory(Story story) {
        this.storiesInPlanning.remove(story);
        for (final StoryTask t : story.getTasks()) {
            this.openTasks.insert(t);
        }
    }

    public void addBugfixTask(Bug bug) {
        this.openBugs.insert(new BugfixTask(this.model));
    }

    public Task getBugToFix() {
        return this.openBugs.removeFirst();
    }

    public Task getOpenTaskWithSatisfiedPreconditionsFromStory() {
        for (final Task t : this.openTasks) {
            if (t.prerequisitesImplemented()) {
                this.openTasks.remove(t);
                return t;
            }
            this.model.current().sendTraceNote(" skips " + t + " because not all prerequisites implemented");
        }
        return null;
    }

    public void addImplementedTask(Task task) {
        this.reviewableTasks.insert(task);
    }

    public void addFinishedTask(Task task) {
        // TODO Auto-generated method stub

        if (task instanceof StoryTask) {
            this.checkAndHandleFinishedStory((StoryTask) task);
        }
    }

    private void checkAndHandleFinishedStory(StoryTask task) {
        if (task.getStory().allTasksFinished()) {
            final TimeSpan timeBetween = timeBetween(this.model.presentTime(), task.getStory().getStartTime());
            this.storyCycleTimeStatistic.update(timeBetween);
            this.completedWorkStatistic.update(task.getStory().getStoryPoints());
        }
    }

    private static TimeSpan timeBetween(TimeInstant presentTime, TimeInstant startTime) {
        return new TimeSpan(presentTime.getTimeInEpsilon() - startTime.getTimeInEpsilon(), TimeOperations.getEpsilon());
    }

    public Task getTaskToReview(Developer developer) {
        for (final Task t : this.reviewableTasks) {
            if (t.getImplementer() != developer) {
                this.reviewableTasks.remove(t);
                return t;
            }
        }
        return null;
    }

    public void addTaskWithReviewRemarks(Task task) {
        this.reviewRemarkTasks.insert(task);
    }

    public Task getTaskWithReviewRemarksToFixFor(Developer developer) {
        for (final Task t : this.reviewRemarkTasks) {
            if (t.getImplementer() == developer) {
                this.reviewRemarkTasks.remove(t);
                return t;
            }
        }
        return null;
    }

}
