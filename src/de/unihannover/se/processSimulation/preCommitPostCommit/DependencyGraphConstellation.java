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

import desmoj.core.dist.MersenneTwisterRandomGenerator;

/**
 * The tasks of a story can have dependencies, i.e. they form a dependy graph. This enum
 * provides several generators for different types of these dependency graphs.
 */
public enum DependencyGraphConstellation {

    /**
     * A simple graph where one task is common prequisite of two others.
     */
    SIMPLISTIC {
        @Override
        public GraphGenerator createGenerator(MersenneTwisterRandomGenerator random) {
            final GraphGenerator g = new GraphGenerator(random);
            g.addTemplate("A->B;A->C", 1);
            return g;
        }
    },

    /**
     * The graph with only one node.
     */
    NO_SUBDIVISION {
        @Override
        public GraphGenerator createGenerator(MersenneTwisterRandomGenerator random) {
            final GraphGenerator g = new GraphGenerator(random);
            g.addTemplate("A", 1);
            return g;
        }
    },

    /**
     * A sample of real dependency graphs and task counts from the ticket system of a commercial software producer.
     */
    REALISTIC {
        @Override
        public GraphGenerator createGenerator(MersenneTwisterRandomGenerator random) {
            final GraphGenerator g = new GraphGenerator(random);
            g.addTemplate("A", 14);
            g.addTemplate("A;B", 5);
            g.addTemplate("A->B", 6);
            g.addTemplate("A->B;C", 1);
            g.addTemplate("A->B->C", 2);
            g.addTemplate("A->B;A->C", 2);
            g.addTemplate("A->C;B->C", 1);
            g.addTemplate("A->B->C;A->D", 1);
            g.addTemplate("A->B;A->C;A->D", 1);
            g.addTemplate("A->B;A->C;D", 1);
            g.addTemplate("A->C;A->D;A->E;B->C;B->D", 1);
            g.addTemplate("A->B->C;A->D;A->E;F->C", 1);
            g.addTemplate("A->B;A->C->D;E->D;F->D", 1);
            g.addTemplate("A->B;C->D->E;A->D;F", 1);
            g.addTemplate("A;B;C->D;C->E;C->F->G", 1);
            g.addTemplate("A->B->C;A->D->E;A->F;G", 1);
            g.addTemplate("A->B->C;A->D;A->E;F;G;H", 1);
            g.addTemplate("A;B->C;D->C;D->E;D->F;E->G;E->H", 1);
            g.addTemplate("A->B;A->C;A->D;D->E;D->F;D->G;D->H", 1);
            g.addTemplate("A->B->C->D;E->F->G->D;A->G;E->H;I", 1);
            g.addTemplate("A->B->C->D->E->F;G->E;H->E;I->B;B->J->K->F", 1);
            g.addTemplate("A->B;A->C;A->D;A->E;A->F;E->G->H;E->I;E->J;E->K", 1);
            g.addTemplate("A->B->C->D->E;C->F;C->G;H->I->J;K->I;L", 1);
            g.addTemplate("A->B->C;A->D->C;A->E;A->F;G->F;H;I;J;K;L;M->N;M->O", 1);
            g.addTemplate("A->F;B->F;C->F;D->F;E->F;F->G;F->H;F->I;F->J;H->K;I->K;L;M;N->O;P", 1);
            return g;
        }
    },

    /**
     * A sample of different graphs without any dependencies. The number of tasks per graph
     * is roughly based on the empirical sample from {@link #REALISTIC}.
     */
    NO_DEPENDENCIES {
        @Override
        public GraphGenerator createGenerator(MersenneTwisterRandomGenerator random) {
            final GraphGenerator g = new GraphGenerator(random);
            g.addTemplate("A", 28);
            g.addTemplate("A;B", 22);
            g.addTemplate("A;B;C", 12);
            g.addTemplate("A;B;C;D", 6);
            g.addTemplate("A;B;C;D;E", 3);
            g.addTemplate("A;B;C;D;E;F", 6);
            g.addTemplate("A;B;C;D;E;F;G", 4);
            g.addTemplate("A;B;C;D;E;F;G;H", 5);
            g.addTemplate("A;B;C;D;E;F;G;H;I", 2);
            g.addTemplate("A;B;C;D;E;F;G;H;I;J", 2);
            g.addTemplate("A;B;C;D;E;F;G;H;I;J;K", 4);
            g.addTemplate("A;B;C;D;E;F;G;H;I;J;K;L", 2);
            g.addTemplate("A;B;C;D;E;F;G;H;I;J;K;L;M", 1);
            g.addTemplate("A;B;C;D;E;F;G;H;I;J;K;L;M;N", 1);
            g.addTemplate("A;B;C;D;E;F;G;H;I;J;K;L;M;N;O", 2);
            g.addTemplate("A;B;C;D;E;F;G;H;I;J;K;L;M;N;O;P", 2);
            return g;
        }
    },

