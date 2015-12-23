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
    private final long bugCountFoundByCustomers;
    private final long investedPersonHours;
    private final long elapsedHours;
    private final double wastedTimeTaskSwitch;
    private final long conflictCount;
    private final long globalBugCount;
    private final long expWallClockDuration;
    private final boolean hadError;

    public ExperimentResult(
                    long finishedStoryPoints,
                    double storyCycleTimeMean,
                    double storyCycleTimeStdDev,
                    long startedStoryCount,
                    long finishedStoryCount,
                    long bugCountFoundByCustomers,
                    long investedPersonHours,
                    long elapsedHours,
                    double wastedTimeTaskSwitch,
                    long conflictCount,
                    long globalBugCount,
                    long expWallClockDuration,
                    boolean hadError) {
        this.finishedStoryPoints = finishedStoryPoints;
        this.storyCycleTimeMean = storyCycleTimeMean;
        this.storyCycleTimeStdDev = storyCycleTimeStdDev;
        this.startedStoryCount = startedStoryCount;
        this.finishedStoryCount = finishedStoryCount;
        this.bugCountFoundByCustomers = bugCountFoundByCustomers;
        this.investedPersonHours = investedPersonHours;
        this.elapsedHours = elapsedHours;
        this.wastedTimeTaskSwitch = wastedTimeTaskSwitch;
        this.conflictCount = conflictCount;
        this.globalBugCount = globalBugCount;
        this.expWallClockDuration = expWallClockDuration;
        this.hadError = hadError;
    }

    public long getFinishedStoryPoints() {
        return this.finishedStoryPoints;
    }

    public double getStoryCycleTimeMean() {
        return this.storyCycleTimeMean;
    }

    /**
     * Return the mean of the story cycle time. When no story was finished, the experiment duration in simulation clock hours
     * is returned (because the cycle time must be at least this long).
     */
    public double getStoryCycleTimeMeanWithDefault() {
        return this.storyCycleTimeMean > 0 ? this.storyCycleTimeMean : this.elapsedHours;
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
        //adjustment by 1.0 for cases where no story points were done
        return (this.bugCountFoundByCustomers + 1.0) / (this.finishedStoryPoints + 1.0);
    }

    public long getInvestedPersonHours() {
        return this.investedPersonHours;
    }

    public double getWastedTimeTaskSwitch() {
        return this.wastedTimeTaskSwitch;
    }

    public long getConflictCount() {
        return this.conflictCount;
    }

    public long getGlobalBugCount() {
        return this.globalBugCount;
    }

    public long getExperimentDuration() {
        return this.expWallClockDuration;
    }

    public boolean hadError() {
        return this.hadError;
    }

}
