package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.Collections;
import java.util.List;

class BugfixTask extends Task {

    private final NormalBug bug;

    public BugfixTask(NormalBug bug) {
        super(bug.getModel(), "bug");
        this.bug = bug;
    }

    public NormalBug getBug() {
        return this.bug;
    }

    @Override
    public String getMemoryKey() {
        return this.bug.getTask().getMemoryKey();
    }

    @Override
    public List<? extends Task> getPrerequisites() {
        return Collections.emptyList();
    }

}
