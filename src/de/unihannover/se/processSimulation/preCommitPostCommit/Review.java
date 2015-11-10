package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.List;

class Review {

    private final List<Bug> remarks;

    public Review(List<? extends Bug> foundBugs) {
        this.remarks = new ArrayList<>(foundBugs);
    }

    public List<Bug> getRemarks() {
        return this.remarks;
    }

    public void addRemark(Bug bug) {
        if (!this.remarks.contains(bug)) {
            this.remarks.add(bug);
        }
    }

}
