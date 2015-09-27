package de.unihannover.se.processSimulation.dataGenerator;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArffWriter implements Closeable {

    private final Writer writer;
    private boolean dataStarted;

    private final List<String> attributes = new ArrayList<>();

    public ArffWriter(Writer writer, String relationName) throws IOException {
        this.writer = writer;
        this.writer.write("@relation " + relationName + "\n");
        this.writer.write("\n");
    }

    @Override
    public void close() throws IOException {
        this.writer.close();
    }

    public void addNumericAttribute(String name) throws IOException {
        assert !this.dataStarted;
        this.writer.write("@attribute " + name + " numeric\n");
        this.attributes.add(name);
    }

    public void writeTuple(Map<String, Object> experimentData) throws IOException {
        if (!this.dataStarted) {
            this.dataStarted = true;
            this.writer.write("\n");
            this.writer.write("@data\n");
        }

        boolean first = true;
        for (final String att : this.attributes) {
            if (first) {
                first = false;
            } else {
                this.writer.write(',');
            }
            this.writer.write(experimentData.get(att).toString());
        }
        this.writer.write('\n');
    }

}
