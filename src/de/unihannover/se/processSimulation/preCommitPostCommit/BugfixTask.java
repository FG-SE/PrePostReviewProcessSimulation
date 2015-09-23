package de.unihannover.se.processSimulation.preCommitPostCommit;

 class BugfixTask extends Task {

    private final Bug bug;

    public BugfixTask(Bug bug) {
        super(bug.getModel(), "bug");
        this.bug = bug;
    }

    public Bug getBug() {
        return this.bug;
    }

    @Override
    public String getMemoryKey() {
        return this.bug.getTask().getMemoryKey();
    }

}
