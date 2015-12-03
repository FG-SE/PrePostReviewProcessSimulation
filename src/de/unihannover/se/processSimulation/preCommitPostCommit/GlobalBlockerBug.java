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

class GlobalBlockerBug extends Bug {

    public GlobalBlockerBug(Task task) {
        super(task, "global-bug");
    }

    @Override
    protected TimeSpan getActivationTimeForDevelopers() {
        return new TimeSpan(1, TimeUnit.MINUTES);
    }

    @Override
    protected TimeSpan getActivationTimeForCustomers() {
        return null;
    }

    @Override
    protected void becomeVisible(boolean byCustomer) {
        assert !byCustomer;
        for (final Task t : this.getBoard().getAllTasksInImplementation()) {
            t.suspendImplementation(this.getModel().getParameters().getGlobalBugSuspendTimeDist().sampleTimeSpan(TimeUnit.HOURS));
        }
        this.fix();
    }

}
