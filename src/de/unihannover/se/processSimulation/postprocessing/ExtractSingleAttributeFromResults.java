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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

import de.unihannover.se.processSimulation.postprocessing.ArffReader.DataHandler;

/**
 * Helper application that extracts a single attribute from an ARFF file an stores it in
 * a file named like the attribute with one value per line.
 */
public class ExtractSingleAttributeFromResults {

    public static void main(String[] args) throws IOException {
        extractAttribute(new File(args[0]), args[1]);
    }

    private static void extractAttribute(File file, String attributeName) throws IOException {
        try (Reader r = new BufferedReader(new FileReader(file));
            Writer w = new FileWriter(new File(file.getParent(), attributeName + ".txt"))) {
            ArffReader.read(r, new DataHandler() {
                @Override
                public void handleInstance(Map<String, String> instance) {
                    try {
                        w.write(instance.get(attributeName) + "\n");
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

}
