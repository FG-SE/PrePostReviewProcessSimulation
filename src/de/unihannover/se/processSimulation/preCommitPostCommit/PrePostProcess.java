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

import desmoj.core.simulator.SimProcess;

/**
 * Common helper superclass for all processes in the model.
 * Contains some getters for commonly needed objects.
 */
abstract class PrePostProcess extends SimProcess {

    public PrePostProcess(PrePostModel owner, String name) {
        this(owner, name, true);
    }

    public PrePostProcess(PrePostModel owner, String name, boolean showInTrace) {
        super(owner, name, showInTrace);
    }

    protected Board getBoard() {
        return this.getModel().getBoard();
    }

    @Override
    public PrePostModel getModel() {
        return (PrePostModel) super.getModel();
    }

}
