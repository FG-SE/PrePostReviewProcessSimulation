package de.tntinteractive.processSimulation.preCommitPostCommitPrototype;

import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeSpan;

public class DevelopmentProcessModel extends Model {

    private final int developerCount = 2;
    private Board board;
    private Parameters parameters;
    private final ReviewMode reviewMode;

    public DevelopmentProcessModel(String name, boolean showInReport, boolean showInTrace, ReviewMode reviewMode) {
        super(null, name, showInReport, showInTrace);
        this.reviewMode = reviewMode;
    }

    @Override
    public String description() {
        return "Models an agile development process (Kanban-style) with code reviews. Reviews can either be pre-commit or post-commit.";
    }

    /**
     * Initialises static model components like distributions and queues.
     */
    @Override
    public void init() {
        this.board = new Board(this);
        this.parameters = new Parameters();
    }

    /**
     * Activates dynamic model components (simulation processes).
     *
     * This method is used to place all events or processes on the
     * internal event list of the simulator which are necessary to start
     * the simulation.
     *
     * In this case these are the developers, which will then generate new stories if they have nothing else to do.
     */
    @Override
    public void doInitialSchedules() {
        for (int i = 0; i < this.developerCount; i++) {
            final Developer dev = new Developer(this, "Dev", 0.5);
            dev.activate(new TimeSpan(0));
        }
    }

    Board getBoard() {
        return this.board;
    }

    Parameters getParameters() {
        return this.parameters;
    }

    public ReviewMode getReviewMode() {
        return this.reviewMode;
    }

}
