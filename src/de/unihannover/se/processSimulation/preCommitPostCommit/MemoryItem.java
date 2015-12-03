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

/**
 * This interface stands for the abstract concept of "a thing that belongs to a topic that can be remembered in a developers memory".
 * It is to determine the task switch overhead, which depends on the time a developer last had to do with a certain topic.
 */
interface MemoryItem {

    /**
     * Returns a unique key for the "topic" of this item.
     */
    public abstract String getMemoryKey();

}
