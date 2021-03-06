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

package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.unihannover.se.processSimulation.common.Parameters;
import de.unihannover.se.processSimulation.common.ParametersFactory;
import de.unihannover.se.processSimulation.common.ReviewMode;
import de.unihannover.se.processSimulation.preCommitPostCommit.SourceRepository.SourceRepositoryDependencies;
import desmoj.core.dist.MersenneTwisterRandomGenerator;
import desmoj.core.dist.UniformRandomGenerator;
import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.ExternalEventReset;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeOperations;
import desmoj.core.simulator.TimeSpan;
import desmoj.core.statistic.Aggregate;
import desmoj.core.statistic.Count;
import desmoj.core.statistic.Tally;

/**
 * The model implementation for comparison of pre commit and post commit reviews.
 * Initializes and manages the model's processes and entities and keeps some statistics.
 */
public class PrePostModel extends Model {

    static {
        Experiment.setEpsilon(TimeUnit.SECONDS);
        Experiment.setReferenceUnit(TimeUnit.HOURS);
    }

    private final ReviewMode reviewMode;
    private final boolean plot;
    private final int hoursToReset;

    private Board board;
    private SourceRepository<Task> sourceRepository;

    private Count finishedStoryPoints;
    private Count issueCountFoundByCustomers;
    private Tally storyCycleTime;
    private Tally planningGroupSize;
    private Tally reviewRoundCount;
    private Tally timePostToPre;
    private Tally timePreToCust;
    private Tally issuesInjectedPerReviewRemark;
    private Tally issuesInjectedPerIssueTask;
    private Tally issuesInjectedPerImplementationTask;
    private final Map<String, Aggregate> timeCounters = new HashMap<>();
    private final Map<String, Count> dynamicCounters = new HashMap<>();

    private final List<Developer> developers = new ArrayList<>();
    private final ParametersFactory parameterFactory;
    private Parameters parameters;

    private UniformRandomGenerator genericRandom;
    private GraphGenerator dependencyGraphGenerator;

    /**
     * Creates a model with the given parameters and the given {@link ReviewMode}.
     */
    public PrePostModel(String name, ReviewMode reviewMode, ParametersFactory parameterFactory, boolean plot, int hoursToReset) {
        super(null, name, true, true);
        this.reviewMode = reviewMode;
        this.parameterFactory = parameterFactory;
        this.plot = plot;
        this.hoursToReset = hoursToReset;
    }

    @Override
    public String description() {
        return "Modell zum Vergleich von Pre-commit und Post-commit-Reviews anhand der Simulation eines Entwicklungsprozesses.";
    }

    /**
     * Initializes all objects needed for execution.
     */
    @Override
    public void init() {
        this.parameters = this.parameterFactory.create(this);
        this.genericRandom = new MersenneTwisterRandomGenerator(this.getParameters().getGenericRandomSeed());
        this.dependencyGraphGenerator = this.parameters.getDependencyGraphConstellation().createGenerator(
                        new MersenneTwisterRandomGenerator(this.getParameters().getGenericRandomSeed() + 8654));

        this.board = new Board(this);
        this.sourceRepository = new SourceRepository<Task>(new SourceRepositoryDependencies() {
            @Override
            public TimeInstant presentTime() {
                return PrePostModel.this.presentTime();
            }
            @Override
            public boolean sampleConflictDist() {
                return PrePostModel.this.getParameters().getConflictDist().sample();
            }
            @Override
            public void sendTraceNote(String description) {
                PrePostModel.this.sendTraceNote(description);
            }
        });

        this.finishedStoryPoints = new Count(this, "finishedStoryPoints", true, false);
        this.storyCycleTime = new Tally(this, "storyCycleTime", true, false);
        this.issueCountFoundByCustomers = new Count(this, "issueCountFoundByCustomers", true, false);
        this.planningGroupSize = new Tally(this, "planningGroupSize", true, false);
        this.reviewRoundCount = new Tally(this, "reviewRoundCount", true, false);
        this.timePostToPre = new Tally(this, "timePostToPre", true, false);
        this.timePreToCust = new Tally(this, "timePreToCust", true, false);
        this.issuesInjectedPerReviewRemark = new Tally(this, "issuesInjectedPerReviewRemark", true, false);
        this.issuesInjectedPerIssueTask = new Tally(this, "issuesInjectedPerIssueTask", true, false);
        this.issuesInjectedPerImplementationTask = new Tally(this, "issuesInjectedPerImplementationTask", true, false);

        for (int i = 0; i < this.parameters.getNumDevelopers(); i++) {
            this.developers.add(new Developer(this,
                            this.parameters.getReviewSkillDist().sample(),
                            this.parameters.getGlobalIssueDist().sample(),
                            this.parameters.getImplementationSkillDist().sample()));
        }
    }

