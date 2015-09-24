package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.List;

import de.unihannover.se.processSimulation.common.Parameters;
import de.unihannover.se.processSimulation.common.ParametersFactory;
import desmoj.core.dist.LinearCongruentialRandomGenerator;
import desmoj.core.dist.UniformRandomGenerator;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeSpan;
import desmoj.core.statistic.Count;
import desmoj.core.statistic.Tally;

class RealProcessingModel extends Model {

    private final ReviewMode reviewMode;
    private Board board;
    private SourceRepository sourceRepository;

    private Count finishedStoryPoints;
    private Tally storyCycleTime;

    private final List<Developer> developers = new ArrayList<>();
    private final ParametersFactory parameterFactory;
    private Parameters parameters;

    private UniformRandomGenerator genericRandom;

    public RealProcessingModel(String name, ReviewMode reviewMode, ParametersFactory parameterFactory) {
        super(null, name, true, true);
        this.reviewMode = reviewMode;
        this.parameterFactory = parameterFactory;
    }

    @Override
    public String description() {
        return "Modell zum Vergleich von Pre-commit und Post-commit-Reviews anhand der Simulation eines Entwicklungsprozesses.";
    }

    @Override
    public void init() {
        this.parameters = this.parameterFactory.create(this);
        this.genericRandom = new LinearCongruentialRandomGenerator(this.getParameters().getGenericRandomSeed());

        this.board = new Board(this);
        this.sourceRepository = new SourceRepository();

        this.finishedStoryPoints = new Count(this, "finishedStoryPoints", true, true);
        this.storyCycleTime = new Tally(this, "storyCycleTime", true, true);

        for (int i = 0; i < this.parameters.getNumDevelopers(); i++) {
            this.developers.add(new Developer(this,
                            this.parameters.getReviewSkillDist().sample(),
                            this.parameters.getGlobalBugDist().sample(),
                            this.parameters.getImplementationSkillDist().sample()));
        }
    }

    @Override
    public void doInitialSchedules() {
        for (final Developer d : this.developers) {
            d.activate();
        }
    }

    public Board getBoard() {
        return this.board;
    }

    public SourceRepository getSourceRepository() {
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

}