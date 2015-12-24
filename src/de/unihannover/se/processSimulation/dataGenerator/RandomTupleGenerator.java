package de.unihannover.se.processSimulation.dataGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import desmoj.core.dist.MersenneTwisterRandomGenerator;

public class RandomTupleGenerator {

    private static final class NumberInterval {
        private final double from;
        private final double to;

        public NumberInterval(double from, double to) {
            this.from = from;
            this.to = to;
        }

        public double sample(MersenneTwisterRandomGenerator rng) {
            return this.from + (this.to - this.from) * rng.nextDouble();
        }

    }

    public static void main(String[] args) throws IOException {
        final File paramFile = new File(args[0]);
        final int tupleCount = Integer.parseInt(args[1]);
        final File outputFile = new File(args[2]);

        final List<NumberInterval> params = readParams(paramFile);
        final MersenneTwisterRandomGenerator rng = new MersenneTwisterRandomGenerator(System.currentTimeMillis());
        try (FileWriter output = new FileWriter(outputFile)) {
            for (int i = 0; i < tupleCount; i++) {
                for (int j = 0; j < params.size(); j++) {
                    output.write(String.format(Locale.ENGLISH, "%f", params.get(j).sample(rng)));
                    output.write(j == params.size() - 1 ? '\n' : ' ');
                }
            }
        }
    }

    private static List<NumberInterval> readParams(File paramFile) throws IOException {
        final List<NumberInterval> ret = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(paramFile))) {
            String line;
            while ((line = r.readLine()) != null) {
                final String[] parts = line.split(" ");
                if (parts.length != 3) {
                    throw new RuntimeException("invalid line: " + line);
                }
                ret.add(new NumberInterval(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
            }
        }
        return ret;
    }

}