    /**
     * Starts all the model's processes.
     */
    @Override
    public void doInitialSchedules() {
        if (this.plot) {
            new Plotter(this).activate();
        }
        for (final Developer d : this.developers) {
            d.activate();
        }
        //reset after some time, so that starting effects are not measured
        new ExternalEventReset(this, true).schedule(new TimeInstant(this.hoursToReset, TimeUnit.HOURS));
    }

    /**
     * Returns the {@link Board}.
     */
    Board getBoard() {
        return this.board;
    }

    /**
     * Returns the {@link SourceRepository}.
     */
    SourceRepository<Task> getSourceRepository() {
        return this.sourceRepository;
    }

    /**
     * Returns the {@link GraphGenerator} for the chosen task dependency structure.
     */
    GraphGenerator getGraphGenerator() {
        return this.dependencyGraphGenerator;
    }

    /**
     * Adjusts the statistics when a story has been finished.
     */
    void countFinishedStory(Story story) {
        final TimeSpan cycleTime = story.getCycleTime(this.presentTime());
        this.sendTraceNote("Story " + story + " finished after " + cycleTime);
        this.storyCycleTime.update(cycleTime);
        this.finishedStoryPoints.update(story.getStoryPoints());
    }

    /**
     * Returns the review mode chosen for this model.
     */
    public ReviewMode getReviewMode() {
        return this.reviewMode;
    }

    /**
     * Returns the parameters chosen for this model.
     */
    public Parameters getParameters() {
        return this.parameters;
    }

    /**
     * Returns a boolean value that is true with the given probability.
     */
    boolean getRandomBool(double probabilityForTrue) {
        return this.genericRandom.nextDouble() < probabilityForTrue;
    }

    /**
     * Returns the sum of story points that have been finished since the last reset.
     */
    public long getFinishedStoryPoints() {
        return this.finishedStoryPoints.getValue();
    }

    /**
     * Returns the mean story cycle time that has been observed since the last reset.
     * Returns -1 iff there has not been a finished story.
     */
    public double getStoryCycleTimeMean() {
        if (this.storyCycleTime.getObservations() <= 0) {
            return -1.0;
        }
        return this.storyCycleTime.getMean();
    }

    /**
     * Returns the story cycle time's standard deviation that has been observed since the last reset.
     * Returns -1 iff there has been at most one finished story.
     */
    public double getStoryCycleTimeStdDev() {
        if (this.storyCycleTime.getObservations() <= 1) {
            return -1.0;
        }
        return this.storyCycleTime.getStdDev();
    }

    /**
     * Returns the number of stories that has been started since the last reset.
     */
    public long getStartedStoryCount() {
        return this.board.getStartedStoryCount();
    }

    /**
     * Returns the number of stories that has been finished since the last reset.
     */
    public long getFinishedStoryCount() {
        return this.storyCycleTime.getObservations();
    }

    /**
     * Returns the number of issues found by customers since the last reset.
     */
    public long getIssueCountFoundByCustomers() {
        return this.issueCountFoundByCustomers.getValue();
    }

    /**
     * Adjusts the statistics for an issue found by a customer.
     */
    void countIssueFoundByCustomer() {
        this.issueCountFoundByCustomers.update();
    }

    /**
     * Adjusts the statistics for a certain kind of work that was done since a given time instant until now.
     */
    void countTime(String name, TimeInstant startTime) {
        Aggregate agg = this.timeCounters.get(name);
        if (agg == null) {
            agg = new Aggregate(this, name, true, true);
            agg.setShowTimeSpansInReport(true);
            this.timeCounters.put(name, agg);
        }
        final TimeSpan diff = TimeOperations.diff(this.presentTime(), startTime);
        if (!diff.isZero()) {
            agg.update(diff);
        }
    }

    private double getTimeCounterAvg(String name) {
        final Aggregate agg = this.timeCounters.get(name);
        if (agg == null) {
            return -1;
        }
        if (agg.getObservations() == 0) {
            return -1;
        }
        return agg.getValue() / agg.getObservations();
    }

    private double getTimeCounterTotal(String name) {
        final Aggregate agg = this.timeCounters.get(name);
        if (agg == null) {
            return 0;
        }
        return agg.getValue();
    }

    /**
     * Increments the counter with the given name by one.
     */
    void dynamicCount(String name) {
        this.dynamicCount(name, 1);
    }

