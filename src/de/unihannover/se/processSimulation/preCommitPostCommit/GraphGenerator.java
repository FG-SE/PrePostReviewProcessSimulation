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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import desmoj.core.dist.MersenneTwisterRandomGenerator;

public class GraphGenerator {

    public static interface GraphItemFactory<N> {
        public abstract N createNode();
        public abstract void connect(N from, N to);
    }

    private final MersenneTwisterRandomGenerator random;
    private final List<String> descriptions;

    public GraphGenerator(MersenneTwisterRandomGenerator random) {
        this.descriptions = new ArrayList<>();
        this.random = random;
    }

    public void addTemplate(String description, int count) {
        for (int i = 0; i < count; i++) {
            this.descriptions.add(description);
        }
    }

    public<NODE> void generateGraph(GraphItemFactory<NODE> factory) {
        this.parseDescription(this.descriptions.get(this.random.nextInt(16) % this.descriptions.size()), factory);
    }

    private<NODE> void parseDescription(String string, GraphItemFactory<NODE> fac) {
        final Map<String, NODE> nodeMapping = new HashMap<>();
        final String[] parts = string.split(";");
        for (final String part : parts) {
            final String[] nodes = part.split("->");
            String current = nodes[0].trim();
            assert !current.isEmpty();
            if (!nodeMapping.containsKey(current)) {
                nodeMapping.put(current, fac.createNode());
            }
            for (int i = 1; i < nodes.length; i++) {
                final String next = nodes[i].trim();
                assert !next.isEmpty();
                if (!nodeMapping.containsKey(next)) {
                    nodeMapping.put(next, fac.createNode());
                }
                fac.connect(nodeMapping.get(current), nodeMapping.get(next));
                current = next;
            }
        }
    }

}
