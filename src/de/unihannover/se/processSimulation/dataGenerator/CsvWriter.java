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

package de.unihannover.se.processSimulation.dataGenerator;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvWriter implements DataWriter {

    private final Writer writer;
    private boolean dataStarted;

    private final List<String> attributes = new ArrayList<>();

    public CsvWriter(Writer writer) {
        this.writer = writer;
    }

    @Override
    public void addNumericAttribute(String name) throws IOException {
        assert !this.dataStarted;
        this.attributes.add(name);
    }

    @Override
    public void addNominalAttribute(String name, Object[] enumValues) throws IOException {
        this.addNumericAttribute(name);
    }

    @Override
    public void writeTuple(Map<String, Object> experimentData) throws IOException {
        if (!this.dataStarted) {
            this.dataStarted = true;
            this.writeHeader();
        }

        boolean first = true;
        for (final String att : this.attributes) {
            if (first) {
                first = false;
            } else {
                this.writer.write(';');
            }
            this.writer.write(experimentData.get(att).toString());
        }
        this.writer.write('\n');
    }

    private void writeHeader() throws IOException {
        boolean first = true;
        for (final String att : this.attributes) {
            if (first) {
                first = false;
            } else {
                this.writer.write(';');
            }
            this.writer.write(att);
        }
        this.writer.write('\n');
    }

    @Override
    public void flush() throws IOException {
        this.writer.flush();
    }

    @Override
    public void close() throws IOException {
        this.writer.close();
    }

}
