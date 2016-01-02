package de.unihannover.se.processSimulation.clusterControl;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import de.unihannover.se.processSimulation.dataGenerator.BulkParameterFactory.ParameterType;
import desmoj.core.dist.MersenneTwisterRandomGenerator;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;

public class MiningGuidedClusterControl {

    private static final int GUIDED_SAMPLES_PER_ROUND_AND_CLASS = 33;
    private static final int RANDOM_SAMPLES_PER_ROUND = 6;
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
    private final List<ParamRestriction> originalRestrictions;
    private final String paramsFileContent;
    private final TupleWriter tupleWriter;
    private final Path resultDir;
    private final Session session;
    private final MessageProducer producer;
    private final MessageConsumer consumer;
    private final File basicArff;
    private final boolean justRandom;

    public MiningGuidedClusterControl(
                    String resultDir,
                    Session session,
                    MessageProducer producer,
                    MessageConsumer consumer,
                    File paramsFile,
                    File basicArff,
                    TupleWriter tupleWriter,
                    boolean justRandom) throws IOException {
        this.resultDir = new File(resultDir).toPath();
        this.session = session;
        this.producer = producer;
        this.consumer = consumer;
        this.basicArff = basicArff;
        this.tupleWriter = tupleWriter;
        this.justRandom = justRandom;
        this.paramsFileContent = Common.readFileAsString(paramsFile);
        this.originalRestrictions = new ArrayList<>();
        for (final String line : this.paramsFileContent.split("\n")) {
            if (line.trim().isEmpty()) {
                continue;
            }
            final String[] parts = line.split(" ");
            final ParameterType type = ParameterType.valueOf(parts[0]);
            final List<Object> values = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                values.add(type.parse(parts[i].trim()));
            }
            if (values.get(0) instanceof Double) {
                this.originalRestrictions.add(
                                new ParamRestriction(parts[0], (Double) values.get(0), (Double) values.get(1)));
            } else if (values.get(0) instanceof Integer) {
                this.originalRestrictions.add(
                                new ParamRestriction(parts[0], (Integer) values.get(0), (Integer) values.get(1)));
            } else {
                final Set<String> stringValues = new LinkedHashSet<>();
                for (final Object o : values) {
                    stringValues.add(o.toString());
                }
                this.originalRestrictions.add(new ParamRestriction(parts[0], stringValues));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        final String url = args[0]; //"tcp://TOBI:61616"
        final String paramsFile = args[1];
        final String basicArffFile = args[2];
        final String resultDir = args[3];
        final boolean onlyRandom = args.length >= 5 && args[4].equals("--onlyRandom");

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
                        final MiningGuidedClusterControl cc = new MiningGuidedClusterControl(
                                resultDir, session, producer, consumer, new File(paramsFile), new File(basicArffFile), tupleWriter, onlyRandom);
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
                    this.analyzeDataAndGenerateNewWorkPackages();
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
        final TextMessage message = this.session.createTextMessage(this.paramsFileContent + Common.SPLITTER + workPackage);
        message.setStringProperty(Common.MSG_ID, id);
        this.producer.send(message);
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
        return (int) Files.list(this.resultDir).filter(MiningGuidedClusterControl::isUnfinished).count();
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

    public void analyzeDataAndGenerateNewWorkPackages() throws Exception {
        if (this.justRandom) {
            this.generateParamValues(this.originalRestrictions,
                            3 * GUIDED_SAMPLES_PER_ROUND_AND_CLASS + RANDOM_SAMPLES_PER_ROUND);
            return;
        }

        final Reader combinedReader = new CombinedReader(new FileReader(this.basicArff),
                        Files.list(this.resultDir).filter(MiningGuidedClusterControl::isFinished));
        final Instances instances = new Instances(combinedReader);
        combinedReader.close();

        System.out.println("Loaded instances: " + instances.size());
        removeIrrelevantOutputAttributes(instances);
        this.analyzeAndGenerateFor(instances, "summaryCycleTime", GUIDED_SAMPLES_PER_ROUND_AND_CLASS);
        removeAttribute(instances, "summaryCycleTime");
        this.analyzeAndGenerateFor(instances, "summaryStoryPoints", GUIDED_SAMPLES_PER_ROUND_AND_CLASS);
        removeAttribute(instances, "summaryStoryPoints");
        this.analyzeAndGenerateFor(instances, "summaryBugs", GUIDED_SAMPLES_PER_ROUND_AND_CLASS);
        this.generateParamValues(this.originalRestrictions, RANDOM_SAMPLES_PER_ROUND);
        this.tupleWriter.endFileIfOpen();
    }

    private void analyzeAndGenerateFor(Instances instances, String clazz, int count) throws Exception {
        instances.setClass(instances.attribute(clazz));
        final int minClassSize = determineMinClassSize(instances);

        final J48 j48 = new J48();
        j48.setMinNumObj(Math.max(2, (int) Math.sqrt(minClassSize)));
        j48.buildClassifier(instances);

        this.generateNewWorkPackages(DecisionTreeNode.parse(j48.prefix()), count);
        System.out.println("generated " + count + " for " + clazz);
    }

    private static int determineMinClassSize(Instances instances) {
        final int numValues = instances.classAttribute().numValues();
        final int[] counts = new int[numValues];
        final int classIdx = instances.classIndex();
        for (final Instance i : instances) {
            counts[(int) i.value(classIdx)]++;
        }
        int min = Integer.MAX_VALUE;
        for (final int count : counts) {
            if (count > 0) {
                min = Math.min(min, count);
            }
        }
        return min;
    }

    private static void removeIrrelevantOutputAttributes(Instances instances) {
        for (int i = instances.numAttributes() - 1; i >= 0 ; i--) {
            final String name = instances.attribute(i).name();
            if (!name.matches("[A-Z_]+|summaryCycleTime|summaryBugs|summaryStoryPoints")) {
                instances.deleteAttributeAt(i);
            }
        }
    }

    private static void removeAttribute(Instances instances, String name) {
        final int idx = instances.attribute(name).index();
        if (idx == instances.classIndex()) {
            instances.setClassIndex(0);
        }
        instances.deleteAttributeAt(idx);
    }

    private void generateNewWorkPackages(DecisionTreeNode decisionTreeRoot, int count) throws IOException {
        final Map<String, ParamRestriction> furtherRestrictions = decisionTreeRoot.getRestrictionsForWorstLeaf().getFirst();
        final List<ParamRestriction> refinedRestrictions = new ArrayList<>();
        for (final ParamRestriction originalRestriction : this.originalRestrictions) {
            final ParamRestriction furtherRestriction = furtherRestrictions.get(originalRestriction.getName());
            if (furtherRestriction != null) {
                refinedRestrictions.add(originalRestriction.intersect(furtherRestriction));
            } else {
                refinedRestrictions.add(originalRestriction);
            }
        }

        this.generateParamValues(refinedRestrictions, count);
    }

    private void generateParamValues(List<ParamRestriction> params, int tupleCount) throws IOException {
        for (int i = 0; i < tupleCount; i++) {
            for (int j = 0; j < params.size(); j++) {
                this.tupleWriter.write(params.get(j).sample(this.rng));
            }
            this.tupleWriter.endTuple();
        }
    }

}
