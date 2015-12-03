#!/bin/sh
nohup java -cp "./ProcessSimulation/lib/*" de.unihannover.se.processSimulation.clusterControl.ClusterControl tcp://wien.se.uni-hannover.de:61616 params.txt sobolParameterSets.txt clusterResultDir >control.log
