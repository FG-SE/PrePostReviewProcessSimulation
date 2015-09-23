package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import desmoj.core.simulator.TimeInstant;

class SourceRepository {

    private final Map<Task, TimeInstant> startTimes = new LinkedHashMap<>();
    private final Map<Task, TimeInstant> commitTimes = new LinkedHashMap<>();

    public void startWork(Task task) {
        assert !this.startTimes.containsKey(task);
        this.restartWork(task);
    }

    public void restartWork(Task task) {
        assert !this.commitTimes.containsKey(task);
        this.startTimes.put(task, task.presentTime());
    }

    public boolean tryCommit(Task task) {
        final TimeInstant startTime = this.startTimes.get(task);
        for (final Entry<Task, TimeInstant> e : this.commitTimes.entrySet()) {
            //wenn der Commit nach dem Start stattgefunden hat besteht Konfliktgefahr
            if (TimeInstant.isAfter(e.getValue(), startTime)) {
                if (task.getModel().getParameters().getConflictDist().sample()) {
                    //Konflikt!
                    task.getModel().sendTraceNote("conflict between " + task + " and " + e.getKey());
                    return false;
                }
            }
        }

        //kein Konflikt => Commit möglich
        this.commitTimes.put(task, task.presentTime());
        this.startTimes.remove(task);

        this.removeUnnecessaryTasks();

        return true;
    }

    private void removeUnnecessaryTasks() {
        TimeInstant earliestStart = null;
        for (final Entry<Task, TimeInstant> e : this.startTimes.entrySet()) {
            if (earliestStart == null || TimeInstant.isBefore(e.getValue(), earliestStart)) {
                earliestStart = e.getValue();
            }
        }

        if (earliestStart == null) {
            this.commitTimes.clear();
        } else {
            final Iterator<Entry<Task, TimeInstant>> iter = this.commitTimes.entrySet().iterator();
            while (iter.hasNext()) {
                final Entry<Task, TimeInstant> e = iter.next();
                if (TimeInstant.isBefore(e.getValue(), earliestStart)) {
                    iter.remove();
                }
            }
        }
    }

}
