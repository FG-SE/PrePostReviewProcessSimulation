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

package de.unihannover.se.processSimulation.clusterControl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

/**
 * Main class to control the distribution of work packages to several cluster workers (using a message queue) and to
 * collect the results.
 */
public class ClusterControl {

    private static final int MAX_LINES_PER_PACKAGE = 20;
    private static final long MESSAGE_TIMEOUT = 1000L * 60 * 60 * 3;

    public static void main(String[] args) throws Exception {
        final String url = args[0]; //"tcp://TOBI:61616"
        final String paramsFile = args[1];
        final String paramSetsFile = args[2];
        final String resultDir = args[3];

        new File(resultDir).mkdir();

        final ActiveMQConnectionFactory connFactory = new ActiveMQConnectionFactory(url);
        final Connection connection = connFactory.createConnection();
        try {
            connection.start();
            startListeningForLog(connection);
            final Set<String> resultIds = sendWorkMessages(connection, paramsFile, paramSetsFile, Collections.emptySet());
            waitForResults(connection, paramsFile, paramSetsFile, resultIds, resultDir);
        } finally {
            connection.close();
        }
    }

    private static void startListeningForLog(Connection connection) throws JMSException {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    final FileWriter w = new FileWriter("log.txt");
                    final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    try {
                        final Queue queue = session.createQueue(Common.LOG_QUEUE);
                        final MessageConsumer consumer = session.createConsumer(queue);
                        try {
                            while (!Thread.interrupted()) {
                                final TextMessage message = (TextMessage) consumer.receive();
                                if (message == null) {
                                    break;
                                }
                                final String logMsg = new Date() + " - Log: " + message.getText();
                                System.out.println(logMsg);
                                w.write(logMsg);
                                w.write("\n");
                                w.flush();
                            }
                        } finally {
                            consumer.close();
                        }
                    } finally {
                        session.close();
                    }
                    w.close();
                } catch (final JMSException | IOException e) {
                    System.err.println("Error in log listener");
                    e.printStackTrace();
                }
            }
        };
        new Thread(r).start();
    }

    private static Set<String> sendWorkMessages(
            Connection connection, String paramsFile, String paramSetsFile, Set<String> idsNotToSend)
        throws JMSException, IOException {

        final String paramsFileContent = Common.readFileAsString(new File(paramsFile)).trim();

        final Set<String> resultIds = new TreeSet<String>();
        int count = 0;
        final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        try {
            final Queue queue = session.createQueue(Common.WORK_QUEUE);
            final MessageProducer producer = session.createProducer(queue);
            try {

                try (BufferedReader r = new BufferedReader(new FileReader(paramSetsFile))) {
                    final StringBuilder workPackage = new StringBuilder();
                    String line;
                    int linesInWorkPackage = 0;
                    while ((line = r.readLine()) != null) {
                        workPackage.append(line).append('\n');
                        linesInWorkPackage++;
                        if (linesInWorkPackage > MAX_LINES_PER_PACKAGE) {
                            sendWorkPackage(paramsFileContent, session, producer, workPackage, count, resultIds, idsNotToSend);
                            count++;
                            linesInWorkPackage = 0;
                            workPackage.setLength(0);
                        }
                    }
                    if (linesInWorkPackage > 0) {
                        sendWorkPackage(paramsFileContent, session, producer, workPackage, count, resultIds, idsNotToSend);
                        count++;
                    }
                }

                System.out.println("Finished sending with " + resultIds.size() + " of " + count + " messages sent.");
            } finally {
                producer.close();
            }
        } finally {
            session.close();
        }
        return resultIds;
    }

    private static void sendWorkPackage(final String paramsFileContent, final Session session, MessageProducer producer,
                    final StringBuilder workPackage, int nbr, Set<String> missingResultsBuffer, Set<String> idsNotToSend) throws JMSException {
        final String id = String.format("%08d", nbr);
        if (!idsNotToSend.contains(id)) {
            final TextMessage message = session.createTextMessage(paramsFileContent + Common.SPLITTER + workPackage);
            message.setStringProperty(Common.MSG_ID, id);
            producer.send(message);
            missingResultsBuffer.add(id);
        }
    }

    private static void waitForResults(
            Connection connection, String paramsFile, String paramSetsFile, Set<String> resultIds, String resultDir)
        throws IOException, JMSException {

        final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        try {
            final Queue queue = session.createQueue(Common.RESULT_QUEUE);
            final MessageConsumer consumer = session.createConsumer(queue);
            try {
                final Set<String> receivedIds = new TreeSet<>();
                while (receivedIds.size() < resultIds.size()) {
                    final TextMessage message = (TextMessage) consumer.receive(MESSAGE_TIMEOUT);
                    if (message != null) {
                        final String msgId = message.getStringProperty(Common.MSG_ID);
                        assert resultIds.contains(msgId);
                        receivedIds.add(msgId);
                        System.out.println(String.format("Received result %d/%d: %s from %s",
                                        receivedIds.size(), resultIds.size(), msgId, message.getStringProperty(Common.MSG_PROCESSOR)));
                        Common.writeToFile(new File(resultDir, "result." + msgId), message.getText());
                    } else {
                        //Timeout => resend open work packages because they were probably lost
                        final Set<String> resentIds = sendWorkMessages(connection, paramsFile, paramSetsFile, receivedIds);
                        System.out.println("Timeout. Resent work packages " + resentIds);
                    }
                }
                System.out.println("All results received");
            } finally {
                consumer.close();
            }
        } finally {
            session.close();
        }
    }

}
