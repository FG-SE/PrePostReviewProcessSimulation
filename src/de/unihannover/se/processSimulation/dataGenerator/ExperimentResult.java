package de.unihannover.se.processSimulation.dataGenerator;

public class ExperimentResult {

    private final long finishedStoryPoints;
    private final double storyCycleTimeMean;
    private final double storyCycleTimeStdDev;
    private final int startedStoryCount;
    private final long finishedStoryCount;
    //TODO sollte auch noch eine Kennzahl für den Technical Debt eingebaut werden?
    private final long bugCountFoundByCustomers;
    private final long investedPersonHours;
    private final long expDuration;

    public ExperimentResult(
                    long finishedStoryPoints,
                    double storyCycleTimeMean,
                    double storyCycleTimeStdDev,
                    int startedStoryCount,
                    long finishedStoryCount,
                    long bugCountFoundByCustomers,
                    long investedPersonHours,
                    long expDuration) {
        this.finishedStoryPoints = finishedStoryPoints;
        this.storyCycleTimeMean = storyCycleTimeMean;
        this.storyCycleTimeStdDev = storyCycleTimeStdDev;
        this.startedStoryCount = startedStoryCount;
        this.finishedStoryCount = finishedStoryCount;
        this.bugCountFoundByCustomers = bugCountFoundByCustomers;
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

    public long getBugCountFoundByCustomers() {
        return this.bugCountFoundByCustomers;
    }

    public long getInvestedPersonHours() {
        return this.investedPersonHours;
    }

    public long getExperimentDuration() {
        return this.expDuration;
    }

}
