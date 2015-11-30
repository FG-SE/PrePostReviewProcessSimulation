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
import desmoj.core.simulator.TimeSpan;
import desmoj.core.statistic.Aggregate;
import desmoj.core.statistic.Count;
import desmoj.core.statistic.Tally;

public class RealProcessingModel extends Model {

    static {
        Experiment.setEpsilon(TimeUnit.MINUTES);
        Experiment.setReferenceUnit(TimeUnit.HOURS);
    }

    public static final int HOURS_TO_RESET = 8 * 400;

    private final ReviewMode reviewMode;
    private final boolean plot;

    private Board board;
    private SourceRepository<Task> sourceRepository;

    private Count finishedStoryPoints;
    private Count bugCountFoundByCustomers;
    private Tally storyCycleTime;
    private final Map<String, Aggregate> timeCounters = new HashMap<>();

    private final List<Developer> developers = new ArrayList<>();
    private final ParametersFactory parameterFactory;
    private Parameters parameters;

    private UniformRandomGenerator genericRandom;
    private GraphGenerator dependencyGraphGenerator;

    public RealProcessingModel(String name, ReviewMode reviewMode, ParametersFactory parameterFactory, boolean plot) {
        super(null, name, true, true);
        this.reviewMode = reviewMode;
        this.parameterFactory = parameterFactory;
        this.plot = plot;
    }

    @Override
    public String description() {
        return "Modell zum Vergleich von Pre-commit und Post-commit-Reviews anhand der Simulation eines Entwicklungsprozesses.";
    }

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
                return RealProcessingModel.this.presentTime();
            }
            @Override
            public boolean sampleConflictDist() {
                return RealProcessingModel.this.getParameters().getConflictDist().sample();
            }
            @Override
            public void sendTraceNote(String description) {
                RealProcessingModel.this.sendTraceNote(description);
            }
        });

        this.finishedStoryPoints = new Count(this, "finishedStoryPoints", true, true);
        this.storyCycleTime = new Tally(this, "storyCycleTime", true, true);
        this.bugCountFoundByCustomers = new Count(this, "bugCountFoundByCustomers", true, true);

        for (int i = 0; i < this.parameters.getNumDevelopers(); i++) {
            this.developers.add(new Developer(this,
                            this.parameters.getReviewSkillDist().sample(),
                            this.parameters.getGlobalBugDist().sample(),
                            this.parameters.getImplementationSkillDist().sample()));
        }
    }

    @Override
    public void doInitialSchedules() {
        if (this.plot) {
            new Plotter(this).activate();
        }
        for (final Developer d : this.developers) {
            d.activate();
        }
        //reset after some time, so that starting effects are not measured
        new ExternalEventReset(this, true).schedule(new TimeInstant(HOURS_TO_RESET, TimeUnit.HOURS));
    }

    public Board getBoard() {
        return this.board;
    }

    public SourceRepository<Task> getSourceRepository() {
        return this.sourceRepository;
    }

    public void countFinishedStory(Story story) {
        final TimeSpan cycleTime = story.getCycleTime(this.presentTime());
        this.sendTraceNote("Story " + story + " finished after " + cycleTime);
        this.storyCycleTime.update(cycleTime);
        this.finishedStoryPoints.update(story.getStoryPoints());
    }

    public ReviewMode getReviewMode() {
        return this.reviewMode;
    }

    public Parameters getParameters() {
        return this.parameters;
    }

    public boolean getRandomBool(double propabilityForTrue) {
        return this.genericRandom.nextDouble() < propabilityForTrue;
    }

    public long getFinishedStoryPoints() {
        return this.finishedStoryPoints.getValue();
    }

    public double getStoryCycleTimeMean() {
        if (this.storyCycleTime.getObservations() <= 0) {
            return -1.0;
        }
        return this.storyCycleTime.getMean();
    }

    public double getStoryCycleTimeStdDev() {
        if (this.storyCycleTime.getObservations() <= 1) {
            return -1.0;
        }
        return this.storyCycleTime.getStdDev();
    }

    public int getStartedStoryCount() {
        return this.board.getStartedStoryCount();
    }

    public long getFinishedStoryCount() {
        return this.storyCycleTime.getObservations();
    }

    public long getBugCountFoundByCustomers() {
        return this.bugCountFoundByCustomers.getValue();
    }

    void countBugFoundByCustomer() {
        this.bugCountFoundByCustomers.update();
    }

    public GraphGenerator getGraphGenerator() {
        return this.dependencyGraphGenerator;
    }

    public void countTime(String typeOfWork, TimeInstant startTime) {
        Aggregate agg = this.timeCounters.get(typeOfWork);
        if (agg == null) {
            agg = new Aggregate(this, "timeFor_" + typeOfWork, true, true);
            agg.setShowTimeSpansInReport(true);
            this.timeCounters.put(typeOfWork, agg);
        }
        agg.update(Util.timeBetween(startTime, this.presentTime()));
    }

}
