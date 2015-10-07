package de.unihannover.se.processSimulation.dataGenerator;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

public interface DataWriter extends Closeable {

    public abstract void addNumericAttribute(String name) throws IOException;

    public abstract void writeTuple(Map<String, Object> experimentData) throws IOException;

    public abstract void flush() throws IOException;

    @Override
    public abstract void close() throws IOException;

}
