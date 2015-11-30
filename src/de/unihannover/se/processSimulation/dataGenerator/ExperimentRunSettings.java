package de.unihannover.se.processSimulation.dataGenerator;

import java.util.EnumMap;

public class ExperimentRunSettings {

    public static enum ExperimentRunParameters {
        MIN_RUNS,
        MAX_RUNS,
        LIMIT_UNREALISTIC,
        LIMIT_NO_REVIEW,
        LIMIT_NEGLIGIBLE_DIFFERENCE_STORY_POINTS,
        LIMIT_NEGLIGIBLE_DIFFERENCE_BUGS,
        LIMIT_NEGLIGIBLE_DIFFERENCE_CYCLE_TIME,
        CONFIDENCE_P;

        public String getDescription() {
            //TODO
            return "";
        }
    }

    private final EnumMap<ExperimentRunParameters, Double> params = new EnumMap<>(ExperimentRunParameters.class);

    public static ExperimentRunSettings defaultSettings() {
        final ExperimentRunSettings ret = new ExperimentRunSettings();
        ret.params.put(ExperimentRunParameters.MIN_RUNS, 15.0);
        ret.params.put(ExperimentRunParameters.MAX_RUNS, 30.0);
        ret.params.put(ExperimentRunParameters.LIMIT_UNREALISTIC, 0.1);
        ret.params.put(ExperimentRunParameters.LIMIT_NO_REVIEW, 1.1);
        ret.params.put(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_STORY_POINTS, 0.05);
        ret.params.put(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_BUGS, 0.05);
        ret.params.put(ExperimentRunParameters.LIMIT_NEGLIGIBLE_DIFFERENCE_CYCLE_TIME, 0.05);
        ret.params.put(ExperimentRunParameters.CONFIDENCE_P, 0.01);
        return ret;
    }

    public double get(ExperimentRunParameters param) {
        return this.params.get(param);
    }

    public ExperimentRunSettings copyWithChangedParam(ExperimentRunParameters param, double value) {
        final ExperimentRunSettings copy = new ExperimentRunSettings();
        copy.params.putAll(this.params);
        copy.params.put(param, value);
        return copy;
    }
}
