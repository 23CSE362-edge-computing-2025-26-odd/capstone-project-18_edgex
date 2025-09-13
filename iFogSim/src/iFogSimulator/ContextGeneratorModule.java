package iFogSimulator;

import org.fog.entities.Tuple;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.SimEntity;

/**
 * ContextGeneratorModule
 *
 * This is a lightweight module that periodically generates context tuples.
 */
public class ContextGeneratorModule extends SimEntity {

    private String appId;
    private double period; // simulation time between events
    private int allocatorId; // ID of the allocator module to send events to

    public ContextGeneratorModule(String name, String appId, double period, int allocatorId) {
        super(name);
        this.appId = appId;
        this.period = period;
        this.allocatorId = allocatorId;
    }

    @Override
    public void startEntity() {
        System.out.println("ContextGeneratorModule is starting. Scheduling first context event at time 1.");
        schedule(getId(), 1.0, 1000);
    }

    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == 1000) {
            // generate a context label
            String label = generateContextLabel();
            System.out.println("\nContextGeneratorModule is generating new context: " + label);
            System.out.println("----------------------------------------------------------");

            // Send an event to the allocator module
            send(allocatorId, 0.0, 2000, label);

            // schedule next generation event
            schedule(getId(), period, 1000);
        }
    }

    private String generateContextLabel() {
        double r = Math.random();
        if (r < 0.25) return "CONTEXT_TUPLE_HIGH_CLEAR";
        if (r < 0.6)  return "CONTEXT_TUPLE_LOW_RAINY";
        return "CONTEXT_TUPLE_MEDIUM_CLOUDY";
    }

    @Override
    public void shutdownEntity() {
        System.out.println("ContextGeneratorModule shutdown.");
    }
}