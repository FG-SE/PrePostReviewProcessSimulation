package de.unihannover.se.processSimulation.clusterControl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory;
import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory.ParameterType;
import de.unihannover.se.processSimulation.postprocessing.ArffReader;
import desmoj.core.dist.MersenneTwisterRandomGenerator;

public class MixingClusterControl {

    private static final int MIX_BULK_SIZE = 10;
    private static final int TUPLES_PER_FILE = 15;
    private static final long MESSAGE_TIMEOUT = 1000L * 60 * 60 * 3;
    private static final long RESEND_TIMEOUT = 1000L * 60 * 60 * 12;
    private static final int NEW_WORK_THRESHOLD = 50;

    private static final class TupleWriter {
        private final File dir;
        private int cnt;
        private Writer currentWriter;
        private boolean tupleStart;
        private int tuplesInCurrentFile;

        public TupleWriter(File dir) throws IOException {
            this.dir = dir;
            this.cnt = Files.list(dir.toPath())
                .filter(f -> isUnfinished(f) || isFinished(f))
                .map(f -> getID(f.toFile()))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
        }

        public void write(String format) throws IOException {
            if (this.currentWriter == null) {
                this.currentWriter = new FileWriter(new File(this.dir, String.format("new.%08d.txt", this.cnt++)));
                this.tuplesInCurrentFile = 0;
                this.tupleStart = true;
            }
            if (this.tupleStart) {
                this.tupleStart = false;
            } else {
                this.currentWriter.write(' ');
            }
            this.currentWriter.write(format);
        }

        public void endTuple() throws IOException {
            this.currentWriter.write('\n');
            this.tupleStart = true;
            this.tuplesInCurrentFile++;
            if (this.tuplesInCurrentFile >= TUPLES_PER_FILE) {
                this.currentWriter.close();
                this.currentWriter = null;
            }
        }

        public void endFileIfOpen() throws IOException {
            if (this.currentWriter != null) {
                this.currentWriter.close();
                this.currentWriter = null;
            }
        }


    }

    private final MersenneTwisterRandomGenerator rng = new MersenneTwisterRandomGenerator(System.currentTimeMillis());
    private final TupleWriter tupleWriter;
    private final Path resultDir;
    private final Session session;
    private final MessageProducer producer;
    private final MessageConsumer consumer;
    private final List<BulkParameterFactory> preBetter;
    private final List<BulkParameterFactory> postBetter;

    public MixingClusterControl(
                    String resultDir,
                    Session session,
                    MessageProducer producer,
                    MessageConsumer consumer,
                    File basicArff,
                    String resultColumn,
                    TupleWriter tupleWriter) throws IOException {
        this.resultDir = new File(resultDir).toPath();
        this.session = session;
        this.producer = producer;
        this.consumer = consumer;
        this.tupleWriter = tupleWriter;
        this.preBetter = new ArrayList<>();
        this.postBetter = new ArrayList<>();

        System.out.println("Loading data for result column " + resultColumn);
        ArffReader.read(new BufferedReader(new FileReader(basicArff)), instance -> {
            if (instance.get(resultColumn).equals("PRE_BETTER")) {
                this.preBetter.add(this.getParameters(instance));
            } else if (instance.get(resultColumn).equals("POST_BETTER")) {
                this.postBetter.add(this.getParameters(instance));
            }
        });

        System.out.println(String.format("Input data read. %d pre better, %d post better", this.preBetter.size(), this.postBetter.size()));
    }

    private BulkParameterFactory getParameters(Map<String, String> instance) {
        BulkParameterFactory ret = BulkParameterFactory.forCommercial();
        for (final ParameterType t : ParameterType.values()) {
            if (instance.containsKey(t.name())) {
                ret = ret.copyWithChangedParam(t, t.parse(instance.get(t.name())));
            }
        }
        return ret;
    }

    public static void main(String[] args) throws Exception {
        final String url = args[0]; //"tcp://TOBI:61616"
        final String basicArffFile = args[1];
        final String resultColumnName = args[2];
        final String resultDir = args[3];

        new File(resultDir).mkdir();
        final TupleWriter tupleWriter = new TupleWriter(new File(resultDir));

        final ActiveMQConnectionFactory connFactory = new ActiveMQConnectionFactory(url);
        final Connection connection = connFactory.createConnection();
        try {
            connection.start();
            startListeningForLog(connection);
            final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            try {
                final Queue queueC = session.createQueue(Common.RESULT_QUEUE);
                final MessageConsumer consumer = session.createConsumer(queueC);
                try {
                    final Queue queueP = session.createQueue(Common.WORK_QUEUE);
                    final MessageProducer producer = session.createProducer(queueP);
                    try {
                        final MixingClusterControl cc = new MixingClusterControl(
                                resultDir, session, producer, consumer, new File(basicArffFile), resultColumnName, tupleWriter);
                        cc.doWork();
                        System.out.println("Cluster control shutting down.");
                    } finally {
                        producer.close();
                    }
                } finally {
                    consumer.close();
                }
            } finally {
                session.close();
            }
        } finally {
            connection.close();
        }
    }

    private void doWork() throws Exception {
        int totalReceived = 0;
        while (true) {
            final int unfinishedWorkPackages = this.countUnfinishedWorkPackages();
            if (this.shallShutdown()) {
                System.out.println("Waiting for shutdown. Open packages = " + unfinishedWorkPackages);
                if (unfinishedWorkPackages == 0) {
                    break;
                }
            } else {
                if (unfinishedWorkPackages < NEW_WORK_THRESHOLD) {
                    System.out.println("Only " + unfinishedWorkPackages + " unfinished packages left, creating new work");
                    this.generateNewWorkPackages();
                }
            }

            this.sendWorkPackages();
            final TextMessage message = (TextMessage) this.consumer.receive(MESSAGE_TIMEOUT);
            if (message != null) {
                final String msgId = message.getStringProperty(Common.MSG_ID);
                totalReceived++;
                System.out.println(String.format("Received result %s from %s, total received %d",
                                msgId, message.getStringProperty(Common.MSG_PROCESSOR), totalReceived));
                this.createResultFile(msgId, message.getText());
            }
        }
    }

