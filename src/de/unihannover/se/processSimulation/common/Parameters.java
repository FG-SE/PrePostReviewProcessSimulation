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

package de.unihannover.se.processSimulation.common;

import de.unihannover.se.processSimulation.preCommitPostCommit.DependencyGraphConstellation;
import desmoj.core.dist.BoolDist;
import desmoj.core.dist.NumericalDist;
import desmoj.core.simulator.TimeSpan;

public class Parameters {

    private final NumericalDist<Double> implementationSkillDist;
    private final NumericalDist<Double> reviewSkillDist;
    private final NumericalDist<Double> globalIssueDist;
    private final BoolDist conflictDist;
    private final NumericalDist<Double> implementationTimeDist;
    private final NumericalDist<Double> issuefixTaskOverheadTimeDist;
    private final NumericalDist<Double> reviewRemarkFixTimeDist;
    private final NumericalDist<Double> globalIssueSuspendTimeDist;
    private final NumericalDist<Double> issueAssessmentTimeDist;
    private final NumericalDist<Double> conflictResolutionTimeDist;
    private final NumericalDist<Double> issueActivationTimeDeveloperDist;
    private final NumericalDist<Double> issueActivationTimeCustomerDist;
    private final NumericalDist<Double> planningTimeDist;
    private final NumericalDist<Double> reviewTimeDist;
    private final int numDevelopers;
    private final TimeSpan taskSwitchOverheadAfterOneHour;
    private final TimeSpan maxTaskSwitchOverhead;
    private final BoolDist internalIssueDist;
    private final DependencyGraphConstellation dependencyGraphConstellation;
    private final int boardSearchCutoffLimit;
    private final double taskSwitchTimeIssueFactor;
    private final double fixingIssueRateFactor;
    private final double followUpIssueSpawnProbability;
    private final double reviewFixToTaskFactor;
    private final long genericRandomSeed;

    public Parameters(
                    NumericalDist<Double> implementationSkillDist,
                    NumericalDist<Double> reviewSkillDist,
                    NumericalDist<Double> globalIssueDist,
                    BoolDist conflictDist,
                    NumericalDist<Double> implementationTimeDist,
                    NumericalDist<Double> issuefixTaskOverheadTimeDist,
                    NumericalDist<Double> reviewRemarkFixTimeDist,
                    NumericalDist<Double> globalIssueSuspendTimeDist,
                    NumericalDist<Double> issueAssessmentTimeDist,
                    NumericalDist<Double> conflictResolutionTimeDist,
                    BoolDist internalIssueDist,
                    NumericalDist<Double> issueActivationTimeDeveloperDist,
                    NumericalDist<Double> issueActivationTimeCustomerDist,
                    NumericalDist<Double> planningTimeDist,
                    NumericalDist<Double> reviewTimeDist,
                    int numDevelopers,
                    TimeSpan taskSwitchOverheadAfterOneHour,
                    TimeSpan maxTaskSwitchOverhead,
                    int boardSearchCutoffLimit,
                    double taskSwitchTimeIssueFactor,
                    double fixingIssueRateFactor,
                    double followUpIssueSpawnProbability,
                    double reviewFixToTaskFactor,
                    long genericRandomSeed,
                    DependencyGraphConstellation dependencyGraphConstellation) {
        this.implementationSkillDist = implementationSkillDist;
        this.reviewSkillDist = reviewSkillDist;
        this.globalIssueDist = globalIssueDist;
        this.conflictDist = conflictDist;
        this.implementationTimeDist = implementationTimeDist;
        this.issuefixTaskOverheadTimeDist = issuefixTaskOverheadTimeDist;
        this.reviewRemarkFixTimeDist = reviewRemarkFixTimeDist;
        this.globalIssueSuspendTimeDist = globalIssueSuspendTimeDist;
        this.issueAssessmentTimeDist = issueAssessmentTimeDist;
        this.conflictResolutionTimeDist = conflictResolutionTimeDist;
        this.issueActivationTimeDeveloperDist = issueActivationTimeDeveloperDist;
        this.issueActivationTimeCustomerDist = issueActivationTimeCustomerDist;
        this.planningTimeDist = planningTimeDist;
        this.reviewTimeDist = reviewTimeDist;
        this.numDevelopers = numDevelopers;
        this.taskSwitchOverheadAfterOneHour = taskSwitchOverheadAfterOneHour;
        this.maxTaskSwitchOverhead = maxTaskSwitchOverhead;
        this.dependencyGraphConstellation = dependencyGraphConstellation;
        this.boardSearchCutoffLimit = boardSearchCutoffLimit;
        this.taskSwitchTimeIssueFactor = taskSwitchTimeIssueFactor;
        this.fixingIssueRateFactor = fixingIssueRateFactor;
        this.followUpIssueSpawnProbability = followUpIssueSpawnProbability;
        this.genericRandomSeed = genericRandomSeed;
        this.reviewFixToTaskFactor = reviewFixToTaskFactor;
        this.internalIssueDist = internalIssueDist;
    }

