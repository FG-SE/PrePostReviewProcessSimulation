package de.tntinteractive.processSimulation.tutorial;

import java.util.concurrent.TimeUnit;

import desmoj.core.simulator.Model;
import desmoj.core.simulator.SimProcess;
import desmoj.core.simulator.TimeSpan;
/**
 * This class represents a van carrier in the ProcessesExample
 * model.
 * The VC waits until a truck requests its service.
 * It will then fetch the container and load it onto
 * the truck.
 * If there is another truck waiting it starts serving this
 * truck. Otherwise it waits again for the next truck to arrive.
 */
public class VanCarrier extends SimProcess {

    /** a reference to the model this process is a part of
     *  useful shortcut to access the model's static components
     */
    private final ProcessesExample myModel;
    /**
     * Constructor of the van carrier process
     *
     * Used to create a new VC to serve trucks.
     *
     * @param owner the model this process belongs to
     * @param name this VC's name
     * @param showInTrace flag to indicate if this process shall produce output
     *                    for the trace
     */
    public VanCarrier(Model owner, String name, boolean showInTrace) {

        super(owner, name, showInTrace);
        // store a reference to the model this VC is associated with
        this.myModel = (ProcessesExample)owner;
    }

    /**
     * Describes this van carrier's life cycle.
     *
     * It will continually loop through the following steps:
     * Check if there is a customer waiting.
     * If there is someone waiting
     *   a) remove customer from queue
     *   b) serve customer
     *   c) return to top
     * If there is no one waiting
     *   a) wait (passivate) until someone arrives (who reactivates the VC)
     *   b) return to top
     */
    @Override
    public void lifeCycle() {

        // the server is always on duty and will never stop working
        while (true) {
            // check if there is someone waiting
            if (this.myModel.truckQueue.isEmpty()) {
                // NO, there is no one waiting

                // insert yourself into the idle VC queue
                this.myModel.idleVCQueue.insert(this);
                // and wait for things to happen
                this.passivate();
            } else {
                // YES, there is a customer (truck) waiting

                // get a reference to the first truck from the truck queue
                final Truck nextTruck = this.myModel.truckQueue.removeFirst();

                // now serve it (fetch container and load it onto truck)
                // service time is represented by a hold of the VC process
                this.hold(new TimeSpan(this.myModel.getServiceTime(), TimeUnit.MINUTES));
                // from inside to outside...
                // ...draw a new period of service time
                // ...make a TimeSpan object out of it
                // ...and hold for this amount of time

                // now the truck has received its container and can leave
                // we will reactivate it, to allow it to finish its life cycle
                nextTruck.activate();
                // the VC can return to top and check for a new customer
            }
        }
    }

}