    private void createResultFile(String msgId, String resultText) throws IOException {
        final Path paramFile = Files.list(this.resultDir).filter(f -> getID(f.toFile()).equals(msgId)).findFirst().get();
        if (isFinished(paramFile)) {
            //duplicate message received
            System.out.println("duplicate message: " + msgId);
            return;
        }

        final String[] inputLines = Common.readFileAsString(paramFile.toFile()).split("\n");
        final String[] resultLines = resultText.split("\n");

        if (inputLines.length != resultLines.length) {
            System.out.println("invalid message: " + msgId + ", " + inputLines.length + ", " + resultLines.length);
            return;
        }

        final String tempFilename = String.format("tmp.%s.txt", msgId);
        final String finalFilename = String.format("result.%s.txt", msgId);
        try (FileWriter resultFile = new FileWriter(this.fileInResultDir(tempFilename))) {
            for (int i = 0; i < inputLines.length; i++) {
                resultFile.write(
                        inputLines[i].trim().replace(' ', ',')
                        + ","
                        + resultLines[i].trim().replace(';', ',')
                        + "\n");
            }
        }
        //rename at end, so that there definetely is no incomplete "result" file
        this.fileInResultDir(tempFilename).renameTo(this.fileInResultDir(finalFilename));
        Files.delete(paramFile);
    }

    private File fileInResultDir(String tempFilename) {
        return new File(this.resultDir.toFile(), tempFilename);
    }

    private void sendWorkPackages() throws IOException {
        Files.list(this.resultDir).forEach(f -> {
            if (isUnsent(f) || isOpenForTooLong(f)) {
                try {
                    this.sendWorkPackage(f.toFile());
                } catch (IOException | JMSException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private static boolean isUnsent(Path f) {
        return f.getFileName().toString().startsWith("new.");
    }

    private static boolean isOpenForTooLong(Path f) {
        final String filename = f.getFileName().toString();
        final Pattern p = Pattern.compile("sent\\.[0-9]+\\.on\\.([0-9]+)\\.txt");
        final Matcher m = p.matcher(filename);
        if (!m.matches()) {
            return false;
        }
        final long sendTimestamp = Long.parseLong(m.group(1));
        return System.currentTimeMillis() - sendTimestamp > RESEND_TIMEOUT;
    }

    private static boolean isUnfinished(Path f) {
        final String filename = f.getFileName().toString();
        return filename.startsWith("new.") || filename.startsWith("sent.");
    }

    private static boolean isFinished(Path f) {
        final String filename = f.getFileName().toString();
        return filename.startsWith("result.");
    }

    private void sendWorkPackage(File f) throws IOException, JMSException {
        final String id = getID(f);
        final String workPackage = Common.readFileAsString(f);
        f.renameTo(new File(f.getParentFile(), String.format("sent.%s.on.%015d.txt", id, System.currentTimeMillis())));
        final TextMessage message = this.session.createTextMessage(this.allParamNames() + Common.SPLITTER + workPackage);
        message.setStringProperty(Common.MSG_ID, id);
        this.producer.send(message);
    }

    private String allParamNames() {
        final StringBuilder ret = new StringBuilder();
        for (final ParameterType t : ParameterType.values()) {
            ret.append(t.toString()).append('\n');
        }
        return ret.toString();
    }

    private static String getID(File f) {
        final Pattern p = Pattern.compile("[a-z]+\\.([0-9]+).*");
        final Matcher m = p.matcher(f.getName());
        m.matches();
        return m.group(1);
    }

    private boolean shallShutdown() {
        return new File(this.resultDir.toFile(), "shutdown.txt").exists();
    }

    private int countUnfinishedWorkPackages() throws IOException {
        return (int) Files.list(this.resultDir).filter(MixingClusterControl::isUnfinished).count();
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

    public void generateNewWorkPackages() throws Exception {
        for (int i = 0; i < MIX_BULK_SIZE; i++) {
            this.generateMix();
        }
        this.tupleWriter.endFileIfOpen();
    }

    private void generateMix() throws IOException {
        final BulkParameterFactory f1 = this.postBetter.get(this.randomIndex(this.postBetter.size()));
        final BulkParameterFactory f2 = this.preBetter.get(this.randomIndex(this.postBetter.size()));

        for (int i = 0; i < ParameterType.values().length; i++) {
            final ParameterType t = ParameterType.values()[i];
            if (f1.getParam(t).equals(f2.getParam(t))) {
                continue;
            }
            final long mixPattern = 1L << i;
            this.writeParamValueTuple(BulkParameterFactory.mix(f1, f2, mixPattern));
            this.writeParamValueTuple(BulkParameterFactory.mix(f2, f1, mixPattern));
        }
        this.writeParamValueTuple(BulkParameterFactory.mix(f1, f2, this.rng.nextLong()));
    }

    private int randomIndex(int size) {
        int rand = (int) this.rng.nextLong();
        if (rand < 0) {
            rand = ~rand;
        }
        return rand % size;
    }

    private void writeParamValueTuple(BulkParameterFactory f) throws IOException {
        for (final ParameterType t : ParameterType.values()) {
            this.tupleWriter.write(f.getParam(t).toString());
        }
        this.tupleWriter.endTuple();
    }

}
