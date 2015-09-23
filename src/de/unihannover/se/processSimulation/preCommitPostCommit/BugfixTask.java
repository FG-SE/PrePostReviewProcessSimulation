package de.unihannover.se.processSimulation.preCommitPostCommit;

 class BugfixTask extends Task {

    public BugfixTask(Bug bug) {
        super(bug.getModel(), "bug");
    }

}
