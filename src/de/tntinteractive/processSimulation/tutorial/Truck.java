package de.tntinteractive.processSimulation.tutorial;

import desmoj.core.simulator.Model;
import desmoj.core.simulator.SimProcess;
/**
 * This class represents the truck in the ProcessesExample
 * model.
 *
 * A truck arrives at the container terminal and requests
 * loading of a container. If possible, it is served
 * by the van carrier immediately. Otherwise it waits in the
 * parking area for its turn.
 * After service is completed, it leaves the system.
 */
public class Truck extends SimProcess {

    /** a reference to the model this process is a part of
     *  useful shortcut to access the model's static components
     */
    private final ProcessesExample myModel;
    /**
     * Constructor of the truck process
     *
     * Used to create a new truck to be serviced by a van carrier.
     *
     * @param owner the model this process belongs to
     * @param name this truck's name
     * @param showInTrace flag to indicate if this process shall produce
     *                    output for the trace
     */
    public Truck(Model owner, String name, boolean showInTrace) {

        super(owner, name, showInTrace);
        // store a reference to the model this truck is associated with
        this.myModel = (ProcessesExample)owner;
    }
    /**
     * Describes this truck's life cycle:
     *
     * On arrival, the truck will enter the queue (parking lot).
     * It will then check if the van carrier is available.
     * If this is the case, it will activate the van carrier to
     * get serviced and transfer the control to the VC.
     * Otherwise it just passivates (waits).
     * After service it leaves the system.
     */
    @Override
    public void lifeCycle() {

        // enter parking-lot
        this.myModel.truckQueue.insert(this);
        this.sendTraceNote("TruckQueuelength: "+ this.myModel.truckQueue.length());

        // check if a VC is available
        if (!this.myModel.idleVCQueue.isEmpty()) {
            // yes, it is

            // get a reference to the first  VC from the idle VC queue
            final VanCarrier vanCarrier = this.myModel.idleVCQueue.removeFirst();

            // place the VC on the eventlist right after me,
            // to ensure that I will be the next customer to get serviced
            vanCarrier.activateAfter(this);
        }

        // wait for service
        this.passivate();

        // Ok, I am back online again, which means I was serviced
        // by the VC. I can leave the systems now.
        // Luckily I don't have to do anything more than sending
        // a message to the trace file, because the
        // Java VM garbage collector will get the job done.
        // Bye!
        this.sendTraceNote("Truck was serviced and leaves system.");
    }
}