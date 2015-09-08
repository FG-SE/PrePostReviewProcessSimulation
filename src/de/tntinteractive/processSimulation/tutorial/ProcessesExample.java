package de.tntinteractive.processSimulation.tutorial;

import java.util.concurrent.TimeUnit;

import desmoj.core.dist.ContDistExponential;
import desmoj.core.dist.ContDistUniform;
import desmoj.core.simulator.Experiment;
import desmoj.core.simulator.Model;
import desmoj.core.simulator.ProcessQueue;
import desmoj.core.simulator.TimeInstant;
import desmoj.core.simulator.TimeSpan;

/**
 * This is the model class. It is the main class of a simple process-oriented
 * model of the loading zone of a container terminal. Trucks arrive at the
 * terminal to load containers. They wait in line until a van carrier (VC) is
 * available to fetch a certain container and load it onto the truck. After
 * loading is completed, the truck leaves the terminal while the van carrier
 * serves the next truck.
 */
public class ProcessesExample extends Model {


    /**
     * Model parameter: the number of van carriers
     */
    protected static int NUM_VC = 2;
    /**
     * Random number stream used to draw an arrival time for the next truck.
     * See init() method for stream parameters.
     */
    private desmoj.core.dist.ContDistExponential truckArrivalTime;
    /**
     * Random number stream used to draw a service time for a truck.
     * Describes the time needed by the VC to fetch and load the container
     * onto the truck.
     * See init() method for stream parameters.
     */
    private desmoj.core.dist.ContDistUniform serviceTime;
    /**
     * A waiting queue object is used to represent the parking area for
     * the trucks.
     * Every time a truck arrives it is inserted into this queue (it parks)
     * and will be removed by the VC for service.
     *
     * This way all necessary basic statistics are monitored by the queue.
     */
    protected desmoj.core.simulator.ProcessQueue<Truck> truckQueue;
    /**
     * A waiting queue object is used to represent the parking spot for
     * the VC.
     * If there is no truck waiting for service the VC will return here
     * and wait for the next truck to come.
     *
     * This way all idle time statistics of the VC are monitored by the queue.
     */
    protected desmoj.core.simulator.ProcessQueue<VanCarrier> idleVCQueue;

    /**
     * ProcessesExample constructor.
     *
     * Creates a new ProcessesExample model via calling
     * the constructor of the superclass.
     *
     * @param owner the model this model is part of (set to null when there is
     *              no such model)
     * @param modelName this model's name
     * @param showInReport flag to indicate if this model shall produce output
     *                     to the report file
     * @param showInTrace flag to indicate if this model shall produce output
     *                    to the trace file
     */
    public ProcessesExample(Model owner, String modelName, boolean showInReport, boolean showInTrace) {
        super(owner, modelName, showInReport, showInTrace);
    }

    /**
     * Returns a description of the model to be used in the report.
     * @return model description as a string
     */
    @Override
    public String description() {
        return "This model describes a queueing system located at a "+
                        "container terminal. Trucks will arrive and "+
                        "require the loading of a container. A van carrier (VC) is "+
                        "on duty and will head off to find the required container "+
                        "in the storage. It will then load the container onto the "+
                        "truck. Afterwards, the truck leaves the terminal. "+
                        "In case the VC is busy, the truck waits "+
                        "for its turn on the parking-lot. "+
                        "If the VC is idle, it waits on its own parking spot for the "+
                        "truck to come.";
    }

    /**
     * Activates dynamic model components (simulation processes).
     *
     * This method is used to place all events or processes on the
     * internal event list of the simulator which are necessary to start
     * the simulation.
     *
     * In this case, the truck generator and the van carrier(s) have to be
     * created and activated.
     */
    @Override
    public void doInitialSchedules() {
        // create and activate the van carrier(s)
        for (int i=0; i < NUM_VC; i++)
        {
            final VanCarrier vanCarrier = new VanCarrier(this, "Van Carrier", true);
            vanCarrier.activate(new TimeSpan(0));
            // Use TimeSpan to activate a process after a span of time relative to actual simulation time,
            // or use TimeInstant to activate the process at an absolute point in time.
        }

        // create and activate the truck generator process
        final TruckGenerator generator = new TruckGenerator(this,"TruckArrival",false);
        generator.activate(new TimeSpan(0));
    }