    /**
     * A sample of different graphs with the maximum of dependencies: Every graph forms a chain of tasks.
     * The number of tasks per graph is roughly based on the empirical sample from {@link #REALISTIC}.
     */
    CHAINS {
        @Override
        public GraphGenerator createGenerator(MersenneTwisterRandomGenerator random) {
            final GraphGenerator g = new GraphGenerator(random);
            g.addTemplate("A->B", 32);
            g.addTemplate("A->B->C", 20);
            g.addTemplate("A->B->C->D", 12);
            g.addTemplate("A->B->C->D->E", 7);
            g.addTemplate("A->B->C->D->E->F", 6);
            g.addTemplate("A->B->C->D->E->F->G", 4);
            g.addTemplate("A->B->C->D->E->F->G->H", 5);
            g.addTemplate("A->B->C->D->E->F->G->H->I", 2);
            g.addTemplate("A->B->C->D->E->F->G->H->I->J", 2);
            g.addTemplate("A->B->C->D->E->F->G->H->I->J->K", 4);
            g.addTemplate("A->B->C->D->E->F->G->H->I->J->K->L", 2);
            g.addTemplate("A->B->C->D->E->F->G->H->I->J->K->L->M", 1);
            g.addTemplate("A->B->C->D->E->F->G->H->I->J->K->L->M->N", 1);
            g.addTemplate("A->B->C->D->E->F->G->H->I->J->K->L->M->N->O", 2);
            g.addTemplate("A->B->C->D->E->F->G->H->I->J->K->L->M->N->O->P", 2);
            return g;
        }
    },

    /**
     * A sample of different graphs where each graph contains a "diamond" structure of a single start and end task
     * and several tasks that can be done in parallel in between.
     * The number of tasks per graph is roughly based on the empirical sample from {@link #REALISTIC}.
     */
    DIAMONDS {
        @Override
        public GraphGenerator createGenerator(MersenneTwisterRandomGenerator random) {
            final GraphGenerator g = new GraphGenerator(random);
            g.addTemplate("A->C->B", 32);
            g.addTemplate("A->C->B;A->D->B;", 22);
            g.addTemplate("A->C->B;A->D->B;A->E->B", 13);
            g.addTemplate("A->C->B;A->D->B;A->E->B;A->F->B", 10);
            g.addTemplate("A->C->B;A->D->B;A->E->B;A->F->B;A->G->B", 4);
            g.addTemplate("A->C->B;A->D->B;A->E->B;A->F->B;A->G->B;A->H->B", 5);
            g.addTemplate("A->C->B;A->D->B;A->E->B;A->F->B;A->G->B;A->H->B;A->I->B", 2);
            g.addTemplate("A->C->B;A->D->B;A->E->B;A->F->B;A->G->B;A->H->B;A->I->B;A->J->B", 2);
            g.addTemplate("A->C->B;A->D->B;A->E->B;A->F->B;A->G->B;A->H->B;A->I->B;A->J->B;A->K->B", 4);
            g.addTemplate("A->C->B;A->D->B;A->E->B;A->F->B;A->G->B;A->H->B;A->I->B;A->J->B;A->K->B;A->L->B", 2);
            g.addTemplate("A->C->B;A->D->B;A->E->B;A->F->B;A->G->B;A->H->B;A->I->B;A->J->B;A->K->B;A->L->B;A->M->B", 1);
            g.addTemplate("A->C->B;A->D->B;A->E->B;A->F->B;A->G->B;A->H->B;A->I->B;A->J->B;A->K->B;A->L->B;A->M->B;A->N->B", 1);
            g.addTemplate("A->C->B;A->D->B;A->E->B;A->F->B;A->G->B;A->H->B;A->I->B;A->J->B;A->K->B;A->L->B;A->M->B;A->N->B;A->O->B", 2);
            g.addTemplate("A->C->B;A->D->B;A->E->B;A->F->B;A->G->B;A->H->B;A->I->B;A->J->B;A->K->B;A->L->B;A->M->B;A->N->B;A->O->B;A->P->B", 2);
            return g;
        }
    };

    /**
     * Returns a {@link GraphGenerator} for the enum value'S type of structure, using the given random number generator.
     */
    abstract GraphGenerator createGenerator(MersenneTwisterRandomGenerator random);

}
