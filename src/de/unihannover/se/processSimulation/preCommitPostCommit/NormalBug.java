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
 * Representation of "normal" bugs, i.e. bugs that are not {@link GlobalBlockerBug}s. The used concept of "bug" is still quite
 * broad: A bug can be everything that is remarked in a review, from "real" correctness defects to performance or maintainability
 * problems. The only distinction that is made is between bugs that only a developer can find (e.g. a maintainability issue) and
 * bugs that customers as well as developers can find.
 */
class NormalBug extends Bug {

    public enum BugType {
        /**
         * A bug that can only be found by developers.
         */
        DEVELOPER_ONLY,
        /**
         * A bug that can be found by developers as well as customers.
         */
        DEVELOPER_AND_CUSTOMER
    }

    private final BugType type;

    /**
     * Creates a bug that was injected during implementation of the given task and has the given type.
     */
    public NormalBug(Task task, BugType type) {
        super(task, "bug");
        this.type = type;
    }

    /**
     * Determines the time it takes a developer to notice this bug from the corresponding distribution.
     */
    @Override
    protected TimeSpan getActivationTimeForDevelopers() {
        return this.getModel().getParameters().getBugActivationTimeDeveloperDist().sampleTimeSpan(TimeUnit.HOURS);
    }

    /**
     * Determines the time it takes a customer to notice this bug from the corresponding distribution.
     * Returns null iff this bug cannot be found by customers.
     */
    @Override
    protected TimeSpan getActivationTimeForCustomers() {
        if (this.type == BugType.DEVELOPER_AND_CUSTOMER) {
            return this.getModel().getParameters().getBugActivationTimeCustomerDist().sampleTimeSpan(TimeUnit.HOURS);
        } else {
            return null;
        }
    }

    /**
     * When this bug becomes visible, it is put on the board for assessment.
     */
    @Override
    protected void becomeVisible(boolean byCustomer) {
        if (byCustomer) {
            this.getModel().countBugFoundByCustomer();
        } else {
            this.getModel().dynamicCount("bugCountFoundByDevelopers");
        }
        if (this.getTask() instanceof StoryTask) {
            this.getModel().dynamicCount("occurredBugsInStory");
        } else {
            this.getModel().dynamicCount("occurredBugsInBug");
        }
        this.getBoard().addUnassessedBug(this);
    }

}
