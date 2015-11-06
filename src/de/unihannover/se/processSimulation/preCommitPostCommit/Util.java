package de.unihannover.se.processSimulation.preCommitPostCommit;

import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;

class Util {

    private Util() {
    }

    static TimeSpan timeBetween(TimeInstant start, TimeInstant end) {
        return new TimeSpan(end.getTimeInEpsilon() - start.getTimeInEpsilon(), TimeOperations.getEpsilon());
    }

}
