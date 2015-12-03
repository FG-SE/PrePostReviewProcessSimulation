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

package de.unihannover.se.processSimulation.dataGenerator;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

public interface DataWriter extends Closeable {

    public abstract void addNumericAttribute(String name) throws IOException;

    public abstract void addNominalAttribute(String name, Object[] enumConstants) throws IOException;

    public abstract void writeTuple(Map<String, Object> experimentData) throws IOException;

    public abstract void flush() throws IOException;

}
