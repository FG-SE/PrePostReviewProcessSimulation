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

package de.unihannover.se.processSimulation.interactive;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import com.google.common.io.Files;

import de.unihannover.se.processSimulation.common.ReviewMode;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory.ParameterType;
import de.unihannover.se.processSimulation.dataGenerator.DataGenerator;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentResult;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRun;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRun.ExperimentRunSummary;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRun.SingleRunCallback;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRunSettings;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRunSettings.ExperimentRunParameters;
import de.unihannover.se.processSimulation.dataGenerator.MedianWithConfidenceInterval;

/**
 * Main class of the interactive web-based simulation view.
 */
public class ServerMain extends AbstractHandler {

    private static final String PARAMS_PROPERTIES = "params.properties";
    private final AtomicInteger requestIdCounter = new AtomicInteger();

    @Override
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
        throws IOException, ServletException {

        baseRequest.setHandled(true);

        final Matcher detailsMainMatcher = Pattern.compile("/details/([0-9]+)/([0-9]+)/overview").matcher(target);
        final Matcher plotImageMatcher = Pattern.compile("/details/([0-9]+)/([0-9]+)/([A-Z_]+)plot.png").matcher(target);
        final Matcher detailsFileMatcher = Pattern.compile("/details/([0-9]+)/([0-9]+)/(.+)").matcher(target);

        if (target.equals("/")) {
            final int currentRequestId = this.getNewRequestId();
            this.handleMainPage(request, response, currentRequestId);
        } else if (detailsMainMatcher.matches()) {
            this.handleDetailsPage(request, response,
                            Integer.parseInt(detailsMainMatcher.group(1)), Integer.parseInt(detailsMainMatcher.group(2)));
        } else if (plotImageMatcher.matches()) {
            this.handlePlotImage(request, response,
                            Integer.parseInt(plotImageMatcher.group(1)), Integer.parseInt(plotImageMatcher.group(2)), plotImageMatcher.group(3));
        } else if (detailsFileMatcher.matches()) {
            this.handleDetailFileRequest(request, response,
                            Integer.parseInt(detailsFileMatcher.group(1)), Integer.parseInt(detailsFileMatcher.group(2)), detailsFileMatcher.group(3));
        }
    }

    private int getNewRequestId() {
        int requestId;
        do {
            requestId = this.requestIdCounter.getAndIncrement();
        } while (this.getRequestDir(requestId).exists());
        return requestId;
    }

