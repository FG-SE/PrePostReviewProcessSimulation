package de.unihannover.se.processSimulation.interactive;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import de.unihannover.se.processSimulation.common.ReviewMode;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory.ParameterType;
import de.unihannover.se.processSimulation.dataGenerator.DataGenerator;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentResult;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRun;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRun.SingleRunCallback;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRunSettings;
import de.unihannover.se.processSimulation.dataGenerator.ExperimentRunSettings.ExperimentRunParameters;
import de.unihannover.se.processSimulation.dataGenerator.StatisticsUtil;

public class ServerMain extends AbstractHandler {

    @Override
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
        throws IOException, ServletException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        final PrintWriter w = response.getWriter();
        this.printHeader(w);
        final BulkParameterFactory f = this.getParameters(w, request);
        final ExperimentRunSettings s = this.getExperimentSettings(w, request);
        this.printInputParameters(w, f, s);
        if (this.shallSimulate(request)) {
            this.simulateAndPrintOutput(w, f, s);
        }
        this.printFooter(w);
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
        w.println("<h1>Pre/Post commit review comparison - Process simulation</h1>");
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

    private void simulateAndPrintOutput(PrintWriter w, BulkParameterFactory f, ExperimentRunSettings s) {
        w.println("<h2>Simulation output</h2>");

        final StringBuilder detailsTable = new StringBuilder();
        final AtomicInteger count = new AtomicInteger(1);
        detailsTable.append("<table border=\"1\">");
        detailsTable.append("<tr><th>#</th><th colspan=\"3\">Story points</th><th colspan=\"3\">Cycle time</th><th colspan=\"3\">Remaining bugs</th></tr>\n");
        detailsTable.append("<tr><th></th><th>no</th><th>pre</th><th>post</th><th>no</th><th>pre</th><th>post</th><th>no</th><th>pre</th><th>post</th></tr>\n");
        final SingleRunCallback detailsCallback = new SingleRunCallback() {
            @Override
            public void handleResult(ExperimentResult no, ExperimentResult pre, ExperimentResult post) {
                System.err.println("run " + count + " finished");
                detailsTable.append("<tr>");
                detailsTable.append("<td>").append(count).append("</td>");
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

        final ExperimentRun result = ExperimentRun.perform(s, DataGenerator::runExperiment, f, detailsCallback);

        w.println("Summary result: " + result.getSummary() + "<br/>");
        if (!result.isSummaryStatisticallySignificant()) {
            w.println("Summary result not statistically significant<br/>");
        }
        w.println("Median finished stories (best alternative): " + result.getFinishedStoryMedian().toHtml() + "<br/>");
        w.println("Median share of productive work: " + result.getShareProductiveWork().toHtmlPercent() + "<br/>");
        w.println("Median share no review/review story points: " + result.getFactorNoReview().toHtmlPercent() + "<br/>");
        w.println("Median difference pre/post story points: " + result.getFactorStoryPoints().toHtmlPercent() + "<br/>");
        w.println("Median difference pre/post remaining bugs: " + result.getFactorBugs().toHtmlPercent() + "<br/>");
        w.println("Median difference pre/post cycle time: " + result.getFactorCycleTime().toHtmlPercent() + "<br/>");
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

    public static void main(String[] args) throws Exception {
        final Server server = new Server(8080);
        server.setHandler(new ServerMain());

        server.start();
        server.join();

        StatisticsUtil.close();
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


