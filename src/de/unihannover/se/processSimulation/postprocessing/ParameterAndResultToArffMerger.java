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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.paralleluniverse.common.util.Pair;
import de.unihannover.se.processSimulation.dataGenerator.ArffWriter;
import de.unihannover.se.processSimulation.dataGenerator.BulkFileExecutor;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory.ParameterType;

public class ParameterAndResultToArffMerger {

    public static void main(String[] args) throws Exception {
//        mergeToArff(new File("sobolStuff/params.txt"),
//                    new File("sobolStuff/sobolParameterSets.txt"),
//                    new File("sobolStuff/results.txt"),
//                    new File("sobolStuff/combined.arff"));
        mergeToArff(new File("sobolStuff/params.txt"),
                        new File("sobolStuff/randomParameterSets.txt"),
                        new File("sobolStuff/results.txt"),
                        new File("sobolStuff/combined.arff"));
    }

    private static void mergeToArff(
                    File paramsFile,
                    File parameterSetsFile,
                    File resultsFile,
                    File outputFile) throws Exception {
        final List<ParameterType> paramNames = BulkFileExecutor.readParamNames(paramsFile);
        try (BufferedReader inputs = new BufferedReader(new FileReader(parameterSetsFile))) {
            try (BufferedReader results = new BufferedReader(new FileReader(resultsFile))) {
                try (ArffWriter output = new ArffWriter(new FileWriter(outputFile), "combinedExperimentResults")) {
                    mergeToArff(paramNames, inputs, results, output);
                }
            }
        }
    }

    private static void mergeToArff(List<ParameterType> paramNames, BufferedReader inputs, BufferedReader results,
                    ArffWriter output) throws IOException {

        for (final ParameterType param : paramNames) {
            if (param.getType().isEnum()) {
                output.addNominalAttribute(param.name(), param.getType().getEnumConstants());
            } else {
                output.addNumericAttribute(param.name());
            }
        }
        for (final Pair<String, Class<?>> resultAttribute : BulkFileExecutor.getResultAttributes()) {
            if (resultAttribute.getSecond().isEnum()) {
                output.addNominalAttribute(resultAttribute.getFirst(), resultAttribute.getSecond().getEnumConstants());
            } else if (resultAttribute.getSecond().equals(Boolean.class)) {
                output.addNominalAttribute(resultAttribute.getFirst(), new String[] {"true", "false"});
            } else {
                output.addNumericAttribute(resultAttribute.getFirst());
            }
        }

        while (true) {
            final String inputLine = inputs.readLine();
            if (inputLine == null) {
                break;
            }
            final String resultLine = results.readLine();
            if (resultLine == null) {
                break;
            }
            final Map<String, Object> data = new HashMap<>();
            parseInputLine(paramNames, inputLine, data);
            parseResultLine(resultLine, data);
            output.writeTuple(data);
        }
    }

    private static void parseInputLine(List<ParameterType> paramNames, String inputLine, Map<String, Object> data) {
        final String[] parts = inputLine.split(" ");
        assert parts.length == paramNames.size();
        for (int i = 0; i < parts.length; i++) {
            data.put(paramNames.get(i).name(), BulkFileExecutor.parseParameterValue(parts[i], paramNames.get(i)));
        }
    }

    private static void parseResultLine(String resultLine, Map<String, Object> data) {
        final String[] parts = resultLine.split(";");
        final List<Pair<String, Class<?>>> resultAttributes = BulkFileExecutor.getResultAttributes();
        assert parts.length == resultAttributes.size();
        for (int i = 0; i < parts.length; i++) {
            data.put(resultAttributes.get(i).getFirst(), parts[i]);
        }
    }

}
