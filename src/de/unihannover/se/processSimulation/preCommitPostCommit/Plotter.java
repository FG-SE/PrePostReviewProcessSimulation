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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import co.paralleluniverse.fibers.SuspendExecution;
import de.unihannover.se.processSimulation.dataGenerator.CsvWriter;
import desmoj.core.simulator.TimeSpan;

/**
 * Hilfsklasse, die regelmäßig die aktuellen Queue-Längen in eine Datei ausgibt.
 * Auf Basis dieser Datei kann dann später z.B. eine grafische Darstellung erstellt werden.
 */
public class Plotter extends RealModelProcess {

    private static final String TASKS_WITH_REVIEW_REMARKS = "tasksWithReviewRemarks";
    private static final String TASKS_READY_FOR_REVIEW = "tasksReadyForReview";
    private static final String OPEN_STORY_TASKS = "openStoryTasks";
    private static final String OPEN_BUGFIX_TASKS = "openBugfixTasks";
    private static final String REMAINING_BUGS = "remainingBugs";
    private static final String FINISHED_STORIES = "finishedStories";
    private static final String STARTED_STORIES = "startedStories";
    private static final String TIME = "time";

    public Plotter(RealProcessingModel owner) {
        super(owner, "plotter");
    }

    @Override
    public void lifeCycle() throws SuspendExecution {
        final String filename = this.getModel().getExperiment().getName() + "plot.csv";
        final File file = new File(this.getModel().getExperiment().getOutputPath(), filename);
        try (CsvWriter w = new CsvWriter(new FileWriter(file))) {
            w.addNumericAttribute(TIME);
            w.addNumericAttribute(STARTED_STORIES);
            w.addNumericAttribute(FINISHED_STORIES);
            w.addNumericAttribute(REMAINING_BUGS);
            w.addNumericAttribute(OPEN_STORY_TASKS);
            w.addNumericAttribute(OPEN_BUGFIX_TASKS);
            w.addNumericAttribute(TASKS_READY_FOR_REVIEW);
            w.addNumericAttribute(TASKS_WITH_REVIEW_REMARKS);
            while (true) {
                final Map<String, Object> data = new HashMap<>();
                data.put(TIME, this.presentTime().getTimeAsDouble(TimeUnit.HOURS));
                data.put(STARTED_STORIES, this.getModel().getStartedStoryCount());
                data.put(FINISHED_STORIES, this.getModel().getFinishedStoryCount());
                data.put(REMAINING_BUGS, this.getModel().getBugCountFoundByCustomers());
                data.put(OPEN_STORY_TASKS, this.getModel().getBoard().countOpenStoryTasks());
                data.put(OPEN_BUGFIX_TASKS, this.getModel().getBoard().countOpenBugfixTasks());
                data.put(TASKS_READY_FOR_REVIEW, this.getModel().getBoard().countTasksReadyForReview());
                data.put(TASKS_WITH_REVIEW_REMARKS, this.getModel().getBoard().countTasksWithReviewRemarks());
                w.writeTuple(data);
                this.hold(new TimeSpan(16, TimeUnit.HOURS));
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

}
