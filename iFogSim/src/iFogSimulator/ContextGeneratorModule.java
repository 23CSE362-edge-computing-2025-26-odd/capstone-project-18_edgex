package iFogSimulator;

import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.SimEntity;

public class ContextGeneratorModule extends SimEntity {

    private String appId;
    private double period;
    private int allocatorId;

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
            String label = generateContextLabel();
            System.out.println("\nContextGeneratorModule is generating new context: " + label);
            System.out.println("----------------------------------------------------------");
            send(allocatorId, 0.0, 2000, label);
            schedule(getId(), period, 1000);
        }
    }

    // <<< MODIFIED: Added a new LOW_CLEAR context >>>
    private String generateContextLabel() {
        double r = Math.random();
        if (r < 0.25) return "CONTEXT_TUPLE_LOW_CLEAR";   // 25% chance for LOW
        if (r < 0.50) return "CONTEXT_TUPLE_HIGH_CLEAR";  // 25% chance for HIGH
        if (r < 0.75) return "CONTEXT_TUPLE_LOW_RAINY";   // 25% chance for HIGH-risk rainy
        return "CONTEXT_TUPLE_MEDIUM_CLOUDY";             // 25% chance for MEDIUM
    }

    @Override
    public void shutdownEntity() {
        System.out.println("ContextGeneratorModule shutdown.");
    }
}