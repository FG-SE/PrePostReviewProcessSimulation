package de.unihannover.se.processSimulation.clusterControl;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Common {

    public static final String WORK_QUEUE = "workQueue";
    public static final String RESULT_QUEUE = "resultQueue";
    public static final String LOG_QUEUE = "logQueue";

    public static final String MSG_ID = "msgId";
    public static final String MSG_PROCESSOR = "msgProcessor";

    public static final String SPLITTER = "\n----\n";

    public static String readFileAsString(File paramsFile) throws IOException {
        final StringBuilder buffer = new StringBuilder();
        try (FileReader r = new FileReader(paramsFile)) {
            int ch;
            while ((ch = r.read()) >= 0) {
                buffer.append((char) ch);
            }
        }
        return buffer.toString();
    }

    public static void writeToFile(File filename, String text) throws IOException {
        try (FileWriter w = new FileWriter(filename)) {
            w.write(text);
        }
    }

}
