package de.unihannover.se.processSimulation.postprocessing;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import de.unihannover.se.processSimulation.common.ReviewMode;
import de.unihannover.se.processSimulation.dataGenerator.ArffWriter;
import de.unihannover.se.processSimulation.dataGenerator.DataGenerator;
import de.unihannover.se.processSimulation.postprocessing.ArffReader.DataHandler;

public class CombineRandomizedTrials {

    private static final class CombinerCallback implements DataHandler {

        private static final String COMBINED_RUN_COUNT = "combinedRunCount";
        private static final String MEAN = "Mean";
        private static final String STD_DEV = "StdDev";
        private static final Set<String> RANDOM_PARAMS = new HashSet<>(Arrays.asList("seed"));

        private final LinkedHashMap<String, List<Map<String, String>>> groupedData = new LinkedHashMap<>();

        @Override
        public void handleInstance(Map<String, String> instance) {
            final StringBuilder inputParamKey = new StringBuilder();
            for (final Entry<String, String> e : instance.entrySet()) {
                if (this.isInputParam(e.getKey())) {
                    inputParamKey.append(e.getValue()).append(',');
                }
            }
            final String key = inputParamKey.toString();
            List<Map<String, String>> entriesForKey = this.groupedData.get(key);
            if (entriesForKey == null) {
                entriesForKey = new ArrayList<>();
                this.groupedData.put(key, entriesForKey);
            }
            entriesForKey.add(instance);
        }

        private boolean isInputParam(String key) {
            return !this.isOutputParam(key) && !this.isRandomParam(key);
        }

        private boolean isRandomParam(String key) {
            return RANDOM_PARAMS.contains(key);
        }

        private boolean isOutputParam(String key) {
            return key.startsWith(ReviewMode.PRE_COMMIT.toString()) || key.startsWith(ReviewMode.POST_COMMIT.toString());
        }

        public void writeResultsTo(ArffWriter w) throws IOException {
            this.writeHeader(w);
            this.combineAndWriteData(w);
        }

        private void writeHeader(ArffWriter w) throws IOException {
            w.addNumericAttribute(COMBINED_RUN_COUNT);
            this.registerOutputAttributes(w, ReviewMode.PRE_COMMIT);
            this.registerOutputAttributes(w, ReviewMode.POST_COMMIT);
            this.registerInputAttributes(w);
        }

        private void registerOutputAttributes(ArffWriter w, ReviewMode mode) throws IOException {
            w.addNumericAttribute(mode + DataGenerator.STORYPOINTS + MEAN);
            w.addNumericAttribute(mode + DataGenerator.STORYPOINTS + STD_DEV);
            w.addNumericAttribute(mode + DataGenerator.CYCLETIME_MEAN);
            w.addNumericAttribute(mode + DataGenerator.CYCLETIME_STD_DEV);
            w.addNumericAttribute(mode + DataGenerator.STARTED_STORIES + MEAN);
            w.addNumericAttribute(mode + DataGenerator.STARTED_STORIES + STD_DEV);
            w.addNumericAttribute(mode + DataGenerator.FINISHED_STORIES + MEAN);
            w.addNumericAttribute(mode + DataGenerator.FINISHED_STORIES + STD_DEV);
            w.addNumericAttribute(mode + DataGenerator.REMAINING_BUGS + MEAN);
            w.addNumericAttribute(mode + DataGenerator.REMAINING_BUGS + STD_DEV);
        }

        private void registerInputAttributes(ArffWriter w) throws IOException {
            final Map<String, String> someData = this.groupedData.values().iterator().next().get(0);
            for (final Entry<String, String> e : someData.entrySet()) {
                if (this.isInputParam(e.getKey())) {
                    if (this.looksLikeNumber(e.getValue())) {
                        w.addNumericAttribute(e.getKey());
                    } else {
                        w.addNominalAttribute(e.getKey(), this.determineDistinctValues(e.getKey()));
                    }
                }
            }
        }

        private boolean looksLikeNumber(String value) {
            try {
                Double.parseDouble(value);
                return true;
            } catch (final NumberFormatException e) {
                return false;
            }
        }

        private Object[] determineDistinctValues(String key) {
            final TreeSet<String> distinctValues = new TreeSet<>();
            for (final List<Map<String, String>> l : this.groupedData.values()) {
                for (final Map<String, String> m : l) {
                    distinctValues.add(m.get(key));
                }
            }
            return distinctValues.toArray();
        }

