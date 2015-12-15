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
import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.TimeSpan;

/**
 * A helper process that regularly prints some counter's values into a csv file so that this
 * file can be used to do plots over time.
 */
public class Plotter extends PrePostProcess {

    private static final String TASKS_WITH_REVIEW_REMARKS = "tasksWithReviewRemarks";
    private static final String TASKS_READY_FOR_REVIEW = "tasksReadyForReview";
    private static final String BUGS_FOUND_BY_CUSTOMER = "bugsFoundByCustomer";

    private static final String OPEN_STORY_TASKS = "openStoryTasks";
    private static final String OPEN_BUGFIX_TASKS = "openBugfixTasks";
    private static final String FINISHED_STORIES = "finishedStories";
    private static final String STARTED_STORIES = "startedStories";
    private static final String TIME = "time";

    public Plotter(PrePostModel owner) {
        super(owner, "plotter", false);
    }

    @Override
    public void lifeCycle() throws SuspendExecution {
        final Experiment exp = this.getModel().getExperiment();
        final File fileResults = new File(exp.getOutputPath(), exp.getName() + "plotResults.csv");
        final File fileBoard = new File(exp.getOutputPath(), exp.getName() + "plotBoard.csv");
        try (CsvWriter wResults = new CsvWriter(new FileWriter(fileResults));
            CsvWriter wBoard = new CsvWriter(new FileWriter(fileBoard))) {
            wResults.addNumericAttribute(TIME);
            wResults.addNumericAttribute(STARTED_STORIES);
            wResults.addNumericAttribute(FINISHED_STORIES);
            wResults.addNumericAttribute(BUGS_FOUND_BY_CUSTOMER);

            wBoard.addNumericAttribute(TIME);
            wBoard.addNumericAttribute(OPEN_STORY_TASKS);
            wBoard.addNumericAttribute(OPEN_BUGFIX_TASKS);
            wBoard.addNumericAttribute(TASKS_READY_FOR_REVIEW);
            wBoard.addNumericAttribute(TASKS_WITH_REVIEW_REMARKS);

            while (true) {
                final Map<String, Object> dataResults = new HashMap<>();
                dataResults.put(TIME, this.presentTime().getTimeAsDouble(TimeUnit.HOURS));
                dataResults.put(STARTED_STORIES, this.getModel().getStartedStoryCount());
                dataResults.put(FINISHED_STORIES, this.getModel().getFinishedStoryCount());
                dataResults.put(BUGS_FOUND_BY_CUSTOMER, this.getModel().getBugCountFoundByCustomers());
                wResults.writeTuple(dataResults);
                wResults.flush();

                final Map<String, Object> dataBoard = new HashMap<>();
                dataBoard.put(TIME, this.presentTime().getTimeAsDouble(TimeUnit.HOURS));
                dataBoard.put(OPEN_STORY_TASKS, this.getBoard().countOpenStoryTasks());
                dataBoard.put(OPEN_BUGFIX_TASKS, this.getBoard().countOpenBugfixTasks());
                dataBoard.put(TASKS_READY_FOR_REVIEW, this.getBoard().countTasksReadyForReview());
                dataBoard.put(TASKS_WITH_REVIEW_REMARKS, this.getBoard().countTasksWithReviewRemarks());
                wBoard.writeTuple(dataBoard);
                wBoard.flush();

                this.hold(new TimeSpan(16, TimeUnit.HOURS));
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

}
