package de.unihannover.se.processSimulation.clusterControl;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
import java.util.List;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;

import de.unihannover.se.processSimulation.dataGenerator.BulkFileExecutor;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory.ParameterType;

public class ClusterWorker {

    private static final long TIMEOUT = 1000L * 60;

    public static void main(String[] args) throws Exception {
        final String url = args[0]; //"tcp://TOBI:61616"
        final String ownId = args[1];

        String fullOwnId = InetAddress.getLocalHost().getHostName()
                        + "_" + System.getProperty("user.name")
                        + "_" + ownId;
        fullOwnId = fullOwnId.replace("\\", "").replace("/", "").replace(":", "");

        final File workDir = new File(fullOwnId);
        workDir.mkdir();

        final ActiveMQConnectionFactory connFactory = new ActiveMQConnectionFactory(url);
        final ActiveMQPrefetchPolicy prefetchPolicy = new ActiveMQPrefetchPolicy();
        prefetchPolicy.setQueuePrefetch(0);
        connFactory.setPrefetchPolicy(prefetchPolicy);
        final Connection connection = connFactory.createConnection();
        try {
            connection.start();
            performWork(connection, workDir);
        } finally {
            connection.close();
        }
    }

    private static void performWork(Connection connection, File workDir) throws Exception {
        final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final MessageProducer logProducer = session.createProducer(session.createQueue(Common.LOG_QUEUE));
        final MessageConsumer workConsumer = session.createConsumer(session.createQueue(Common.WORK_QUEUE));
        final MessageProducer resultProducer = session.createProducer(session.createQueue(Common.RESULT_QUEUE));

        final File shutdownFile = new File(workDir, "shutdown.txt");
        while (!shutdownFile.exists()) {
            final TextMessage message = (TextMessage) workConsumer.receive(TIMEOUT);
            if (message == null) {
                System.out.println("received no work for " + TIMEOUT + " ms ...");
                log(session, logProducer, workDir, "has nothing to do");
                continue;
            }
            final String msgId = message.getStringProperty(Common.MSG_ID);
            log(session, logProducer, workDir, "starts working on message " + msgId);
            final File msgDir = new File(workDir, msgId);
            msgDir.mkdir();
            final File paramFile = new File(msgDir, "params.txt");
            final File parameterSetsFile = new File(msgDir, "parameterSets.txt");
            final File resultsFile = new File(msgDir, "results.txt");
            writeInputToMsgDir(message, paramFile, parameterSetsFile);
            executeRuns(paramFile, parameterSetsFile, resultsFile);
            sendResultMessage(session, resultsFile, resultProducer);
            log(session, logProducer, workDir, "finished working on message " + msgId);
        }
        log(session, logProducer, workDir, "shutting down");

        resultProducer.close();
        workConsumer.close();
        logProducer.close();
    }

    private static void writeInputToMsgDir(TextMessage message, File paramFile, File parameterSetsFile) throws IOException, JMSException {
        final String[] parts = message.getText().split(Common.SPLITTER);
        assert parts.length == 2;
        Common.writeToFile(paramFile, parts[0]);
        Common.writeToFile(parameterSetsFile, parts[1]);
    }

    private static void executeRuns(File paramFile, File parameterSetsFile, File resultsFile) throws Exception {
        final List<ParameterType> paramNames = BulkFileExecutor.readParamNames(paramFile);
        BulkFileExecutor.executeBulk(paramNames, parameterSetsFile, resultsFile);
    }

    private static void sendResultMessage(Session session, File resultsFile, MessageProducer resultProducer) throws JMSException, IOException {
        final TextMessage msg = session.createTextMessage(Common.readFileAsString(resultsFile));
        msg.setStringProperty(Common.MSG_ID, resultsFile.getParentFile().getName());
        msg.setStringProperty(Common.MSG_PROCESSOR, resultsFile.getParentFile().getParentFile().getName());
        resultProducer.send(msg);
    }

    private static void log(Session session, MessageProducer logProducer, File workDir, String string) throws JMSException {
        final String fullMessage = workDir.getName() + " " + new Date() + ": " + string;
        System.out.println(fullMessage);
        final TextMessage msg = session.createTextMessage(fullMessage);
        msg.setStringProperty(Common.MSG_PROCESSOR, workDir.getName());
        logProducer.send(msg);
    }
}