    private void handleMainPage(HttpServletRequest request, HttpServletResponse response, int requestId) throws IOException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        final PrintWriter w = response.getWriter();
        this.printHeader(w);
        w.println("<h1>Pre/Post commit review comparison - Process simulation</h1>");
        final BulkParameterFactory f = this.getParameters(w, request);
        this.saveParameters(requestId, f);
        final ExperimentRunSettings s = this.getExperimentSettings(w, request);
        this.printInputParameters(w, f, s);
        if (this.shallSimulate(request)) {
            this.simulateAndPrintOutput(w, f, s, requestId);
        }
        this.printFooter(w);
    }

    private void saveParameters(int requestId, BulkParameterFactory f) throws IOException {
        final File requestDir = this.getRequestDir(requestId);
        requestDir.mkdirs();

        final Properties p = this.parametersToProperties(f);
        try (FileOutputStream out = new FileOutputStream(new File(requestDir, PARAMS_PROPERTIES))) {
            p.store(out, "Params for request id " + requestId);
        }
    }

    private File getRequestDir(int requestId) {
        return new File("webguiWorkdir", Integer.toString(requestId));
    }

    private Properties parametersToProperties(BulkParameterFactory f) {
        final Properties ret = new Properties();
        for (final ParameterType type : ParameterType.values()) {
            ret.setProperty(type.name(), f.getParam(type).toString());
        }
        return ret;
    }

    private void handleDetailsPage(HttpServletRequest request, HttpServletResponse response,
                    int requestId, int runNbr) throws IOException {

        final BulkParameterFactory f = this.createRunDirectoryIfMissing(requestId, runNbr);

        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        final PrintWriter w = response.getWriter();
        w.println("<h2>Details for run " + runNbr + "</h2>");
        w.println("<h3>Parameters</h3>");
        for (final ParameterType type : ParameterType.values()) {
            w.println(type + ": " + f.getParam(type) +  "<br/>");
        }
        w.println("Seed: " + f.getSeed());

        w.println("<h3>No review</h3>");
        w.println("<img src=\"NO_REVIEWplot.png\" /><br/>");
        w.println("<a href=\"ExperimentNO_REVIEW_run_report.html\">Report</a><br/>");
        w.println("<a href=\"ExperimentNO_REVIEW_run_trace.html\">Trace</a><br/>");
        w.println("<a href=\"ExperimentNO_REVIEW_run_error.html\">Error log</a><br/>");
        w.println("<a href=\"ExperimentNO_REVIEW_run_debug.html\">Debug</a><br/>");

        w.println("<h3>Pre commit review</h3>");
        w.println("<img src=\"PRE_COMMITplot.png\" /><br/>");
        w.println("<a href=\"ExperimentPRE_COMMIT_run_report.html\">Report</a><br/>");
        w.println("<a href=\"ExperimentPRE_COMMIT_run_trace.html\">Trace</a><br/>");
        w.println("<a href=\"ExperimentPRE_COMMIT_run_error.html\">Error log</a><br/>");
        w.println("<a href=\"ExperimentPRE_COMMIT_run_debug.html\">Debug</a><br/>");

        w.println("<h3>Post commit review</h3>");
        w.println("<img src=\"POST_COMMITplot.png\" /><br/>");
        w.println("<a href=\"ExperimentPOST_COMMIT_run_report.html\">Report</a><br/>");
        w.println("<a href=\"ExperimentPOST_COMMIT_run_trace.html\">Trace</a><br/>");
        w.println("<a href=\"ExperimentPOST_COMMIT_run_error.html\">Error log</a><br/>");
        w.println("<a href=\"ExperimentPOST_COMMIT_run_debug.html\">Debug</a><br/>");
    }

    private BulkParameterFactory createRunDirectoryIfMissing(int requestId, int runNbr) throws IOException {
        BulkParameterFactory f = this.loadParameters(requestId);
        for (int i = 0; i < runNbr; i++) {
            f = f.copyWithChangedSeed();
        }

        final File runDirectory = this.getRunDir(requestId, runNbr);
        if (!runDirectory.exists()) {
            runDirectory.mkdir();

            DataGenerator.runExperiment(f, ReviewMode.NO_REVIEW, runDirectory, "run");
            DataGenerator.runExperiment(f, ReviewMode.PRE_COMMIT, runDirectory, "run");
            DataGenerator.runExperiment(f, ReviewMode.POST_COMMIT, runDirectory, "run");
        }

        return f;
    }

    private File getRunDir(int requestId, int runNbr) {
        return new File(this.getRequestDir(requestId), Integer.toString(runNbr));
    }

    private BulkParameterFactory loadParameters(int requestId) throws IOException {
        final File requestDir = this.getRequestDir(requestId);
        final File paramsFile = new File(requestDir, PARAMS_PROPERTIES);
        try (FileInputStream in = new FileInputStream(paramsFile)) {
            final Properties properties = new Properties();
            properties.load(in);

            BulkParameterFactory ret = BulkParameterFactory.forCommercial();
            for (final String name : properties.stringPropertyNames()) {
                final ParameterType type = ParameterType.valueOf(name);
                ret = ret.copyWithChangedParam(type, type.parse(properties.getProperty(name)));
            }
            return ret;
        }
    }

    private void handlePlotImage(HttpServletRequest request, HttpServletResponse response,
                    int requestId, int runNbr, String reviewType) throws IOException {

        final DefaultCategoryDataset dataset = this.loadDatasetFromCsv(new File(this.getRunDir(requestId, runNbr),
                        "Experiment" + reviewType + "_runplot.csv"));
        response.setContentType("image/png");
        final JFreeChart chart = ChartFactory.createLineChart("", "time", "count", dataset);
        final BufferedImage image = chart.createBufferedImage(800, 500);
        ImageIO.write(image, "PNG", response.getOutputStream());
    }

    private DefaultCategoryDataset loadDatasetFromCsv(File csvFile) throws IOException {
        try (BufferedReader r = new BufferedReader(new FileReader(csvFile))) {
            final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            final String[] columnNames = r.readLine().split(";");
            String line;
            while ((line = r.readLine()) != null) {
                final String[] values = line.split(";");
                final Double time = Double.valueOf(values[0]);
                for (int i = 1; i < values.length; i++) {
                    dataset.addValue(Double.parseDouble(values[i]), columnNames[i], time);
                }
            }
            return dataset;
        }
    }

    private void handleDetailFileRequest(HttpServletRequest request, HttpServletResponse response,
                    int requestId, int runNbr, String filename) throws IOException {
        final File file = new File(this.getRunDir(requestId, runNbr), filename);

        response.setContentType("text/html;charset=utf-8");
        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().println("File not found: " + filename);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            Files.copy(file, response.getOutputStream());
        }
    }

    private boolean shallSimulate(HttpServletRequest request) {
        return request.getParameter(ExperimentRunParameters.MIN_RUNS.name()) != null;
    }

    private BulkParameterFactory getParameters(PrintWriter w, HttpServletRequest request) {
        BulkParameterFactory f = BulkParameterFactory.forCommercial();
        for (final ParameterType t : ParameterType.values()) {
            final String param = request.getParameter(t.name());
            if (param != null && !param.isEmpty()) {
                try {
                    f = f.copyWithChangedParam(t, t.parse(param));
                } catch (final RuntimeException e) {
                    this.printParseError(w, t.toString(), e);
                }
            }
        }
        return f;
    }

    private ExperimentRunSettings getExperimentSettings(PrintWriter w, HttpServletRequest request) {
        ExperimentRunSettings s = ExperimentRunSettings.defaultSettings();
        for (final ExperimentRunParameters t : ExperimentRunParameters.values()) {
            final String param = request.getParameter(t.name());
            if (param != null && !param.isEmpty()) {
                try {
                    s = s.copyWithChangedParam(t, Double.parseDouble(param));
                } catch (final RuntimeException e) {
                    this.printParseError(w, t.toString(), e);
                }
            }
        }
        return s;
    }

    private void printParseError(PrintWriter w, String t, final RuntimeException e) {
        w.println("<b>Fehler beim Parsen von Parameter " + t + "</b><br/>");
        w.println(e.toString());
        w.println("<br/>");
    }

    private void printHeader(final PrintWriter w) {
        w.println("<html><head><title>Pre/Post commit review comparison - Process simulation</title></head><body>");
    }

    private void printFooter(final PrintWriter w) {
        w.println("</body></html>");
    }

    private void printInputParameters(final PrintWriter w, BulkParameterFactory f, ExperimentRunSettings s) {
        w.println("<form action=\".\">");
        w.println("<h2>Input parameters</h2>");
        w.println("<table>");
        for (final ParameterType t : ParameterType.values()) {
            w.println("<tr><td>");
            w.println(t.toString());
            w.println("</td><td>");
            w.println(this.getInputFor(t, f));
            w.println("</td><td>");
            w.println(t.getDescription());
            w.println("</td></tr>");
        }
        w.println("</table>");
        w.println("<h2>Experiment settings</h2>");
        w.println("<table>");
        for (final ExperimentRunParameters t : ExperimentRunParameters.values()) {
            w.println("<tr><td>");
            w.println(t.toString());
            w.println("</td><td>");
            w.println(this.getInputFor(t, s));
            w.println("</td><td>");
            w.println(t.getDescription());
            w.println("</td></tr>");
        }
        w.println("</table>");
        w.println("<button type=\"submit\">Start simulation</button>");
        w.println("</form>");
    }

    private String getInputFor(ParameterType t, BulkParameterFactory f) {
        if (t.getType().isEnum()) {
            final StringBuilder ret = new StringBuilder();
            ret.append("<select name=\"").append(t.name()).append("\">");
            for (final Object o : t.getType().getEnumConstants()) {
                if (o.equals(f.getParam(t))) {
                    ret.append("<option selected=\"selected\">");
                } else {
                    ret.append("<option>");
                }
                ret.append(o.toString()).append("</option>");
            }
            ret.append("</select>");
            return ret.toString();
        } else if (t.getType().equals(Double.class)) {
            return "<input name=\"" + t.name() + "\" value=\"" + f.getParam(t) + "\" type=\"number\" step=\"any\" />";
        } else if (t.getType().equals(Integer.class)) {
            return "<input name=\"" + t.name() + "\" value=\"" + f.getParam(t) + "\" type=\"number\" step=\"1\" />";
        } else {
            return "Invalid type: " + t.getType();
        }
    }

    private String getInputFor(ExperimentRunParameters t, ExperimentRunSettings s) {
        return "<input name=\"" + t.name() + "\" value=\"" + s.get(t) + "\" type=\"number\" step=\"any\" />";
    }

    private void simulateAndPrintOutput(PrintWriter w, BulkParameterFactory f, ExperimentRunSettings s, int requestId) {
        w.println("<h2>Simulation output</h2>");

        final StringBuilder detailsTable = new StringBuilder();
        final AtomicInteger count = new AtomicInteger(1);
        detailsTable.append("<table border=\"1\">");
        detailsTable.append("<tr><th>#</th><th colspan=\"3\">Story points</th><th colspan=\"3\">Cycle time</th><th colspan=\"3\">Bugs found by customer</th></tr>\n");
        detailsTable.append("<tr><th></th><th>no</th><th>pre</th><th>post</th><th>no</th><th>pre</th><th>post</th><th>no</th><th>pre</th><th>post</th></tr>\n");
        final SingleRunCallback detailsCallback = new SingleRunCallback() {
            @Override
            public void handleResult(ExperimentResult no, ExperimentResult pre, ExperimentResult post) {
                System.err.println("run " + count + " finished");
                detailsTable.append("<tr>");
                detailsTable.append("<td><a href=\"details/").append(requestId).append("/").append(count).append("/overview\">").append(count).append("</a></td>");
                detailsTable.append("<td>").append(no == null ? "" : no.getFinishedStoryPoints()).append("</td>");
                detailsTable.append("<td>").append(pre.getFinishedStoryPoints()).append("</td>");
                detailsTable.append("<td>").append(post.getFinishedStoryPoints()).append("</td>");
                detailsTable.append("<td>").append(no == null ? "" : no.getStoryCycleTimeMean()).append("</td>");
                detailsTable.append("<td>").append(pre.getStoryCycleTimeMean()).append("</td>");
                detailsTable.append("<td>").append(post.getStoryCycleTimeMean()).append("</td>");
                detailsTable.append("<td>").append(no == null ? "" : no.getBugCountFoundByCustomers()).append("</td>");
                detailsTable.append("<td>").append(pre.getBugCountFoundByCustomers()).append("</td>");
                detailsTable.append("<td>").append(post.getBugCountFoundByCustomers()).append("</td>");
                detailsTable.append("</tr>");
                count.incrementAndGet();
            }
        };

        final ExperimentRun result;
        try {
            result = ExperimentRun.perform(s, DataGenerator::runExperiment, f, detailsCallback);
        } catch (final RuntimeException e) {
            w.println("An exception occured during simulation: " + e.getMessage());
            return;
        }

        final ExperimentRunSummary summary = result.getSummary();
        w.println("Summary result - Story points: " + summary.getStoryPointsResult() + "<br/>");
        w.println("Summary result - Bugs found by customer: " + summary.getBugsResult() + "<br/>");
        w.println("Summary result - Cycle time: " + summary.getCycleTimeResult() + "<br/>");
        if (!result.isSummaryStatisticallySignificant()) {
            w.println("Summary result not statistically significant<br/>");
        }
        w.println("Median finished stories (best alternative): " + result.getFinishedStoryMedian().toHtml() + "<br/>");
        w.println("Median share of productive work: " + result.getShareProductiveWork().toHtmlPercent() + "<br/>");
        w.println("Median share no review/review story points: " + result.getFactorNoReview().toHtmlPercent() + "<br/>");
        w.println("Median difference pre/post story points: " + this.formatDiff(result.getFactorStoryPoints(), "pre", "post") + "<br/>");
        w.println("Median difference pre/post bugs found by customer: " + this.formatDiff(result.getFactorBugs(), "post", "pre") + "<br/>");
        w.println("Median difference pre/post cycle time: " + this.formatDiff(result.getFactorCycleTime(), "post", "pre") + "<br/>");
        w.println("<br/>");

        detailsTable.append("<tr>");
        detailsTable.append("<td></td>");
        detailsTable.append("<td>").append(result.getFinishedStoryPointsMedian(ReviewMode.NO_REVIEW).toHtml()).append("</td>");
        detailsTable.append("<td>").append(result.getFinishedStoryPointsMedian(ReviewMode.PRE_COMMIT).toHtml()).append("</td>");
        detailsTable.append("<td>").append(result.getFinishedStoryPointsMedian(ReviewMode.POST_COMMIT).toHtml()).append("</td>");
        detailsTable.append("<td>").append(result.getStoryCycleTimeMeanMedian(ReviewMode.NO_REVIEW).toHtml()).append("</td>");
        detailsTable.append("<td>").append(result.getStoryCycleTimeMeanMedian(ReviewMode.PRE_COMMIT).toHtml()).append("</td>");
        detailsTable.append("<td>").append(result.getStoryCycleTimeMeanMedian(ReviewMode.POST_COMMIT).toHtml()).append("</td>");
        detailsTable.append("<td>").append(result.getBugCountMedian(ReviewMode.NO_REVIEW).toHtml()).append("</td>");
        detailsTable.append("<td>").append(result.getBugCountMedian(ReviewMode.PRE_COMMIT).toHtml()).append("</td>");
        detailsTable.append("<td>").append(result.getBugCountMedian(ReviewMode.POST_COMMIT).toHtml()).append("</td>");
        detailsTable.append("</tr>");
        detailsTable.append("</table>");
        w.println(detailsTable);
    }

    private String formatDiff(MedianWithConfidenceInterval median, String betterWhenNegative, String betterWhenPositive) {
        return median.toHtmlPercent() + " [" + (median.getMedian() < 0 ? betterWhenNegative : betterWhenPositive) + " better]";
    }

    public static void main(String[] args) throws Exception {
        final Server server = new Server(8080);
        server.setHandler(new ServerMain());

        server.start();
        server.join();
    }
}

//    public static void main(final String[] args) throws IOException {
//        final Properties settings = loadSettings(args[0]);
//
//        final List<Contestant> contestants = Contestant.loadFromCsv(settings);
//        final Writer redo = new FileWriter("redo." + System.currentTimeMillis() + ".log");
//        final Tournament t = new Tournament(contestants, redo);
//
//        final HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
//        server.createContext("/doubleOut", new Handler(t, Integer.parseInt(settings.getProperty("breakLimit"))));
//        server.setExecutor(null);
//        server.start();
//        System.out.println("Server started...");
//    }
//
//    private static Properties loadSettings(final String filename) throws IOException {
//        try (FileInputStream is = new FileInputStream(filename)) {
//            final Properties settings = new Properties();
//            settings.load(is);
//            return settings;
//        }
//    }


