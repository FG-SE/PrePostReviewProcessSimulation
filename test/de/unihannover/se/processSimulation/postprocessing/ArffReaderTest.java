package de.unihannover.se.processSimulation.postprocessing;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.Map;

import org.junit.Test;

import de.unihannover.se.processSimulation.postprocessing.ArffReader.DataHandler;

public class ArffReaderTest {

    private static final class TestCallback implements DataHandler {

        private final StringBuilder results = new StringBuilder();

        @Override
        public void handleInstance(Map<String, String> instance) {
            this.results.append(instance).append('\n');
        }

        public String getResults() {
            return this.results.toString();
        }

    }

    private static TestCallback runTest(String s) throws Exception {
        final TestCallback callback = new TestCallback();
        ArffReader.read(new StringReader(s), callback);
        return callback;
    }

    @Test
    public void testReadSimple() throws Exception {
        final TestCallback callback = runTest("@relation sensitivityAnalysis\n"
            + "\n"
            + "@attribute implementationSkillMode numeric\n"
            + "@attribute reviewSkillMode numeric\n"
            + "@attribute dependencyGraphConstellation {SIMPLISTIC,REALISTIC,NO_DEPENDENCIES,CHAINS,DIAMONDS}\n"
            + "\n"
            + "@data\n"
            + "0.9,0.5,REALISTIC\n"
            + "0.9,0.5,REALISTIC");
        assertEquals("{implementationSkillMode=0.9, reviewSkillMode=0.5, dependencyGraphConstellation=REALISTIC}\n"
                    + "{implementationSkillMode=0.9, reviewSkillMode=0.5, dependencyGraphConstellation=REALISTIC}\n",
                    callback.getResults());
    }

}
