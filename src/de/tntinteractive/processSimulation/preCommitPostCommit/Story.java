package de.tntinteractive.processSimulation.preCommitPostCommit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeSpan;

public class Story extends DevelopmentSimProcess {

    private final TimeSpan planningTime;

    private TimeInstant startTime;
    private final List<Developer> planningDevelopers = new ArrayList<>();
    private final List<StoryTask> tasks = new ArrayList<>();

    public Story(DevelopmentProcessModel owner, String name) {
        super(owner, name);
        this.planningTime = owner.getParameters().getPlanningTime();
    }

    public void startPlanning(Developer developer, DevelopmentProcessModel m) {
        assert this.planningDevelopers.isEmpty();
        assert this.tasks.isEmpty();
        this.startTime = m.presentTime();
        this.planningDevelopers.add(developer);
        this.activate();
    }

    public void joinPlanning(Developer developer) {
        assert !this.planningDevelopers.isEmpty();
        assert this.tasks.isEmpty();
        developer.sendTraceNote("joins planning of " + this);
        this.planningDevelopers.add(developer);
    }

    @Override
    public void lifeCycle() {
        this.hold(this.planningTime);
        this.endPlanning();
    }

    private void endPlanning() {
        //TODO Tasks unterschiedlich erstellen
        final StoryTask s1 = new StoryTask(this, Collections.emptyList());
        this.tasks.add(s1);
        this.tasks.add(new StoryTask(this, Collections.singletonList(s1)));
        this.tasks.add(new StoryTask(this, Collections.singletonList(s1)));

        this.getBoard().addPlannedStory(this);

        for (final Developer dev : this.planningDevelopers) {
            dev.activate();
        }
        this.planningDevelopers.clear();
    }

    public List<StoryTask> getTasks() {
        return this.tasks;
    }

    public boolean allTasksFinished() {
        for (final Task t : this.getTasks()) {
            if (!t.isFinished()) {
                return false;
            }
        }
        return true;
    }

    public TimeInstant getStartTime() {
        return this.startTime;
    }

    public int getStoryPoints() {
        //der Einfachheit halber wird davon ausgegangen, dass Story Points geschaetzter Aufwand in Stunden sind
        int hours = 0;
        for (final Task t : this.getTasks()) {
            hours += t.getEstimatedDuration().getTimeRounded(TimeUnit.HOURS);
        }
        return hours;
    }

}
