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
