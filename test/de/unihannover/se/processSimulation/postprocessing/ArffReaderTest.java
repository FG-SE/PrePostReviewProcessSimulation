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

package de.unihannover.se.processSimulation.postprocessing;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.Map;

import org.junit.Test;

import de.unihannover.se.processSimulation.postprocessing.ArffReader.DataHandler;

public class ArffReaderTest {

    private static final class TestCallback implements DataHandler {

        private final StringBuilder results = new StringBuilder();

        @Override
        public void handleInstance(Map<String, String> instance) {
            this.results.append(instance).append('\n');
        }

        public String getResults() {
            return this.results.toString();
        }

    }

    private static TestCallback runTest(String s) throws Exception {
        final TestCallback callback = new TestCallback();
        ArffReader.read(new StringReader(s), callback);
        return callback;
    }

    @Test
    public void testReadSimple() throws Exception {
        final TestCallback callback = runTest("@relation sensitivityAnalysis\n"
            + "\n"
            + "@attribute implementationSkillMode numeric\n"
            + "@attribute reviewSkillMode numeric\n"
            + "@attribute dependencyGraphConstellation {SIMPLISTIC,REALISTIC,NO_DEPENDENCIES,CHAINS,DIAMONDS}\n"
            + "\n"
            + "@data\n"
            + "0.9,0.5,REALISTIC\n"
            + "0.9,0.5,REALISTIC");
        assertEquals("{implementationSkillMode=0.9, reviewSkillMode=0.5, dependencyGraphConstellation=REALISTIC}\n"
                    + "{implementationSkillMode=0.9, reviewSkillMode=0.5, dependencyGraphConstellation=REALISTIC}\n",
                    callback.getResults());
    }

}
