package de.tntinteractive.processSimulation.preCommitPostCommitPrototype;

import java.util.Collections;

public class BugfixTask extends Task {

    public BugfixTask(DevelopmentProcessModel owner) {
        super(owner, "Bugfix", owner.getParameters().getBugfixTime(), Collections.emptyList());
    }

}
