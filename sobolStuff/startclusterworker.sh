#!/bin/sh
nohup java -cp "./ProcessSimulation/lib/*" -javaagent:./ProcessSimulation/lib/quasar-core-0.7.3-jdk8.jar de.unihannover.se.processSimulation.clusterControl.ClusterWorker tcp://wien.se.uni-hannover.de:61616 $1 >worker.$1.log
