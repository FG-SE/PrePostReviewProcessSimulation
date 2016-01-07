package de.unihannover.se.processSimulation.postprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Adds a "total summary" column to the data, combining the results for story points, bugs and cycle time.
 * The "@attribute" entry has to be added by hand.
 */
public class AddTotalSummary {

    public static void main(String[] args) throws Exception {
        final File input = new File(args[0]);
        final File output = new File(args[1]);

        try (BufferedReader r = new BufferedReader(new FileReader(input))) {
            try (FileWriter w = new FileWriter(output)) {
                String line;
                boolean inData = false;
                while ((line = r.readLine()) != null) {
                    if (line.equals("@data")) {
                        inData = true;
                        w.append(line).append('\n');
                    } else if (inData) {
                        w.append(line + "," + determineTotalSummary(line)).append('\n');
                    } else {
                        w.append(line).append('\n');
                    }
                }
            }
        }
    }

    private static String determineTotalSummary(String line) {
        if (line.contains(",UNREALISTIC,")) {
            return "UNREALISTIC";
        } else if (line.contains(",NO_REVIEW,")) {
            return "NO_REVIEW";
        } else if (line.contains(",NOT_SIGNIFICANT,")) {
            return "NOT_SIGNIFICANT";
        } else {
            final boolean pre = line.contains(",PRE_BETTER,");
            final boolean post = line.contains(",POST_BETTER,");
            if (pre && post) {
                return "CONFLICT";
            } else if (pre) {
                return "PRE_BETTER";
            } else if (post) {
                return "POST_BETTER";
            } else {
                return "NEGLIGIBLE_DIFFERENCE";
            }
        }
    }

}
