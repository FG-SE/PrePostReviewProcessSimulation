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
    private final long issueCountFoundByCustomers;
    private final long investedPersonHours;
    private final long elapsedHours;
    private final double wastedTimeTaskSwitch;
    private final long conflictCount;
    private final long globalIssueCount;
    private final long expWallClockDuration;
    private final boolean hadError;
    private final double avgImplementationTime;
    private final double avgReviewTime;
    private final double avgRemarkFixingTime;
    private final double avgIssueFixingTime;
    private final double avgIssueAssessmentTime;
    private final double avgPlanningTime;
    private final double totalImplementationTime;
    private final double totalReviewTime;
    private final double totalRemarkFixingTime;
    private final double totalIssueFixingTime;
    private final double totalIssueAssessmentTime;
    private final double totalPlanningTime;
    private final double avgReviewRoundCount;
    private final double avgTimePostToPre;
    private final double avgTimePreToCust;
    private final double avgIssuesInjectedPerReviewRemark;
    private final double avgIssuesInjectedPerIssueTask;
    private final double avgIssuesInjectedPerImplementationTask;

    public ExperimentResult(
                    long finishedStoryPoints,
                    double storyCycleTimeMean,
                    double storyCycleTimeStdDev,
                    long startedStoryCount,
                    long finishedStoryCount,
                    long issueCountFoundByCustomers,
                    long investedPersonHours,
                    long elapsedHours,
                    double wastedTimeTaskSwitch,
                    long conflictCount,
                    long globalIssueCount,
                    double avgImplementationTime,
                    double avgReviewTime,
                    double avgRemarkFixingTime,
                    double avgIssueFixingTime,
                    double avgIssueAssessmentTime,
                    double avgPlanningTime,
                    double totalImplementationTime,
                    double totalReviewTime,
                    double totalRemarkFixingTime,
                    double totalIssueFixingTime,
                    double totalIssueAssessmentTime,
                    double totalPlanningTime,
                    double avgReviewRoundCount,
                    double avgTimePostToPre,
                    double avgTimePreToCust,
                    double avgIssuesInjectedPerReviewRemark,
                    double avgIssuesInjectedPerIssueTask,
                    double avgIssuesInjectedPerImplementationTask,
                    long expWallClockDuration,
                    boolean hadError) {
        this.finishedStoryPoints = finishedStoryPoints;
        this.storyCycleTimeMean = storyCycleTimeMean;
        this.storyCycleTimeStdDev = storyCycleTimeStdDev;
        this.startedStoryCount = startedStoryCount;
        this.finishedStoryCount = finishedStoryCount;
        this.issueCountFoundByCustomers = issueCountFoundByCustomers;
        this.investedPersonHours = investedPersonHours;
        this.elapsedHours = elapsedHours;
        this.wastedTimeTaskSwitch = wastedTimeTaskSwitch;
        this.conflictCount = conflictCount;
        this.globalIssueCount = globalIssueCount;
        this.avgImplementationTime = avgImplementationTime;
        this.avgReviewTime = avgReviewTime;
        this.avgRemarkFixingTime = avgRemarkFixingTime;
        this.avgIssueFixingTime = avgIssueFixingTime;
        this.avgIssueAssessmentTime = avgIssueAssessmentTime;
        this.avgPlanningTime = avgPlanningTime;
        this.totalImplementationTime = totalImplementationTime;
        this.totalReviewTime = totalReviewTime;
        this.totalRemarkFixingTime = totalRemarkFixingTime;
        this.totalIssueFixingTime = totalIssueFixingTime;
        this.totalIssueAssessmentTime = totalIssueAssessmentTime;
        this.totalPlanningTime = totalPlanningTime;
        this.avgReviewRoundCount = avgReviewRoundCount;
        this.avgTimePostToPre = avgTimePostToPre;
        this.avgTimePreToCust = avgTimePreToCust;
        this.avgIssuesInjectedPerReviewRemark = avgIssuesInjectedPerReviewRemark;
        this.avgIssuesInjectedPerIssueTask = avgIssuesInjectedPerIssueTask;
        this.avgIssuesInjectedPerImplementationTask = avgIssuesInjectedPerImplementationTask;
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

    public long getIssueCountFoundByCustomers() {
        return this.issueCountFoundByCustomers;
    }

    public double getIssueCountFoundByCustomersPerStoryPoint() {
        //adjustment by 1.0 for cases where no story points were done
        return (this.issueCountFoundByCustomers + 1.0) / (this.finishedStoryPoints + 1.0);
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

    public double getAvgIssueFixingTime() {
        return this.avgIssueFixingTime;
    }

    public double getAvgIssueAssessmentTime() {
        return this.avgIssueAssessmentTime;
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

    public double getTotalIssueFixingTime() {
        return this.totalIssueFixingTime;
    }

    public double getTotalIssueAssessmentTime() {
        return this.totalIssueAssessmentTime;
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

    public double getAvgIssuesInjectedPerReviewRemark() {
        return this.avgIssuesInjectedPerReviewRemark;
    }

    public double getAvgIssuesInjectedPerIssueTask() {
        return this.avgIssuesInjectedPerIssueTask;
    }

    public double getAvgIssuesInjectedPerImplementationTask() {
        return this.avgIssuesInjectedPerImplementationTask;
    }

    public double getWastedTimeTaskSwitch() {
        return this.wastedTimeTaskSwitch;
    }

    public long getConflictCount() {
        return this.conflictCount;
    }

    public long getGlobalIssueCount() {
        return this.globalIssueCount;
    }

    public long getExperimentDuration() {
        return this.expWallClockDuration;
    }

    public boolean hadError() {
        return this.hadError;
    }

}
