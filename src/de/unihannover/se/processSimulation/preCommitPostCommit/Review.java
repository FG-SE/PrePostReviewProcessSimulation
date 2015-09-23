package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.List;

class Review {

    private final List<Bug> remarks;

    public Review(List<Bug> foundBugs) {
        this.remarks = foundBugs;
    }

    public List<Bug> getRemarks() {
        return this.remarks;
    }

}
