package de.unihannover.se.processSimulation.dataGenerator;

public class ExperimentResult {

    private final long finishedStoryPoints;
    private final double storyCycleTimeMean;
    private final double storyCycleTimeStdDev;
    private final int startedStoryCount;
    private final long finishedStoryCount;
    //TEST TODO Umstellen auf "Bugs not found by developers (= remaining + found by customers)
    private final long remainingBugCount;
    private final long investedPersonHours;
    private final long expDuration;

    public ExperimentResult(
                    long finishedStoryPoints,
                    double storyCycleTimeMean,
                    double storyCycleTimeStdDev,
                    int startedStoryCount,
                    long finishedStoryCount,
                    long remainingBugCount,
                    long investedPersonHours,
                    long expDuration) {
        this.finishedStoryPoints = finishedStoryPoints;
        this.storyCycleTimeMean = storyCycleTimeMean;
        this.storyCycleTimeStdDev = storyCycleTimeStdDev;
        this.startedStoryCount = startedStoryCount;
        this.finishedStoryCount = finishedStoryCount;
        this.remainingBugCount = remainingBugCount;
        this.investedPersonHours = investedPersonHours;
        this.expDuration = expDuration;
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

    public int getStartedStoryCount() {
        return this.startedStoryCount;
    }

    public long getFinishedStoryCount() {
        return this.finishedStoryCount;
    }

    public long getRemainingBugCount() {
        return this.remainingBugCount;
    }

    public long getInvestedPersonHours() {
        return this.investedPersonHours;
    }

    public long getExperimentDuration() {
        return this.expDuration;
    }

}
