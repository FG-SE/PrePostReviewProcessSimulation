package de.tntinteractive.processSimulation.tutorial;

import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.Model;
import desmoj.core.simulator.SimProcess;
import desmoj.core.simulator.TimeSpan;
/**
 * This class represents a process source, which continually generates
 * trucks in order to keep the simulation running.
 *
 * It will create a new truck, activate it (so that it arrives at
 * the terminal) and then wait until the next truck arrival is
 * due.
 */
public class TruckGenerator extends SimProcess {

    /**
     * TruckGenerator constructor comment.
     * @param owner the model this truck generator belongs to
     * @param name this truck generator's name
     * @param showInTrace flag to indicate if this process shall produce output
     *                    for the trace
     */
    public TruckGenerator(Model owner, String name, boolean showInTrace) {
        super(owner, name, showInTrace);
    }

    /**
     * describes this process's life cycle: continually generate new trucks.
     */
    @Override
    public void lifeCycle() {

        // get a reference to the model
        final ProcessesExample model = (ProcessesExample)this.getModel();

        // endless loop:
        while (true) {

            // create a new truck
            // Parameters:
            // model       = it's part of this model
            // "Truck"     = name of the object
            // true        = yes please, show the truck in trace file
            final Truck truck = new Truck(model, "Truck", true);

            // now let the newly created truck roll on the parking-lot
            // which means we will activate it after this truck generator
            truck.activateAfter(this);

            // wait until next truck arrival is due
            this.hold(new TimeSpan(model.getTruckArrivalTime(), TimeUnit.MINUTES));
            // from inside to outside...
            // we draw a new inter-arrival time
            // we make a TimeSpan object out of it and
            // we wait for exactly this period of time
        }
    }
}
