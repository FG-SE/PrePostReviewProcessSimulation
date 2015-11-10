package de.unihannover.se.processSimulation.dataGenerator;

public class CombinedResult {

    private final ExperimentResult resultPost;
    private final ExperimentResult resultPre;

    public CombinedResult(ExperimentResult resultPost, ExperimentResult resultPre) {
        this.resultPost = resultPost;
        this.resultPre = resultPre;
    }

    public long getFinishedStoryPointDiff() {
        return this.resultPost.getFinishedStoryPoints() - this.resultPre.getFinishedStoryPoints();
    }

}
