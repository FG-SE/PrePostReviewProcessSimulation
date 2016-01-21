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

import desmoj.core.simulator.ExternalEvent;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeSpan;

/**
 * Abstract class for all kinds of "issues/bugs" that can be injected during development. The term "issue" is used in a very broad sense in
 * this model, it can mean a correctness defect, but also things like usability or maintainability issues.
 *
 * An issue is injected into the source code during implementation. After some event (commit/deploy to customer), it can become visible to a developer
 * or customer. When it becomes visible, it has to be handled in the development process. An issue can also be "fixed", which prevents it from
 * becoming visible in the future.
 */
abstract class Issue extends PrePostEntity {

    /**
     * Event that makes an issue visible when it occurs.
     * This is modeled as event and not as process for performance reasons.
     */
    private final class IssueBecomesVisibleEvent extends ExternalEvent {

        private final boolean byCustomer;

        public IssueBecomesVisibleEvent(Model owner, String name, boolean byCustomer) {
            super(owner, name, true);
            this.byCustomer = byCustomer;
        }

        @Override
        public void eventRoutine() {
            if (!Issue.this.fixed) {
                Issue.this.becomeVisible(this.byCustomer);
            }
        }

    }

    private final Task task;
    private boolean startedForDevelopers;
    private boolean startedForCustomers;
    private boolean fixed;
    private boolean wasObserved;
    private final TimeSpan fixEffort;

    /**
     * Creates a new issue that was injected during implementation of the given task.
     */
    public Issue(Task task, String name) {
        super(task.getModel(), name);
        this.task = task;
        this.fixEffort = task.getModel().getParameters().getReviewRemarkFixDist().sampleTimeSpan(TimeUnit.HOURS);
    }

    /**
     * Has to be called as soon as the issue can become visible to developers.
     * Schedules the corresponding event (if the issue has not been already fixed).
     */
    public final void handlePublishedForDevelopers() {
        if (this.startedForDevelopers || this.fixed) {
            return;
        }
        this.startedForDevelopers = true;
        final TimeSpan t = this.getActivationTimeForDevelopers();
        if (t != null) {
            new IssueBecomesVisibleEvent(this.getModel(), this.getName(), false).schedule(t);
        }
    }

    /**
     * Has to be called as soon as the issue can become visible to customers.
     * Schedules the corresponding event (if the issue has not been already fixed).
     */
    public final void handlePublishedForCustomers() {
        if (this.startedForCustomers || this.fixed) {
            return;
        }
        this.startedForCustomers = true;
        final TimeSpan t = this.getActivationTimeForCustomers();
        if (t != null) {
            new IssueBecomesVisibleEvent(this.getModel(), this.getName(), true).schedule(t);
        }
    }

    /**
     * Needs to be implemented in subclasses to return the time until this issue becomes visible
     * to a developer.
     */
    protected abstract TimeSpan getActivationTimeForDevelopers();

    /**
     * Needs to be implemented in subclasses to return the time until this issue becomes visible
     * to a customer.
     */
    protected abstract TimeSpan getActivationTimeForCustomers();

    /**
     * Is called when the issue became visible.
     */
    protected abstract void becomeVisible(boolean byCustomer);

    /**
     * Marks this issue as fixed. A fixed issue can not become visible any more and can not be observed
     * in a review, too.
     */
    public final void fix() {
        assert this.wasObserved;
        assert !this.fixed;

        this.fixed = true;
        this.task.handleIssueFixed(this);
    }

    /**
     * Returns the task which injected this issue.
     */
    public final Task getTask() {
        return this.task;
    }

    /**
     * Return true iff this issue is fixed.
     */
    final boolean isFixed() {
        return this.fixed;
    }

    /**
     * The time/effort needed to fix this problem when it is fixed as a review remark.
     */
    public TimeSpan getFixEffort() {
        return this.fixEffort;
    }

    /**
     * Registers that this issue was observed (and assessed) by a developer.
     */
    void setWasObserved() {
        this.wasObserved = true;
    }

    /**
     * Returns true if this issue has already been observed by someone, either in review or in issue assessment.
     */
    boolean wasObserved() {
        return this.wasObserved;
    }

}
