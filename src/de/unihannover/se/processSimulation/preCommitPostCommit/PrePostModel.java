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
    private Count bugCountFoundByCustomers;
    private Tally storyCycleTime;
    private Tally planningGroupSize;
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
        this.bugCountFoundByCustomers = new Count(this, "bugCountFoundByCustomers", true, false);
        this.planningGroupSize = new Tally(this, "planningGroupSize", true, false);

        for (int i = 0; i < this.parameters.getNumDevelopers(); i++) {
            this.developers.add(new Developer(this,
                            this.parameters.getReviewSkillDist().sample(),
                            this.parameters.getGlobalBugDist().sample(),
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
     * Returns the number of bugs found by customers since the last reset.
     */
    public long getBugCountFoundByCustomers() {
        return this.bugCountFoundByCustomers.getValue();
    }

    /**
     * Adjusts the statistics for a bug found by a customer.
     */
    void countBugFoundByCustomer() {
        this.bugCountFoundByCustomers.update();
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
     * Returns the number of global bugs that occurred.
     */
    public long getGlobalBugCount() {
        final Count cnt = this.dynamicCounters.get("occurredGlobalBugs");
        return cnt == null ? 0 : cnt.getValue();
    }

    /**
     * Returns the number of conflicts that occurred.
     */
    public long getConflictCount() {
        final Aggregate agg = this.timeCounters.get("timeWasted_resolvingConflicts");
        return agg == null ? 0 : agg.getObservations();
    }

}
