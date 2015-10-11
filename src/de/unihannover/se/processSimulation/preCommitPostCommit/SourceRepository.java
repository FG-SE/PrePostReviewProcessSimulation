package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import desmoj.core.simulator.TimeInstant;

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

    public void startWork(U task) {
        assert !this.startTimes.containsKey(task);
        this.restartWork(task);
    }

    public void restartWork(U task) {
        assert !this.commitTimes.containsKey(task);
        this.startTimes.put(task, this.deps.presentTime());
    }

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
