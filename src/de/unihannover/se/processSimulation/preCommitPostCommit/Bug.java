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

import desmoj.core.simulator.ExternalEvent;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeSpan;

abstract class Bug extends RealModelEntity {

    private final class BugBecomesVisibleEvent extends ExternalEvent {

        private final boolean byCustomer;

        public BugBecomesVisibleEvent(Model owner, String name, boolean byCustomer) {
            super(owner, name, true);
            this.byCustomer = byCustomer;
        }

        @Override
        public void eventRoutine() {
            if (!Bug.this.fixed) {
                Bug.this.becomeVisible(this.byCustomer);
            }
        }

    }

    private final Task task;
    private boolean startedForDevelopers;
    private boolean startedForCustomers;
    private boolean fixed;

    public Bug(Task task, String name) {
        super(task.getModel(), name);
        this.task = task;
    }

    public final void handlePublishedForDevelopers() {
        if (this.startedForDevelopers || this.fixed) {
            return;
        }
        this.startedForDevelopers = true;
        final TimeSpan t = this.getActivationTimeForDevelopers();
        if (t != null) {
            new BugBecomesVisibleEvent(this.getModel(), this.getName(), false).schedule(t);
        }
    }

    public final void handlePublishedForCustomers() {
        if (this.startedForCustomers || this.fixed) {
            return;
        }
        this.startedForCustomers = true;
        final TimeSpan t = this.getActivationTimeForCustomers();
        if (t != null) {
            new BugBecomesVisibleEvent(this.getModel(), this.getName(), true).schedule(t);
        }
    }

    protected abstract TimeSpan getActivationTimeForDevelopers();
    protected abstract TimeSpan getActivationTimeForCustomers();

    protected abstract void becomeVisible(boolean byCustomer);

    public final void fix() {
        if (!this.fixed) {
            this.fixed = true;
            this.task.handleBugFixed(this);
        }
    }

    public Task getTask() {
        return this.task;
    }

    boolean isFixed() {
        return this.fixed;
    }

}