    /**
     * Liefert die Verteilung für den "Entwickler-Skill", gemessen in Issues/Stunde Implementierung.
     */
    public NumericalDist<Double> getImplementationSkillDist() {
        return this.implementationSkillDist;
    }

    /**
     * Liefert die Verteilung für die Wahrscheinlichkeit, einen Issue zu finden (je Entwickler)
     * (1.0 = Issue wird mit Sicherheit gefunden, 0.0 = Issue wird mit Sicherheit nicht gefunden).
     */
    public NumericalDist<Double> getReviewSkillDist() {
        return this.reviewSkillDist;
    }

    /**
     * Liefert die Verteilung für die Wahrscheinlichkeit, einen globalen "Blocker-Issue" einzufügen (je Entwickler)
     * (1.0 = In jeden Task wird ein Global-Issue eingefügt, 0.0 = Global-Issues werden nie eingefügt).
     */
    public NumericalDist<Double> getGlobalIssueDist() {
        return this.globalIssueDist;
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
     * Liefert die Verteilung, aus der die "Analysedauer" von Issuefix-Tasks bezogen wird.
     * Die gesampelten Werte werden als "Stunden" interpretiert. Die Gesamtdauer eines Tasks
     * ergibt sich aus dem Korrekturaufwand (gleiche Verteilung wie bei Review-Remarks) und
     * dem Analyseaufwand. Die Analysedauer ist für die beim Issue-Fixen eingebauten Folgefehler
     * irrelevant.
     */
    public NumericalDist<Double> getIssuefixTaskOverheadTimeDist() {
        return this.issuefixTaskOverheadTimeDist;
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
     * Liefert die Verteilung, aus der Zeit, nach der ein normaler Issue von einem Entwickler gefunden wird (gezählt ab Commit), bezogen wird.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getIssueActivationTimeDeveloperDist() {
        //Both the time for finding internal issues as well as external issues depends mostly on the intensity with which the developers are
        //  dealing with old code in their daily work. Therefore we use the same distribution for these two times.
        return this.issueActivationTimeDeveloperDist;
    }

    /**
     * Liefert die Verteilung, aus der Zeit, nach der ein normaler Issue vom Kunden gefunden wird (gezählt ab Veröffentlichung für Kunden), bezogen wird.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getIssueActivationTimeCustomerDist() {
        return this.issueActivationTimeCustomerDist;
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
     * Liefert die Verteilung, aus der die Zeit bezogen wird, die ein Entwickler durch das Auftreten eines "globalen Issues" während der Implementierung
     * verliert.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getGlobalIssueSuspendTimeDist() {
        return this.globalIssueSuspendTimeDist;
    }

    /**
     * Liefert die Verteilung, aus der die Zeit bezogen wird, die ein Entwickler für die Bewertung eines neu reingekommenen Issues
     * braucht.
     * Die gesampelten Werte werden als "Stunden" interpretiert.
     */
    public NumericalDist<Double> getIssueAssessmentTimeDist() {
        return this.issueAssessmentTimeDist;
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

    public BoolDist getInternalIssueDist() {
        return this.internalIssueDist;
    }

    public int getBoardSearchCutoffLimit() {
        return this.boardSearchCutoffLimit;
    }

    public double getTaskSwitchTimeIssueFactor() {
        return this.taskSwitchTimeIssueFactor;
    }

    public double getFixingIssueRateFactor() {
        return this.fixingIssueRateFactor;
    }

    public double getFollowUpIssueSpawnProbability() {
        return this.followUpIssueSpawnProbability;
    }

    public double getReviewFixToTaskFactor() {
        return this.reviewFixToTaskFactor;
    }

    public long getGenericRandomSeed() {
        return this.genericRandomSeed;
    }

}
