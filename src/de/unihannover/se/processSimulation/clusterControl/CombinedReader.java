package de.unihannover.se.processSimulation.clusterControl;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;

public class CombinedReader extends Reader {

    private Reader currentReader;
    private final Iterator<Path> furtherFiles;

    public CombinedReader(FileReader firstReader, Stream<Path> furtherFiles) {
        this.currentReader = firstReader;
        this.furtherFiles = furtherFiles.iterator();
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (this.currentReader == null) {
            return -1;
        }
        final int readFromFile = this.currentReader.read(cbuf, off, len);
        if (readFromFile < 0) {
            if (this.furtherFiles.hasNext()) {
                this.currentReader.close();
                this.currentReader = new FileReader(this.furtherFiles.next().toFile());
                return this.read(cbuf, off, len);
            } else {
                this.currentReader = null;
                return -1;
            }
        }
        return readFromFile;
    }

    @Override
    public void close() throws IOException {
        if (this.currentReader != null) {
            this.currentReader.close();
            this.currentReader = null;
        }
    }

}
