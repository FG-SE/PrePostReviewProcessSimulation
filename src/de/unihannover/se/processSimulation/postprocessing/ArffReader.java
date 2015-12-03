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
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ArffReader {

    public static interface DataHandler {
        public abstract void handleInstance(Map<String, String> instance);
    }

    private ArffReader() {
    }

    public static void read(Reader reader, DataHandler callback) throws IOException {
        final BufferedReader br = new BufferedReader(reader);
        String line;

        //Header lesen
        final List<String> attributeNames = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            final String lt = line.trim();
            if (lt.startsWith("@attribute ")) {
                attributeNames.add(line.split(" ")[1]);
            } else if (lt.equals("@data")) {
                break;
            }
        }

        //Daten lesen
        while ((line = br.readLine()) != null) {
            final String[] parts = line.split(",");
            if (parts.length != attributeNames.size()) {
                throw new RuntimeException("Invalid attribute count in line " + line);
            }
            final Map<String, String> map = new LinkedHashMap<>();
            for (int i = 0; i < parts.length; i++) {
                map.put(attributeNames.get(i), parts[i]);
            }
            callback.handleInstance(map);
        }
    }

}
