package de.unihannover.se.processSimulation.common;

import de.unihannover.se.processSimulation.preCommitPostCommit.DependencyGraphConstellation;
import desmoj.core.dist.BoolDist;
import desmoj.core.dist.BoolDistConstant;
import desmoj.core.dist.NumericalDist;
import desmoj.core.simulator.TimeSpan;

public class Parameters {

    private final NumericalDist<Double> implementationSkillDist;
    private final NumericalDist<Double> reviewSkillDist;
    private final NumericalDist<Double> globalBugDist;
    private final BoolDist conflictDist;
    private final NumericalDist<Double> implementationTimeDist;
    private final NumericalDist<Double> bugfixTaskTimeDist;
    private final NumericalDist<Double> reviewRemarkFixTimeDist;
    private final NumericalDist<Double> globalBugSuspendTimeDist;
    private final NumericalDist<Double> bugAssessmentTimeDist;
    private final NumericalDist<Double> conflictResolutionTimeDist;
    private final NumericalDist<Double> bugActivationTimeDist;
    private final NumericalDist<Double> planningTimeDist;
    private final NumericalDist<Double> reviewTimeDist;
    private final int numDevelopers;
    private final TimeSpan taskSwitchOverheadAfterOneHour;
    private final TimeSpan maxTaskSwitchOverhead;
    private final BoolDist internalBugDist;
    private final DependencyGraphConstellation dependencyGraphConstellation;
    private final long genericRandomSeed;

    public Parameters(
                    NumericalDist<Double> implementationSkillDist,
                    NumericalDist<Double> reviewSkillDist,
                    NumericalDist<Double> globalBugDist,
                    BoolDist conflictDist,
                    NumericalDist<Double> implementationTimeDist,
                    NumericalDist<Double> bugfixTaskTimeDist,
                    NumericalDist<Double> reviewRemarkFixTimeDist,
                    NumericalDist<Double> globalBugSuspendTimeDist,
                    NumericalDist<Double> bugAssessmentTimeDist,
                    NumericalDist<Double> conflictResolutionTimeDist,
                    NumericalDist<Double> bugActivationTimeDist,
                    NumericalDist<Double> planningTimeDist,
                    NumericalDist<Double> reviewTimeDist,
                    int numDevelopers,
                    TimeSpan taskSwitchOverheadAfterOneHour,
                    TimeSpan maxTaskSwitchOverhead,
                    long genericRandomSeed,
                    DependencyGraphConstellation dependencyGraphConstellation) {
        this.implementationSkillDist = implementationSkillDist;
        this.reviewSkillDist = reviewSkillDist;
        this.globalBugDist = globalBugDist;
        this.conflictDist = conflictDist;
        this.implementationTimeDist = implementationTimeDist;
        this.bugfixTaskTimeDist = bugfixTaskTimeDist;
        this.reviewRemarkFixTimeDist = reviewRemarkFixTimeDist;
        this.globalBugSuspendTimeDist = globalBugSuspendTimeDist;
        this.bugAssessmentTimeDist = bugAssessmentTimeDist;
        this.conflictResolutionTimeDist = conflictResolutionTimeDist;
        this.bugActivationTimeDist = bugActivationTimeDist;
        this.planningTimeDist = planningTimeDist;
        this.reviewTimeDist = reviewTimeDist;
        this.numDevelopers = numDevelopers;
        this.taskSwitchOverheadAfterOneHour = taskSwitchOverheadAfterOneHour;
        this.maxTaskSwitchOverhead = maxTaskSwitchOverhead;
        this.dependencyGraphConstellation = dependencyGraphConstellation;
        this.genericRandomSeed = genericRandomSeed;
        //TODO auslagern
        this.internalBugDist = new BoolDistConstant(reviewTimeDist.getModel(), "internalBugDist", false, true, true);
    }

    /**
     * Liefert die Verteilung für den "Entwickler-Skill", gemessen in Bugs/Stunde Implementierung.
     */
    public NumericalDist<Double> getImplementationSkillDist() {
        return this.implementationSkillDist;
    }

    /**
     * Liefert die Verteilung für die Wahrscheinlichkeit, einen Bug zu finden (je Entwickler)
     * (1.0 = Bug wird mit Sicherheit gefunden, 0.0 = Bug wird mit Sicherheit nicht gefunden).
     */
    public NumericalDist<Double> getReviewSkillDist() {
        return this.reviewSkillDist;
    }

