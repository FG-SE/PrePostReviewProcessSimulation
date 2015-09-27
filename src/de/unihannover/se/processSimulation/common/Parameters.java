package de.unihannover.se.processSimulation.common;

import desmoj.core.dist.BoolDist;
import desmoj.core.dist.NumericalDist;
import desmoj.core.simulator.TimeSpan;

public class Parameters {

    private final NumericalDist<Double> implementationSkillDist;
    private final NumericalDist<Double> reviewSkillDist;
    private final NumericalDist<Double> globalBugDist;
    private final BoolDist conflictDist;
    private final NumericalDist<Double> implementationTimeDist;
    private final NumericalDist<Double> fixTimeDist;
    private final NumericalDist<Double> globalBugSuspendTimeDist;
    private final NumericalDist<Double> conflictResolutionTimeDist;
    private final NumericalDist<Double> bugActivationTimeDist;
    private final NumericalDist<Double> planningTimeDist;
    private final NumericalDist<Double> reviewTimeDist;
    private final int numDevelopers;
    private final TimeSpan taskSwitchOverheadAfterOneHour;
    private final TimeSpan maxTaskSwitchOverhead;
    private final long genericRandomSeed;

    public Parameters(
                    NumericalDist<Double> implementationSkillDist,
                    NumericalDist<Double> reviewSkillDist,
                    NumericalDist<Double> globalBugDist,
                    BoolDist conflictDist,
                    NumericalDist<Double> implementationTimeDist,
                    NumericalDist<Double> fixTimeDist,
                    NumericalDist<Double> globalBugSuspendTimeDist,
                    NumericalDist<Double> conflictResolutionTimeDist,
                    NumericalDist<Double> bugActivationTimeDist,
                    NumericalDist<Double> planningTimeDist,
                    NumericalDist<Double> reviewTimeDist,
                    int numDevelopers,
                    TimeSpan taskSwitchOverheadAfterOneHour,
                    TimeSpan maxTaskSwitchOverhead,
                    long genericRandomSeed) {
        this.implementationSkillDist = implementationSkillDist;
        this.reviewSkillDist = reviewSkillDist;
        this.globalBugDist = globalBugDist;
        this.conflictDist = conflictDist;
        this.implementationTimeDist = implementationTimeDist;
        this.fixTimeDist = fixTimeDist;
        this.globalBugSuspendTimeDist = globalBugSuspendTimeDist;
        this.conflictResolutionTimeDist = conflictResolutionTimeDist;
        this.bugActivationTimeDist = bugActivationTimeDist;
        this.planningTimeDist = planningTimeDist;
        this.reviewTimeDist = reviewTimeDist;
        this.numDevelopers = numDevelopers;
        this.taskSwitchOverheadAfterOneHour = taskSwitchOverheadAfterOneHour;
        this.maxTaskSwitchOverhead = maxTaskSwitchOverhead;
        this.genericRandomSeed = genericRandomSeed;
    }

    /**
     * Liefert die Verteilung f�r den "Entwickler-Skill", gemessen in Bugs/Stunde Implementierung.
     */
    public NumericalDist<Double> getImplementationSkillDist() {
        return this.implementationSkillDist;
    }

    /**
     * Liefert die Verteilung f�r die Wahrscheinlichkeit, einen Bug zu finden (je Entwickler)
     * (1.0 = Bug wird mit Sicherheit gefunden, 0.0 = Bug wird mit Sicherheit nicht gefunden).
     */
    public NumericalDist<Double> getReviewSkillDist() {
        return this.reviewSkillDist;
    }

    /**
     * Liefert die Verteilung f�r die Wahrscheinlichkeit, einen globalen "Blocker-Bug" einzuf�gen (je Entwickler)
     * (1.0 = In jeden Task wird ein Global-Bug eingef�gt, 0.0 = Global-Bugs werden nie eingef�gt).
     */
    public NumericalDist<Double> getGlobalBugDist() {
        return this.globalBugDist;
    }

    /**
     * Liefert die Wahrscheinlichkeit, dass ein Task mit einem anderen Task in Konflikt steht,
     * unter der Bedingung dass der zweitere w�hrend der Arbeit am ersten commitet wurde.
     */
    public BoolDist getConflictDist() {
        return this.conflictDist;
    }

    /**
     * Liefert die Anzahl der Entwickler in der Simulation.
     */
    public int getNumDevelopers() {
        return this.numDevelopers;
    }

    /**
     * Liefert die Verteilung, aus der die Implementierungsdauer von Tasks bezogen wird.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getImplementationTimeDist() {
        return this.implementationTimeDist;
    }

    /**
     * Liefert die Verteilung, aus der ben�tigte Zeit f�r die Korrektur EINER Review-Anmerkung bezogen wird.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getFixTimeDist() {
        return this.fixTimeDist;
    }

    /**
     * Liefert die Verteilung, aus der ben�tigte Zeit f�r das Review eines Tasks bezogen wird.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getReviewTimeDist() {
        return this.reviewTimeDist;
    }

    /**
     * Liefert die Verteilung, aus der ben�tigte Zeit f�r die Planung einer Story bezogen wird.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getPlanningTimeDist() {
        return this.planningTimeDist;
    }

    /**
     * Liefert die Verteilung, aus der Zeit, nach der ein normaler Bug auftritt (gez�hlt ab Commit), bezogen wird.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getBugActivationTimeDist() {
        return this.bugActivationTimeDist;
    }

    /**
     * Liefert die Verteilung, aus der die f�r das Aufl�sen eines Konflikts mit einem oder mehreren parallel bearbeiteten Tasks
     * ben�tigte Zeit bezogen wird.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getConflictResolutionTimeDist() {
        return this.conflictResolutionTimeDist;
    }

    /**
     * Liefert die Verteilung, aus der die Zeit bezogen wird, die ein Entwickler durch das Auftreten eines "globalen Bugs" w�hrend der Implementierung
     * verliert.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getGlobalBugSuspendTimeDist() {
        return this.globalBugSuspendTimeDist;
    }

    /**
     * Taskwechsel-Overhead der angesetzt wird, nachdem ein Entwickler f�r eine Stunde von einer anderen Aufgabe unterbrochen wurde.
     * Wird zusammen mit {@link #getMaxTaskSwitchOverhead()} verwendet, um die Parameter f�r die Ebbinghaussche Vergessenskurve zu bestimmen.
     */
    public TimeSpan getTaskSwitchOverheadAfterOneHourInterruption() {
        return this.taskSwitchOverheadAfterOneHour;
    }

    /**
     * Obergrenze f�r den angesetzten Taskwechsel-Overhead.
     * Wird zusammen mit {@link #getTaskSwitchOverheadAfterFiveMinuteInterruption()} verwendet, um die Parameter f�r die
     * Ebbinghaussche Vergessenskurve zu bestimmen.
     */
    public TimeSpan getMaxTaskSwitchOverhead() {
        //TODO: besser als Anteil des Gesamtaufwands der Story (abh�ngig von der Anzahl Story Points) angeben
        //TODO: vielleicht auch noch unterscheiden zwischen "Zeit f�r komplettes Neu-Einarbeiten" und "Zeit f�r Wieder-Einarbeiten nach langer Zeit"
        return this.maxTaskSwitchOverhead;
    }

    public long getGenericRandomSeed() {
        return this.genericRandomSeed;
    }

}
