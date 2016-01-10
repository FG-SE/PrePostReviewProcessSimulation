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
    private final double avgImplementationTime;
    private final double avgReviewTime;
    private final double avgRemarkFixingTime;
    private final double avgBugFixingTime;
    private final double avgBugAssessmentTime;
    private final double avgPlanningTime;
    private final double totalImplementationTime;
    private final double totalReviewTime;
    private final double totalRemarkFixingTime;
    private final double totalBugFixingTime;
    private final double totalBugAssessmentTime;
    private final double totalPlanningTime;
    private final double avgReviewRoundCount;
    private final double avgTimePostToPre;
    private final double avgTimePreToCust;

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
                    double avgImplementationTime,
                    double avgReviewTime,
                    double avgRemarkFixingTime,
                    double avgBugFixingTime,
                    double avgBugAssessmentTime,
                    double avgPlanningTime,
                    double totalImplementationTime,
                    double totalReviewTime,
                    double totalRemarkFixingTime,
                    double totalBugFixingTime,
                    double totalBugAssessmentTime,
                    double totalPlanningTime,
                    double avgReviewRoundCount,
                    double avgTimePostToPre,
                    double avgTimePreToCust,
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
        this.avgImplementationTime = avgImplementationTime;
        this.avgReviewTime = avgReviewTime;
        this.avgRemarkFixingTime = avgRemarkFixingTime;
        this.avgBugFixingTime = avgBugFixingTime;
        this.avgBugAssessmentTime = avgBugAssessmentTime;
        this.avgPlanningTime = avgPlanningTime;
        this.totalImplementationTime = totalImplementationTime;
        this.totalReviewTime = totalReviewTime;
        this.totalRemarkFixingTime = totalRemarkFixingTime;
        this.totalBugFixingTime = totalBugFixingTime;
        this.totalBugAssessmentTime = totalBugAssessmentTime;
        this.totalPlanningTime = totalPlanningTime;
        this.avgReviewRoundCount = avgReviewRoundCount;
        this.avgTimePostToPre = avgTimePostToPre;
        this.avgTimePreToCust = avgTimePreToCust;
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

    public double getAvgImplementationTime() {
        return this.avgImplementationTime;
    }

    public double getAvgReviewTime() {
        return this.avgReviewTime;
    }

    public double getAvgRemarkFixingTime() {
        return this.avgRemarkFixingTime;
    }

    public double getAvgBugFixingTime() {
        return this.avgBugFixingTime;
    }

    public double getAvgBugAssessmentTime() {
        return this.avgBugAssessmentTime;
    }

    public double getAvgPlanningTime() {
        return this.avgPlanningTime;
    }

    public double getTotalImplementationTime() {
        return this.totalImplementationTime;
    }

    public double getTotalReviewTime() {
        return this.totalReviewTime;
    }

    public double getTotalRemarkFixingTime() {
        return this.totalRemarkFixingTime;
    }

    public double getTotalBugFixingTime() {
        return this.totalBugFixingTime;
    }

    public double getTotalBugAssessmentTime() {
        return this.totalBugAssessmentTime;
    }

    public double getTotalPlanningTime() {
        return this.totalPlanningTime;
    }

    public double getAvgReviewRoundCount() {
        return this.avgReviewRoundCount;
    }

    public double getAvgTimePostToPre() {
        return this.avgTimePostToPre;
    }

    public double getAvgTimePreToCust() {
        return this.avgTimePreToCust;
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
