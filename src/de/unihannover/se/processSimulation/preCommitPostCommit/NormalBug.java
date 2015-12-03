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

class NormalBug extends Bug {

    public enum BugType {
        DEVELOPER_ONLY,
        DEVELOPER_AND_CUSTOMER
    }

    private final BugType type;

    public NormalBug(Task task, BugType type) {
        super(task, "bug");
        this.type = type;
    }

    @Override
    protected TimeSpan getActivationTimeForDevelopers() {
        return this.getModel().getParameters().getBugActivationTimeDeveloperDist().sampleTimeSpan(TimeUnit.HOURS);
    }

    @Override
    protected TimeSpan getActivationTimeForCustomers() {
        if (this.type == BugType.DEVELOPER_AND_CUSTOMER) {
            return this.getModel().getParameters().getBugActivationTimeCustomerDist().sampleTimeSpan(TimeUnit.HOURS);
        } else {
            return null;
        }
    }

    @Override
    protected void becomeVisible(boolean byCustomer) {
        if (byCustomer) {
            this.getModel().countBugFoundByCustomer();
        }
        this.getBoard().addUnassessedBug(this);
    }

}
