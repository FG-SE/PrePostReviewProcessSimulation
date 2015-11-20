package de.unihannover.se.processSimulation.preCommitPostCommit;

import desmoj.core.simulator.TimeSpan;

class NormalBug extends Bug {

    public enum BugType {
        DEVELOPER_ONLY,
        DEVELOPER_AND_CUSTOMER
    }

    private final Task task;
    private final BugType type;
    private final TimeSpan assessmentTime;
    private final TimeSpan activationTimeDeveloper;
    private final TimeSpan activationTimeCustomer;
    private final long fixTaskRandomnessSeed;

    public NormalBug(Task task, BugType type, Randomness randomness) {
        super(task.getModel(), "bug", randomness);
        this.task = task;
        this.type = type;
        this.assessmentTime = randomness.sampleTimeSpan(this.getModel().getParameters().getBugAssessmentTimeDist());
        this.activationTimeDeveloper = randomness.sampleTimeSpan(this.getModel().getParameters().getBugActivationTimeDeveloperDist());
        this.activationTimeCustomer = randomness.sampleTimeSpan(this.getModel().getParameters().getBugActivationTimeCustomerDist());
        this.fixTaskRandomnessSeed = randomness.sampleLong();
    }

    @Override
    protected TimeSpan getActivationTimeForDevelopers() {
        return this.activationTimeDeveloper;
    }

    @Override
    protected TimeSpan getActivationTimeForCustomers() {
        if (this.type == BugType.DEVELOPER_AND_CUSTOMER) {
            return this.activationTimeCustomer;
        } else {
            return null;
        }
    }

    @Override
    protected void becomeVisible() {
        this.getBoard().addUnassessedBug(this);
    }

    public Task getTask() {
        return this.task;
    }

    public TimeSpan getAssessmentTime() {
        return this.assessmentTime;
    }

    public Randomness getRandomnessForTask() {
        return new Randomness(this.fixTaskRandomnessSeed);
    }

}
