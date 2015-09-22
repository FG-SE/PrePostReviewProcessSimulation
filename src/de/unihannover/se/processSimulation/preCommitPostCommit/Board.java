package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.Iterator;

import desmoj.core.simulator.ProcessQueue;

public class Board {

    private final ProcessQueue<Task> tasksWithReviewRemarks;

    public Board(RealProcessingModel owner) {
        this.tasksWithReviewRemarks = new ProcessQueue<>(owner, "taskWithReviewRemarks", true, true);
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

}
