package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.List;

import desmoj.core.simulator.Model;

public class RealProcessingModel extends Model {

    private final ReviewMode reviewMode;
    private Board board;

    private final List<Developer> developers = new ArrayList<>();

    public RealProcessingModel(String name, ReviewMode reviewMode) {
        super(null, name, true, true);
        this.reviewMode = reviewMode;
    }

    @Override
    public String description() {
        return "Modell zum Vergleich von Pre-commit und Post-commit-Reviews anhand der Simulation eines Entwicklungsprozesses.";
    }

    @Override
    public void init() {
        this.board = new Board(this);
        this.developers.add(new Developer(this));

        //TEST
        final Task task = new Task(this);
        task.startImplementation(this.developers.get(0));
        this.board.addTaskWithReviewRemarks(task);
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

}
