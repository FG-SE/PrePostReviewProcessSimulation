package de.tntinteractive.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.List;

public class Review {

    private final Developer reviewer;
    private final List<Bug> remarks;

    public Review(Developer reviewer) {
        this.reviewer = reviewer;
        this.remarks = new ArrayList<>();
    }

    public Developer getReviewer() {
        return this.reviewer;
    }

    public void perform(List<Bug> lurkingBugs) {
        for (final Bug b : lurkingBugs) {
            if (this.reviewer.findsBugInReview(b)) {
                this.remarks.add(b);
            }
        }
    }

    public boolean hasRemarks() {
        return !this.remarks.isEmpty();
    }

    public List<Bug> getRemarks() {
        return this.remarks;
    }

}
