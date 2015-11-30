package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

class BugfixTask extends Task {

    private final NormalBug bug;
    private final Story cachedStory;

    public BugfixTask(NormalBug bug) {
        super(bug.getModel(), "bug", bug.getModel().getParameters().getBugfixTaskTimeDist().sampleTimeSpan(TimeUnit.HOURS));
        this.bug = bug;
        this.cachedStory = this.bug.getTask().getStory();
        this.cachedStory.registerBug(this);
    }

    public NormalBug getBug() {
        return this.bug;
    }

    @Override
    public String getMemoryKey() {
        return this.cachedStory.getMemoryKey();
    }

    @Override
    public Story getStory() {
        return this.cachedStory;
    }

    @Override
    public List<? extends Task> getPrerequisites() {
        return Collections.emptyList();
    }

    @Override
    protected void handleCommited() {
        this.bug.fix();
    }

    @Override
    protected void handleFinishedTask() {
        this.startLurkingBugsForCustomer();
        this.cachedStory.unregisterBug(this);
        if (!this.cachedStory.isFinished() && this.cachedStory.canBeFinished()) {
            this.cachedStory.finish();
        }
    }

}
