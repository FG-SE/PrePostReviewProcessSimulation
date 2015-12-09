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

import java.util.EnumMap;

public class ExperimentRunSettings {

    public static enum ExperimentRunParameters {
        MIN_RUNS("Mindestanzahl an Simulationsdurchläufen mit unterschiedlichen Zufallswerten"),
        MAX_RUNS("Höchstzahl an Simulationsdurchläufen, nach der abgebrochen wird selbst wenn eine Ergebniszuordnung noch nicht statistisch signifikant ist."),
        LIMIT_UNREALISTIC("Grenzwert, bei der als Gesamtergebnis 'unrealistisch' angenommen wird. Angegeben als Verhältnis 'produktiver Arbeit' zu 'geleisteten Stunden', d.h. 1 ist das Optimimum und 0 ist totale Unproduktivität."),
        LIMIT_NO_REVIEW("Grenzwert für das Verhältnis der ohne und mit Review erzielten Story-Points, ab dem 'kein Review' als beste Lösung angesehen wird. Werte größer 1 bedeuten, dass Review selbst dann noch gemacht wird (z.B. zur Wissensverteilung), wenn es laut Simulation etwas schlechtere Story Point-Werte liefert."),
        LIMIT_NEGLIGIBLE_DIFFERENCE_STORY_POINTS("Grenzwert für den Korridor, innerhalb dessen ein Unterschied bei den erzielten Story Points als 'vernachlässigbar' gilt. 0,05 heißt z.B., dass Unterschiede von -5% bis %5 vernachlässigbar sind."),
        LIMIT_NEGLIGIBLE_DIFFERENCE_BUGS("Grenzwert für den Korridor, innerhalb dessen ein Unterschied bei den vom Kunden entdeckten Bugs als 'vernachlässigbar' gilt. 0,05 heißt z.B., dass Unterschiede von -5% bis %5 vernachlässigbar sind."),
        LIMIT_NEGLIGIBLE_DIFFERENCE_CYCLE_TIME("Grenzwert für den Korridor, innerhalb dessen ein Unterschied bei der durchschnittlichen Story-Durchlaufzeit als 'vernachlässigbar' gilt. 0,05 heißt z.B., dass Unterschiede von -5% bis %5 vernachlässigbar sind."),
        CONFIDENCE_P("p-Wert, mit dem die Konfidenzintervalle bestimmt werden"),
        WORKING_DAYS_FOR_STARTUP("Anzahl Werktage, die als 'Aufwärm-Zeit' nicht in die Auswertung einbezogen werden."),
        WORKING_DAYS_FOR_MEASUREMENT("Anzahl Werktage, die nach dem aufwärmen für die Messung verwendet werden.");

        private final String description;

        private ExperimentRunParameters(String description) {
            this.description = description;
        }

        public String getDescription() {
            return this.description;
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
        ret.params.put(ExperimentRunParameters.WORKING_DAYS_FOR_STARTUP, 400.0);
        ret.params.put(ExperimentRunParameters.WORKING_DAYS_FOR_MEASUREMENT, 600.0);
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
