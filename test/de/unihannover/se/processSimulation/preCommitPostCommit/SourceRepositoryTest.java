/**
    This file is part of LUH PrePostReview Process Simulation.

    LUH PrePostReview Process Simulation is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    LUH PrePostReview Process Simulation is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with LUH PrePostReview Process Simulation. If not, see <http://www.gnu.org/licenses/>.
 */

package de.unihannover.se.processSimulation.preCommitPostCommit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import de.unihannover.se.processSimulation.preCommitPostCommit.SourceRepository.SourceRepositoryDependencies;
import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.TimeInstant;

public class SourceRepositoryTest {

    static {
        Experiment.setEpsilon(TimeUnit.MINUTES);
        Experiment.setReferenceUnit(TimeUnit.HOURS);
    }

    private static final class StubSourceRepositoryDependencies implements SourceRepositoryDependencies {

        private long time;
        private final boolean conflict;
        private final StringBuilder traces;

        public StubSourceRepositoryDependencies(boolean conflict) {
            this.conflict = conflict;
            this.traces = new StringBuilder();
        }

        @Override
        public TimeInstant presentTime() {
            return new TimeInstant(this.time, TimeUnit.MINUTES);
        }

        @Override
        public boolean sampleConflictDist() {
            return this.conflict;
        }

        @Override
        public void sendTraceNote(String description) {
            this.traces.append(description).append('\n');
        }

        public void incrementTime() {
            this.time++;
        }

        public String getTraces() {
            return this.traces.toString();
        }

    }

    private static final class StubTask {
        private final String name;