        private void combineAndWriteData(ArffWriter w) throws IOException {
            for (final List<Map<String, String>> dataWithSameInputParams : this.groupedData.values()) {
                final Map<String, Object> outputRowBuffer = new LinkedHashMap<>();
                this.combineOutputParams(dataWithSameInputParams, ReviewMode.PRE_COMMIT, outputRowBuffer);
                this.combineOutputParams(dataWithSameInputParams, ReviewMode.POST_COMMIT, outputRowBuffer);
                this.copyInputParams(dataWithSameInputParams, outputRowBuffer);
                w.writeTuple(outputRowBuffer);
            }
        }

        private void combineOutputParams(List<Map<String, String>> dataWithSameInputParams, ReviewMode mode, Map<String, Object> outputRowBuffer) {
            outputRowBuffer.put(COMBINED_RUN_COUNT, dataWithSameInputParams.size());
            this.combineToMeanStdDev(mode + DataGenerator.STORYPOINTS, dataWithSameInputParams, outputRowBuffer);
            this.combineToMeanStdDev(mode + DataGenerator.STARTED_STORIES, dataWithSameInputParams, outputRowBuffer);
            this.combineToMeanStdDev(mode + DataGenerator.FINISHED_STORIES, dataWithSameInputParams, outputRowBuffer);
            this.combineToMeanStdDev(mode + DataGenerator.REMAINING_BUGS, dataWithSameInputParams, outputRowBuffer);
            this.combineCycleTime(mode, dataWithSameInputParams, outputRowBuffer);
        }

        private void combineToMeanStdDev(String key, List<Map<String, String>> dataWithSameInputParams,
                        Map<String, Object> outputRowBuffer) {
            final double[] values = this.getValuesWithKey(key, dataWithSameInputParams);
            //TODO das ist aktuell der Mittelwert für die Grundgesamtheit aber die Standardabweichung für eine Stichprobe, das passt nicht
            outputRowBuffer.put(key + MEAN, MathUtil.determineMean(values));
            outputRowBuffer.put(key + STD_DEV, MathUtil.determineStdDev(values));
        }

        private double[] getValuesWithKey(String key, List<Map<String, String>> dataWithSameInputParams) {
            return dataWithSameInputParams.stream().mapToDouble(m -> Double.parseDouble(m.get(key))).toArray();
        }

        private void combineCycleTime(ReviewMode mode, List<Map<String, String>> dataWithSameInputParams, Map<String, Object> outputRowBuffer) {
            final double[] storyCounts = this.getValuesWithKey(mode + DataGenerator.FINISHED_STORIES, dataWithSameInputParams);
            final double[] means = this.getValuesWithKey(mode + DataGenerator.CYCLETIME_MEAN, dataWithSameInputParams);
            final double[] stdDev = this.getValuesWithKey(mode + DataGenerator.CYCLETIME_STD_DEV, dataWithSameInputParams);

            outputRowBuffer.put(mode + DataGenerator.CYCLETIME_MEAN, MathUtil.determineWeightedMean(storyCounts, means));
            outputRowBuffer.put(mode + DataGenerator.CYCLETIME_STD_DEV, MathUtil.determinePooledStdDev(storyCounts, stdDev));
        }

        private void copyInputParams(List<Map<String, String>> dataWithSameInputParams, Map<String, Object> outputRowBuffer) {
            final Map<String, String> data = dataWithSameInputParams.get(0);
            for (final Entry<String, String> e : data.entrySet()) {
                if (this.isInputParam(e.getKey())) {
                    outputRowBuffer.put(e.getKey(), e.getValue());
                }
            }
        }

    }

    public static void main(String[] args) throws IOException {
        combineRandomizeTrials("changingNumberOfDevelopers.arff", "changingNumberOfDevelopersCombined.arff");
    }

    private static void combineRandomizeTrials(String inputFilename, String outputFilename) throws IOException {
        final CombinerCallback callback = new CombinerCallback();
        try (FileReader r = new FileReader(inputFilename)) {
            ArffReader.read(r, callback);
        }

        try (ArffWriter w = new ArffWriter(new FileWriter(outputFilename), outputFilename.replace(".arff", ""))) {
            callback.writeResultsTo(w);
        }
    }

}
