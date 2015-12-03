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

package de.unihannover.se.processSimulation.clusterControl;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Constants and functionality common to cluster control and cluster workers.
 */
class Common {

    static final String WORK_QUEUE = "workQueue";
    static final String RESULT_QUEUE = "resultQueue";
    static final String LOG_QUEUE = "logQueue";

    static final String MSG_ID = "msgId";
    static final String MSG_PROCESSOR = "msgProcessor";

    static final String SPLITTER = "\n----\n";

    static String readFileAsString(File paramsFile) throws IOException {
        final StringBuilder buffer = new StringBuilder();
        try (FileReader r = new FileReader(paramsFile)) {
            int ch;
            while ((ch = r.read()) >= 0) {
                buffer.append((char) ch);
            }
        }
        return buffer.toString();
    }

    static void writeToFile(File filename, String text) throws IOException {
        try (FileWriter w = new FileWriter(filename)) {
            w.write(text);
        }
    }

}