    /**
     * Liefert die Verteilung für die Wahrscheinlichkeit, einen globalen "Blocker-Bug" einzufügen (je Entwickler)
     * (1.0 = In jeden Task wird ein Global-Bug eingefügt, 0.0 = Global-Bugs werden nie eingefügt).
     */
    public NumericalDist<Double> getGlobalBugDist() {
        return this.globalBugDist;
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
        return this.numDevelopers;
    }

    /**
     * Liefert die Verteilung, aus der die Implementierungsdauer von Story-Tasks bezogen wird.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getImplementationTimeDist() {
        return this.implementationTimeDist;
    }

    /**
     * Liefert die Verteilung, aus der die Implementierungsdauer von Bugfix-Tasks bezogen wird.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getBugfixTaskTimeDist() {
        return this.bugfixTaskTimeDist;
    }

    /**
     * Liefert die Verteilung für die Zeit für das Fixen EINER Review-Anmerkung.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getReviewRemarkFixDist() {
        return this.reviewRemarkFixTimeDist;
    }

    /**
     * Liefert die Verteilung, aus der benötigte Zeit für das Review eines Tasks bezogen wird.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getReviewTimeDist() {
        return this.reviewTimeDist;
    }

    /**
     * Liefert die Verteilung, aus der benötigte Zeit für die Planung einer Story bezogen wird.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getPlanningTimeDist() {
        return this.planningTimeDist;
    }

    /**
     * Liefert die Verteilung, aus der Zeit, nach der ein normaler Bug von einem Entwickler gefunden wird (gezählt ab Commit), bezogen wird.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getBugActivationTimeDeveloperDist() {
        //TODO unterschiedliche Verteilungen Kunde/Entwickler
        return this.bugActivationTimeDist;
    }

    /**
     * Liefert die Verteilung, aus der Zeit, nach der ein normaler Bug vom Kunden gefunden wird (gezählt ab Veröffentlichung für Kunden), bezogen wird.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getBugActivationTimeCustomerDist() {
        //TODO unterschiedliche Verteilungen Kunde/Entwickler
        return this.bugActivationTimeDist;
    }

    /**
     * Liefert die Verteilung, aus der die für das Auflösen eines Konflikts mit einem oder mehreren parallel bearbeiteten Tasks
     * benötigte Zeit bezogen wird.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getConflictResolutionTimeDist() {
        return this.conflictResolutionTimeDist;
    }

    /**
     * Liefert die Verteilung, aus der die Zeit bezogen wird, die ein Entwickler durch das Auftreten eines "globalen Bugs" während der Implementierung
     * verliert.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getGlobalBugSuspendTimeDist() {
        return this.globalBugSuspendTimeDist;
    }

    /**
     * Liefert die Verteilung, aus der die Zeit bezogen wird, die ein Entwickler für die Bewertung eines neu reingekommenen Bugs
     * braucht.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getBugAssessmentTimeDist() {
        return this.bugAssessmentTimeDist;
    }

    /**
     * Taskwechsel-Overhead der angesetzt wird, nachdem ein Entwickler für eine Stunde von einer anderen Aufgabe unterbrochen wurde.
     * Wird zusammen mit {@link #getMaxTaskSwitchOverhead()} verwendet, um die Parameter für die Ebbinghaussche Vergessenskurve zu bestimmen.
     */
    public TimeSpan getTaskSwitchOverheadAfterOneHourInterruption() {
        return this.taskSwitchOverheadAfterOneHour;
    }

    /**
     * Obergrenze für den angesetzten Taskwechsel-Overhead.
     * Wird zusammen mit {@link #getTaskSwitchOverheadAfterFiveMinuteInterruption()} verwendet, um die Parameter für die
     * Ebbinghaussche Vergessenskurve zu bestimmen.
     */
    public TimeSpan getMaxTaskSwitchOverhead() {
        //TODO: besser als Anteil des Gesamtaufwands der Story (abhängig von der Anzahl Story Points) angeben
        //TODO: vielleicht auch noch unterscheiden zwischen "Zeit für komplettes Neu-Einarbeiten" und "Zeit für Wieder-Einarbeiten nach langer Zeit"
        return this.maxTaskSwitchOverhead;
    }

    public DependencyGraphConstellation getDependencyGraphConstellation() {
        return this.dependencyGraphConstellation;
    }

    public BoolDist getInternalBugDist() {
        return this.internalBugDist;
    }

    public long getGenericRandomSeed() {
        return this.genericRandomSeed;
    }

}