        public StubTask(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private static StubSourceRepositoryDependencies createDepsNoConflicts() {
        return new StubSourceRepositoryDependencies(false);
    }

    private static StubSourceRepositoryDependencies createDepsConflictsAlways() {
        return new StubSourceRepositoryDependencies(true);
    }

    @Test
    public void testSimpleSingleWork() {
        final StubSourceRepositoryDependencies deps = createDepsNoConflicts();
        final SourceRepository<StubTask> r = new SourceRepository<>(deps);
        final StubTask t1 = new StubTask("t1");
        r.startWork(t1);
        deps.incrementTime();
        final boolean success = r.tryCommit(t1);
        assertTrue(success);
        assertEquals("", deps.getTraces());
    }

    @Test
    public void testNoConflictWhenWorkingAlone() {
        final StubSourceRepositoryDependencies deps = createDepsConflictsAlways();
        final SourceRepository<StubTask> r = new SourceRepository<>(deps);
        final StubTask t1 = new StubTask("t1");
        r.startWork(t1);
        deps.incrementTime();
        final boolean success = r.tryCommit(t1);
        assertTrue(success);
    }

    @Test
    public void testNoConflictInSameInstant() {
        final StubSourceRepositoryDependencies deps = createDepsNoConflicts();
        final SourceRepository<StubTask> r = new SourceRepository<>(deps);
        final StubTask t1 = new StubTask("t1");
        r.startWork(t1);
        final boolean success = r.tryCommit(t1);
        assertTrue(success);
    }

    @Test
    public void testNoConflictInSameInstant2() {
        final StubSourceRepositoryDependencies deps = createDepsConflictsAlways();
        final SourceRepository<StubTask> r = new SourceRepository<>(deps);
        final StubTask t1 = new StubTask("t1");
        r.startWork(t1);
        final boolean success = r.tryCommit(t1);
        assertTrue(success);
    }

    @Test
    public void testConflictWithOtherTask() {
        final StubSourceRepositoryDependencies deps = createDepsConflictsAlways();
        final SourceRepository<StubTask> r = new SourceRepository<>(deps);
        final StubTask t1 = new StubTask("t1");
        final StubTask t2 = new StubTask("t2");
        r.startWork(t1);
        deps.incrementTime();
        r.startWork(t2);
        deps.incrementTime();
        final boolean success1 = r.tryCommit(t1);
        assertTrue(success1);
        assertEquals("", deps.getTraces());
        deps.incrementTime();
        final boolean success2 = r.tryCommit(t2);
        assertFalse(success2);
        assertEquals("conflict between t2 and t1\n",
                        deps.getTraces());
    }

    @Test
    public void testNoConflictWithSequentialTasks() {
        final StubSourceRepositoryDependencies deps = createDepsConflictsAlways();
        final SourceRepository<StubTask> r = new SourceRepository<>(deps);

        final StubTask t1 = new StubTask("t1");
        r.startWork(t1);
        deps.incrementTime();
        final boolean success1 = r.tryCommit(t1);
        assertTrue(success1);
        deps.incrementTime();

        final StubTask t2 = new StubTask("t2");
        r.startWork(t2);
        deps.incrementTime();
        final boolean success2 = r.tryCommit(t2);
        assertTrue(success2);
        deps.incrementTime();

        final StubTask t3 = new StubTask("t3");
        r.startWork(t3);
        deps.incrementTime();
        final boolean success3 = r.tryCommit(t3);
        assertTrue(success3);
    }

    @Test
    public void testRestartAfterConflict() {
        final StubSourceRepositoryDependencies deps = createDepsConflictsAlways();
        final SourceRepository<StubTask> r = new SourceRepository<>(deps);
        final StubTask t1 = new StubTask("t1");
        final StubTask t2 = new StubTask("t2");
        r.startWork(t1);
        deps.incrementTime();
        r.startWork(t2);
        deps.incrementTime();
        final boolean success1 = r.tryCommit(t1);
        assertTrue(success1);
        deps.incrementTime();
        final boolean success2 = r.tryCommit(t2);
        assertFalse(success2);
        r.restartWork(t2);
        deps.incrementTime();
        final boolean successAfterRestart = r.tryCommit(t2);
        assertTrue(successAfterRestart);
    }

    @Test
    public void testThreeTasksAndSavedCommits() {
        final StubSourceRepositoryDependencies deps = createDepsConflictsAlways();
        final SourceRepository<StubTask> r = new SourceRepository<>(deps);

        assertEquals(0, r.countRemainingSavedCommits());
        final StubTask t1 = new StubTask("t1");
        r.startWork(t1);
        deps.incrementTime();
        final StubTask t2 = new StubTask("t2");
        r.startWork(t2);
        deps.incrementTime();
        final boolean success2 = r.tryCommit(t2);
        assertTrue(success2);
        assertEquals(1, r.countRemainingSavedCommits());
        deps.incrementTime();
        final StubTask t3 = new StubTask("t3");
        r.startWork(t3);
        deps.incrementTime();
        final boolean success1 = r.tryCommit(t1);
        assertFalse(success1);
        assertEquals(1, r.countRemainingSavedCommits());
        r.restartWork(t1);
        deps.incrementTime();
        final boolean success1a = r.tryCommit(t1);
        assertTrue(success1a);
        assertEquals(1, r.countRemainingSavedCommits());
        deps.incrementTime();
        final boolean success3 = r.tryCommit(t3);
        assertFalse(success3);
        assertEquals(1, r.countRemainingSavedCommits());
        r.restartWork(t3);
        deps.incrementTime();
        final boolean success3a = r.tryCommit(t3);
        assertTrue(success3a);
        assertEquals(0, r.countRemainingSavedCommits());
        assertEquals("conflict between t1 and t2\n"
                        + "conflict between t3 and t1\n",
                        deps.getTraces());
    }

    @Test
    public void testSavedCommitsWithFourTasks() {
        final StubSourceRepositoryDependencies deps = createDepsNoConflicts();
        final SourceRepository<StubTask> r = new SourceRepository<>(deps);

        final StubTask t1 = new StubTask("t1");
        r.startWork(t1);
        deps.incrementTime();
        assertEquals(0, r.countRemainingSavedCommits());

        final StubTask t2 = new StubTask("t2");
        r.startWork(t2);
        deps.incrementTime();

        final boolean success1 = r.tryCommit(t1);
        deps.incrementTime();
        assertTrue(success1);
        assertEquals(1, r.countRemainingSavedCommits());

        final StubTask t3 = new StubTask("t3");
        r.startWork(t3);
        deps.incrementTime();

        final StubTask t4 = new StubTask("t4");
        r.startWork(t4);
        deps.incrementTime();

        final boolean success4 = r.tryCommit(t4);
        deps.incrementTime();
        assertTrue(success4);
        assertEquals(2, r.countRemainingSavedCommits());

        final boolean success3 = r.tryCommit(t3);
        deps.incrementTime();
        assertTrue(success3);
        assertEquals(3, r.countRemainingSavedCommits());

        final boolean success2 = r.tryCommit(t2);
        deps.incrementTime();
        assertTrue(success2);
        assertEquals(0, r.countRemainingSavedCommits());
    }

    @Test
    public void testWorkMultipleTimesAtTheSameTask() {
        final StubSourceRepositoryDependencies deps = createDepsNoConflicts();
        final SourceRepository<StubTask> r = new SourceRepository<>(deps);

        final StubTask t1 = new StubTask("t1");
        r.startWork(t1);
        deps.incrementTime();
        final boolean successFirstTime = r.tryCommit(t1);
        assertTrue(successFirstTime);
        deps.incrementTime();

        r.startWork(t1);
        deps.incrementTime();
        final boolean successSecondTime = r.tryCommit(t1);
        assertTrue(successSecondTime);
    }

    @Test
    public void testWorkMultipleTimesAtTheSameTaskWithAnotherTaskParallel() {
        final StubSourceRepositoryDependencies deps = createDepsConflictsAlways();
        final SourceRepository<StubTask> r = new SourceRepository<>(deps);

        final StubTask t1 = new StubTask("t1");
        r.startWork(t1);

        final StubTask t2 = new StubTask("t2");
        r.startWork(t2);
        deps.incrementTime();
        final boolean success2FirstTime = r.tryCommit(t2);
        assertTrue(success2FirstTime);
        deps.incrementTime();

        final boolean success1FirstTime = r.tryCommit(t1);
        assertFalse(success1FirstTime);

        r.startWork(t2);
        deps.incrementTime();
        final boolean success2SecondTime = r.tryCommit(t2);
        assertTrue(success2SecondTime);

        final boolean success1SecondTime = r.tryCommit(t1);
        assertFalse(success1SecondTime);

        assertEquals("conflict between t1 and t2\n"
                        + "conflict between t1 and t2\n",
                        deps.getTraces());
    }
}