    /**
     * Increments the counter with the given name by the given count.
     */
    void dynamicCount(String name, int count) {
        Count cnt = this.dynamicCounters.get(name);
        if (cnt == null) {
            cnt = new Count(this, name, true, true);
            this.dynamicCounters.put(name, cnt);
        }
        if (count != 0) {
            cnt.update(count);
        }
    }

    public Tally getPlanningGroupSizeTally() {
        return this.planningGroupSize;
    }

    /**
     * Returns the time (in hours) that has been spent on task switch overhead.
     */
    public double getWastedTimeTaskSwitch() {
        final Aggregate agg = this.timeCounters.get("timeWasted_taskSwitch");
        return agg == null ? 0.0 : agg.getValue();
    }

    /**
     * Returns the number of global issues that occurred.
     */
    public long getGlobalIssueCount() {
        final Count cnt = this.dynamicCounters.get("occurredGlobalIssues");
        return cnt == null ? 0 : cnt.getValue();
    }

    /**
     * Returns the number of conflicts that occurred.
     */
    public long getConflictCount() {
        final Aggregate agg = this.timeCounters.get("timeWasted_resolvingConflicts");
        return agg == null ? 0 : agg.getObservations();
    }

    /**
     * Updates the statistic for the number of review rounds when the last review for a task
     * has been finished.
     */
    public void updateReviewRoundStatistic(int reviewRounds) {
        this.reviewRoundCount.update(reviewRounds);
    }

    /**
     * Returns the average number of review rounds (not including issue assessment short-cut review)
     * performed per task.
     */
    public double getAvgReviewRoundCount() {
        return this.reviewRoundCount.getMean();
    }

    /**
     * Updates the statistic for the average time between the instant when issues would become developer-visible
     * when doing post commit and when doing pre commit.
     */
    public void updateTimePostToPreStatistic(TimeSpan diff) {
        this.timePostToPre.update(diff);
    }

    public double getAvgTimePostToPre() {
        return this.timePostToPre.getMean();
    }

    /**
     * Updates the statistic for the average time between the instant when issues would become developer-visible
     * when doing pre commit and when they will be customer-visible.
     */
    public void updateTimePreToCustStatistic(TimeSpan diff) {
        this.timePreToCust.update(diff);
    }

    public double getAvgTimePreToCust() {
        return this.timePreToCust.getMean();
    }

    public double getAvgImplementationTime() {
        return this.getTimeCounterAvg("timeFor_implementing");
    }

    public double getAvgReviewTime() {
        return this.getTimeCounterAvg("timeFor_reviewing");
    }

    public double getAvgRemarkFixingTime() {
        return this.getTimeCounterAvg("timeFor_fixingReviewRemarks");
    }

    public double getAvgIssueFixingTime() {
        return this.getTimeCounterAvg("timeFor_fixingIssues");
    }

    public double getAvgIssueAssessmentTime() {
        return this.getTimeCounterAvg("timeFor_assessingIssues");
    }

    public double getAvgPlanningTime() {
        return this.getTimeCounterAvg("timeFor_planning");
    }

    public double getTotalImplementationTime() {
        return this.getTimeCounterTotal("timeFor_implementing");
    }

    public double getTotalReviewTime() {
        return this.getTimeCounterTotal("timeFor_reviewing");
    }

    public double getTotalRemarkFixingTime() {
        return this.getTimeCounterTotal("timeFor_fixingReviewRemarks");
    }

    public double getTotalIssueFixingTime() {
        return this.getTimeCounterTotal("timeFor_fixingIssues");
    }

    public double getTotalIssueAssessmentTime() {
        return this.getTimeCounterTotal("timeFor_assessingIssues");
    }

    public double getTotalPlanningTime() {
        return this.getTimeCounterTotal("timeFor_planning");
    }

    void updateIssuesInjectedPerReviewRemark(double ratio) {
        this.issuesInjectedPerReviewRemark.update(ratio);
    }

    public double getAvgIssuesInjectedPerReviewRemark() {
        return this.issuesInjectedPerReviewRemark.getMean();
    }

    void updateIssuesInjectedPerIssueTask(double ratio) {
        this.issuesInjectedPerIssueTask.update(ratio);
    }

    public double getAvgIssuesInjectedPerIssueTask() {
        return this.issuesInjectedPerIssueTask.getMean();
    }

    void updateIssuesInjectedPerImplementationTask(double ratio) {
        this.issuesInjectedPerImplementationTask.update(ratio);
    }

    public double getAvgIssuesInjectedPerImplementationTask() {
        return this.issuesInjectedPerImplementationTask.getMean();
    }

}
