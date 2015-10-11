package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import desmoj.core.simulator.TimeInstant;

/**
 * Simulates a source code repository (like Subversion or Git): A developer starts
 * working on a task (and updates his local working copy). Later he integrates/commits
 * his changes to the common development line. When doing so, he can have a conflict with
 * other changes that were commited between his last update and the current (commit) time.
 * The normal reaction to such a conflict is to update the local working copy, resolve the
 * conflicts and try to commit again.
 *
 * This class assumes that there is at one-to-one relationship between local working copies
 * and currently active tasks (e.g. every developer works on at most one task at a time or
 * he uses local feature branches).
 *
 * @param <U> Type of the tasks (can be changed for unit tests).
 */
class SourceRepository<U> {

    public static interface SourceRepositoryDependencies {
        public abstract TimeInstant presentTime();

        public abstract boolean sampleConflictDist();

        public abstract void sendTraceNote(String description);
    }

    private final SourceRepositoryDependencies deps;
    private final Map<U, TimeInstant> startTimes = new LinkedHashMap<>();
    private final Map<U, TimeInstant> commitTimes = new LinkedHashMap<>();

    public SourceRepository(SourceRepositoryDependencies deps) {
        this.deps = deps;
    }

    /**
     * Start working on a task: Update the local working copy.
     * @pre There is no open working copy for this task yet.
     */
    public void startWork(U task) {
        assert !this.startTimes.containsKey(task);
        this.updateForTask(task);
    }

    /**
     * Restart working on a task after a failed commit: Update the local working copy again and solve conflicts.
     */
    public void restartWork(U task) {
        assert this.startTimes.containsKey(task);
        this.updateForTask(task);
    }

    private void updateForTask(U task) {
        this.startTimes.put(task, this.deps.presentTime());
    }

    /**
     * Try to commit. When another task was commited since the last update (start/restart) of the given
     * task, there is a certain propability of conflict. When a conflict occurs, this method does not
     * commit and returns false. Otherwise it commits and returns true.
     */
    public boolean tryCommit(U task) {
        final TimeInstant startTime = this.startTimes.get(task);
        for (final Entry<U, TimeInstant> e : this.commitTimes.entrySet()) {
            //wenn der Commit nach dem Start stattgefunden hat besteht Konfliktgefahr
            if (TimeInstant.isAfter(e.getValue(), startTime)) {
                if (this.deps.sampleConflictDist()) {
                    //Konflikt!
                    this.deps.sendTraceNote("conflict between " + task + " and " + e.getKey());
                    return false;
                }
            }
        }

        //kein Konflikt => Commit möglich
        this.commitTimes.put(task, this.deps.presentTime());
        this.startTimes.remove(task);

        this.removeUnnecessaryTasks();

        return true;
    }

    private void removeUnnecessaryTasks() {
        TimeInstant earliestStart = null;
        for (final Entry<U, TimeInstant> e : this.startTimes.entrySet()) {
            if (earliestStart == null || TimeInstant.isBefore(e.getValue(), earliestStart)) {
                earliestStart = e.getValue();
            }
        }

        if (earliestStart == null) {
            this.commitTimes.clear();
        } else {
            final Iterator<Entry<U, TimeInstant>> iter = this.commitTimes.entrySet().iterator();
            while (iter.hasNext()) {
                final Entry<U, TimeInstant> e = iter.next();
                if (TimeInstant.isBefore(e.getValue(), earliestStart)) {
                    iter.remove();
                }
            }
        }
    }

    int countRemainingSavedCommits() {
        return this.commitTimes.size();
    }

}
