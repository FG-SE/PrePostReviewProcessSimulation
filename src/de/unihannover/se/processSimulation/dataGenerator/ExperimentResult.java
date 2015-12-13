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

public class ExperimentResult {

    private final long finishedStoryPoints;
    private final double storyCycleTimeMean;
    private final double storyCycleTimeStdDev;
    private final long startedStoryCount;
    private final long finishedStoryCount;
    //TODO sollte auch noch eine Kennzahl für den Technical Debt eingebaut werden?
    private final long bugCountFoundByCustomers;
    private final long investedPersonHours;
    private final long expDuration;
    private final boolean hadError;

    public ExperimentResult(
                    long finishedStoryPoints,
                    double storyCycleTimeMean,
                    double storyCycleTimeStdDev,
                    long startedStoryCount,
                    long finishedStoryCount,
                    long bugCountFoundByCustomers,
                    long investedPersonHours,
                    long expDuration,
                    boolean hadError) {
        this.finishedStoryPoints = finishedStoryPoints;
        this.storyCycleTimeMean = storyCycleTimeMean;
        this.storyCycleTimeStdDev = storyCycleTimeStdDev;
        this.startedStoryCount = startedStoryCount;
        this.finishedStoryCount = finishedStoryCount;
        this.bugCountFoundByCustomers = bugCountFoundByCustomers;
        this.investedPersonHours = investedPersonHours;
        this.expDuration = expDuration;
        this.hadError = hadError;
    }

    public long getFinishedStoryPoints() {
        return this.finishedStoryPoints;
    }

    public double getStoryCycleTimeMean() {
        return this.storyCycleTimeMean;
    }

    public double getStoryCycleTimeStdDev() {
        return this.storyCycleTimeStdDev;
    }

    public long getStartedStoryCount() {
        return this.startedStoryCount;
    }

    public long getFinishedStoryCount() {
        return this.finishedStoryCount;
    }

    public long getBugCountFoundByCustomers() {
        return this.bugCountFoundByCustomers;
    }

    public double getBugCountFoundByCustomersPerStoryPoint() {
        if (this.finishedStoryPoints == 0) {
            return this.bugCountFoundByCustomers;
        }
        return ((double) this.bugCountFoundByCustomers) / this.finishedStoryPoints;
    }

    public long getInvestedPersonHours() {
        return this.investedPersonHours;
    }

    public long getExperimentDuration() {
        return this.expDuration;
    }

    public boolean hadError() {
        return this.hadError;
    }

}
