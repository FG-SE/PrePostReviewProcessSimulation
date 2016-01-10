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
 * Representation of "normal" issues, i.e. issues that are not {@link GlobalBlockerIssue}s. The used concept of "issue" is still quite
 * broad: An issue can be everything that is remarked in a review, from "real" correctness defects to performance or maintainability
 * problems. The only distinction that is made is between issues that only a developer can find (e.g. a maintainability issue) and
 * issues that customers as well as developers can find (e.g. a correctness defect).
 */
class NormalIssue extends Issue {

    public enum IssueType {
        /**
         * An issue that can only be found by developers.
         */
        DEVELOPER_ONLY,
        /**
         * An issue that can be found by developers as well as customers.
         */
        DEVELOPER_AND_CUSTOMER
    }

    private final IssueType type;

    /**
     * Creates an issue that was injected during implementation of the given task and has the given type.
     */
    public NormalIssue(Task task, IssueType type) {
        super(task, "issue");
        this.type = type;
    }

    /**
     * Determines the time it takes a developer to notice this issue from the corresponding distribution.
     */
    @Override
    protected TimeSpan getActivationTimeForDevelopers() {
        return this.getModel().getParameters().getIssueActivationTimeDeveloperDist().sampleTimeSpan(TimeUnit.HOURS);
    }

    /**
     * Determines the time it takes a customer to notice this issue from the corresponding distribution.
     * Returns null iff this issue cannot be found by customers.
     */
    @Override
    protected TimeSpan getActivationTimeForCustomers() {
        if (this.type == IssueType.DEVELOPER_AND_CUSTOMER) {
            return this.getModel().getParameters().getIssueActivationTimeCustomerDist().sampleTimeSpan(TimeUnit.HOURS);
        } else {
            return null;
        }
    }

    /**
     * When this issue becomes visible, it is put on the board for assessment.
     */
    @Override
    protected void becomeVisible(boolean byCustomer) {
        if (byCustomer) {
            this.getModel().countIssueFoundByCustomer();
        } else {
            this.getModel().dynamicCount("issueCountFoundByDevelopers");
        }
        if (this.getTask() instanceof StoryTask) {
            this.getModel().dynamicCount("occurredIssuesInStory");
        } else {
            this.getModel().dynamicCount("occurredIssuesInIssue");
        }
        this.getBoard().addUnassessedIssue(this);
    }

}
