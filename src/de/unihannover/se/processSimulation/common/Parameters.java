package de.unihannover.se.processSimulation.common;

import java.util.concurrent.TimeUnit;

import desmoj.core.dist.BoolDist;
import desmoj.core.dist.BoolDistBernoulli;
import desmoj.core.dist.ContDistConstant;
import desmoj.core.dist.NumericalDist;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.TimeSpan;

public class Parameters {

    private NumericalDist<Double> reviewSkillDist;
    private BoolDist conflictDist;

    public Parameters() {
    }

    public void init(Model owner) {
        this.reviewSkillDist = new ContDistConstant(owner, "reviewSkillDist", 1.0, true, true);
        this.conflictDist = new BoolDistBernoulli(owner, "conflictDist", 0.1, true, true);
    }

    /**
     * Liefert die Verteilung für die Wahrscheinlichkeit, einen Bug zu finden
     * (1.0 = Bug wird mit Sicherheit gefunden, 0.0 = Bug wird mit Sicherheit nicht gefunden).
     */
    public NumericalDist<Double> getReviewSkillDist() {
        return this.reviewSkillDist;
    }

    /**
     * Liefert die Wahrscheinlichkeit, dass ein Task mit einem anderen Task in Konflikt steht,
     * unter der Bedingung dass der zweitere während der Arbeit am ersten commitet wurde.
     */
    public BoolDist getConflictDist() {
        return this.conflictDist;
    }

    /**
     * Liefert die Anzahl der Entwickler in der Simulation.
     */
    public int getNumDevelopers() {
        return 2;
    }

    /**
     * Taskwechsel-Overhead der angesetzt wird, nachdem ein Entwickler für 5 Minuten von einer anderen Aufgabe unterbrochen wurde.
     * Wird zusammen mit {@link #getMaxTaskSwitchOverhead()} verwendet, um die Parameter für die Ebbinghaussche Vergessenskurve zu bestimmen.
     */
    public TimeSpan getTaskSwitchOverheadAfterFiveMinuteInterruption() {
        return new TimeSpan(15, TimeUnit.MINUTES);
    }

    /**
     * Obergrenze für den angesetzten Taskwechsel-Overhead.
     * Wird zusammen mit {@link #getTaskSwitchOverheadAfterFiveMinuteInterruption()} verwendet, um die Parameter für die
     * Ebbinghaussche Vergessenskurve zu bestimmen.
     */
    public TimeSpan getMaxTaskSwitchOverhead() {
        return new TimeSpan(2, TimeUnit.HOURS);
    }

}
