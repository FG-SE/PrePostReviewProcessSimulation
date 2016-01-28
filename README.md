# PrePostReviewProcessSimulation
Contains the model (and management code) for a study to compare pre commit and post commit reviews using discrete event simulation.
It is built using DESMO-J and uses Quasar fibers to speed up execution. It can be used interactively, in bulk on a computer cluster or locally.

For more information, contact tobias.baum (at) inf.uni-hannover.de

## Building
The program is built using Gradle:
gradlew installDist

## Using the Web-GUI
The main class de.unihannover.se.processSimulation.interactive.ServerMain starts a web GUI for interactive simulation.
Don't forget to activate the quasar agent using the command line option for the JVM.

## Using the bulk execution cluster
Cluster workers and cluster control communicate using Apache ActiveMQ.
The main class de.unihannover.se.processSimulation.clusterControl.ClusterWorker starts a worker.
There are several types of control nodes:
de.unihannover.se.processSimulation.clusterControl.ClusterControl for pre-created data (e.g. sensitivity analysis),
de.unihannover.se.processSimulation.clusterControl.MiningGuidedClusterControl for random data or data generation guided by data mining,
de.unihannover.se.processSimulation.clusterControl.MixingClusterControl for data obtained by mixing points with opposite outcomes

## Further tools
The package de.unihannover.se.processSimulation.postprocessing contains some further tools, e.g. for local sensitivity analysis.