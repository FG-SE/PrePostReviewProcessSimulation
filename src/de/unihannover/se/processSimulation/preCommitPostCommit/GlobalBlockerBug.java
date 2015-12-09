/**
    This file is part of LUH PrePostReview Process Simulation.

    LUH PrePostReview Process Simulation is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    LUH PrePostReview Process Simulation is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with LUH PrePostReview Process Simulation. If not, see <http://www.gnu.org/licenses/>.
 */

package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.TimeSpan;

/**
 * A "global blocker bug" is a certain kind of bug that occurs almost immediately after commit and that impedes
 * all currently implementing developers (think: someone broke the build badly).
 */
class GlobalBlockerBug extends Bug {

    public GlobalBlockerBug(Task task) {
        super(task, "global-bug");
    }

    /**
     * Occurs almost immediately after commit.
     */
    @Override
    protected TimeSpan getActivationTimeForDevelopers() {
        return new TimeSpan(1, TimeUnit.MINUTES);
    }

    /**
     * Cannot be observed by customers.
     */
    @Override
    protected TimeSpan getActivationTimeForCustomers() {
        return null;
    }

    /**
     * When it becomes visible, all currently active implementation tasks are delayed for a certain time span.
     * It is then fixed automatically (i.e. some waiting developer fixes it).
     */
    @Override
    protected void becomeVisible(boolean byCustomer) {
        assert !byCustomer;
        this.getModel().dynamicCount("occurredGlobalBugs");
        //TODO: wenn gerade nichts im Implementierung ist wird das Problem gelöst, ohne dass es Zeit gekostet hat. Das ist unrealistisch. Ist das schlimm?
        for (final Task t : this.getBoard().getAllTasksInImplementation()) {
            t.suspendImplementation(this.getModel().getParameters().getGlobalBugSuspendTimeDist().sampleTimeSpan(TimeUnit.HOURS));
        }
        this.fix();
    }

}