    /**
     * Initialises static model components like distributions and queues.
     */
    @Override
    public void init() {
        // initialise the serviceTimeStream
        // Parameters:
        // this                = belongs to this model
        // "ServiceTimeStream" = the name of the stream
        // 3.0                 = minimum time in minutes to deliver a container
        // 7.0                 = maximum time in minutes to deliver a container
        // true                = show in report?
        // false               = show in trace?
        this.serviceTime= new ContDistUniform(this, "ServiceTimeStream", 3.0, 7.0, true, false);

        // initialise the truckArrivalTimeStream
        // Parameters:
        // this                     = belongs to this model
        // "TruckArrivalTimeStream" = the name of the stream
        // 3.0                      = mean time in minutes between arrival of trucks
        // true                     = show in report?
        // false                    = show in trace?
        this.truckArrivalTime= new ContDistExponential(this, "TruckArrivalTimeStream", 3.0, true, false);

        // necessary because an inter-arrival time can not be negative, but
        // a sample of an exponential distribution can...
        this.truckArrivalTime.setNonNegative(true);

        this.truckQueue = new ProcessQueue<>(this, "Truck queue", true, true);
        this.idleVCQueue = new ProcessQueue<>(this, "VC queue", true, true);
    }

    /**
     * Returns a sample of the random stream used to determine the
     * time needed to fetch the container for a truck from the
     * storage area and the time the VC needs to load it onto the truck.
     *
     * @return double a serviceTime sample
     */
    public double getServiceTime() {
        return this.serviceTime.sample();
    }
    /**
     * Returns a sample of the random stream used to determine
     * the next truck arrival time.
     *
     * @return double a truckArrivalTime sample
     */
    public double getTruckArrivalTime() {
        return this.truckArrivalTime.sample();
    }

    /**
     * Runs the model.
     *
     * @param args is an array of command-line arguments (will be ignored here)
     */
    public static void main(java.lang.String[] args) {

        // create model and experiment
        final ProcessesExample model = new ProcessesExample(null,
                        "Simple Process-Oriented Van Carrier Model", true, true);
        // null as first parameter because it is the main model and has no mastermodel

        final Experiment exp = new Experiment("ProcessExampleExperiment",
                        TimeUnit.SECONDS, TimeUnit.MINUTES, null);
        // ATTENTION, since the name of the experiment is used in the names of the
        // output files, you have to specify a string that's compatible with the
        // filename constraints of your computer's operating system. The remaing three
        // parameters specify the granularity of simulation time, default unit to
        // display time and the time formatter to use (null yields a default formatter).

        // connect both
        model.connectToExperiment(exp);

        // set experiment parameters
        exp.setShowProgressBar(false);
        exp.stop(new TimeInstant(1500, TimeUnit.MINUTES));   // set end of simulation at 1500 minutes
        exp.tracePeriod(new TimeInstant(0), new TimeInstant(100, TimeUnit.MINUTES));
        // set the period of the trace
        exp.debugPeriod(new TimeInstant(0), new TimeInstant(50, TimeUnit.MINUTES));   // and debug output
        // ATTENTION!
        // Don't use too long periods. Otherwise a huge HTML page will
        // be created which crashes Netscape :-)

        // start the experiment at simulation time 0.0
        exp.start();

        // --> now the simulation is running until it reaches its end criterion
        // ...
        // ...
        // <-- afterwards, the main thread returns here

        // generate the report (and other output files)
        exp.report();

        // stop all threads still alive and close all output files
        exp.finish();
    }
}