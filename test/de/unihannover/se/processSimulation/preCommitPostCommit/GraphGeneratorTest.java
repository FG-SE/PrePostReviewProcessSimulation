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

import java.util.TreeSet;

import org.junit.Test;

import de.unihannover.se.processSimulation.preCommitPostCommit.GraphGenerator.GraphItemFactory;
import desmoj.core.dist.MersenneTwisterRandomGenerator;

public class GraphGeneratorTest {

    private static final class SimpleGraphItemFactory implements GraphItemFactory<Integer> {

        private int nextNodeId;
        private final TreeSet<String> edges = new TreeSet<>();

        @Override
        public Integer createNode() {
            return this.nextNodeId++;
        }

        @Override
        public void connect(Integer from, Integer to) {
            this.edges.add(from + "->" + to);
        }

        public String getResults() {
            final StringBuilder ret = new StringBuilder();
            ret.append("nodes: 0 .. ").append(this.nextNodeId - 1).append("\n");
            for (final String edge : this.edges) {
                ret.append(edge).append('\n');
            }
            return ret.toString();
        }

    }

    private static SimpleGraphItemFactory createFactory() {
        return new SimpleGraphItemFactory();
    }

    private static GraphGenerator createGenerator() {
        return new GraphGenerator(new MersenneTwisterRandomGenerator(7643));
    }

    @Test
    public void testGraphWithOneNode() {
        final GraphGenerator g = createGenerator();
        g.addTemplate("A", 1);
        final SimpleGraphItemFactory testFactory = createFactory();
        g.generateGraph(testFactory);

        assertEquals("nodes: 0 .. 0\n", testFactory.getResults());
    }

    @Test
    public void testGraphWithTwoUnconnectedNodes() {
        final GraphGenerator g = createGenerator();
        g.addTemplate("A;B", 1);
        final SimpleGraphItemFactory testFactory = createFactory();
        g.generateGraph(testFactory);

        assertEquals("nodes: 0 .. 1\n", testFactory.getResults());
    }

    @Test
    public void testGraphWithTwoConnectedNodes() {
        final GraphGenerator g = createGenerator();
        g.addTemplate("A->B", 1);
        final SimpleGraphItemFactory testFactory = createFactory();
        g.generateGraph(testFactory);

        assertEquals("nodes: 0 .. 1\n"
                        + "0->1\n",
                        testFactory.getResults());
    }

    @Test
    public void testRedeclarationAndWhitespaceDoesNotMatter() {
        final GraphGenerator g = createGenerator();
        g.addTemplate("A; B; A ->B", 1);
        final SimpleGraphItemFactory testFactory = createFactory();
        g.generateGraph(testFactory);

        assertEquals("nodes: 0 .. 1\n"
                        + "0->1\n",
                        testFactory.getResults());
    }

    @Test
    public void testEdgeChaining() {
        final GraphGenerator g = createGenerator();
        g.addTemplate("A->B->C", 1);
        final SimpleGraphItemFactory testFactory = createFactory();
        g.generateGraph(testFactory);

        assertEquals("nodes: 0 .. 2\n"
                        + "0->1\n"
                        + "1->2\n",
                        testFactory.getResults());
    }

    @Test
    public void testCountDoesSomething() {
        final GraphGenerator g = createGenerator();
        g.addTemplate("A->B", 0);
        g.addTemplate("A->B", 0);
        g.addTemplate("A;B", 1);
        g.addTemplate("A->B", 0);
        g.addTemplate("A->B", 0);
        final SimpleGraphItemFactory testFactory = createFactory();
        g.generateGraph(testFactory);

        assertEquals("nodes: 0 .. 1\n", testFactory.getResults());
    }

    @Test
    public void testRandomness() {
        final GraphGenerator g = createGenerator();
        g.addTemplate("A->B", 1);
        g.addTemplate("A;B", 1);

        final SimpleGraphItemFactory testFactory2 = createFactory();
        g.generateGraph(testFactory2);
        assertEquals("nodes: 0 .. 1\n", testFactory2.getResults());

        final SimpleGraphItemFactory testFactory1 = createFactory();
        g.generateGraph(testFactory1);
        assertEquals("nodes: 0 .. 1\n"
                        + "0->1\n", testFactory1.getResults());
    }

}
