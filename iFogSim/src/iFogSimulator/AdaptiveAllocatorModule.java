package iFogSimulator;

import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

/**
 * AdaptiveAllocatorModule - UPDATED
 *
 * Receives context, calls the GA server for priority,
 * calls the Fuzzy server for allocation, and instructs the V2V module if needed.
 */
public class AdaptiveAllocatorModule extends SimEntity {

    private String appId;
    private PythonClient pythonClient;
    private int v2vTransmitterId;

    public AdaptiveAllocatorModule(String name, String appId, int v2vTransmitterId) {
        super(name);
        this.appId = appId;
        this.pythonClient = new PythonClient(); // NEW: Simpler constructor
        this.v2vTransmitterId = v2vTransmitterId;
    }

    @Override
    public void startEntity() {
        System.out.println("AdaptiveAllocatorModule started.");
    }

    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == 2000) {
            String contextLabel = (String) ev.getData();
            System.out.println("adaptive-allocator-module received context event: " + contextLabel);

            try {
                // --- NEW WORKFLOW ---
                // Step 1: Call GA server to get priority label
                // For demo, we use fixed inputs, but you could parse them from contextLabel
                System.out.println("AI BRAIN: Querying GA server for priority...");
                String priorityLabel = pythonClient.predictPriority(0.8, 0.1, 85.0, 0.6);
                System.out.println("AI BRAIN: Received Priority = " + priorityLabel);

                // Step 2: Call Fuzzy Logic server with the label to get resource allocation
                System.out.println("AI BRAIN: Querying Fuzzy server for resource allocation...");
                String allocationDecision = pythonClient.allocateResources(priorityLabel);
                System.out.println("AI BRAIN: Received Action = " + allocationDecision);

                // Step 3: Act based on the priority
                if ("HIGH".equalsIgnoreCase(priorityLabel)) {
                    System.out.println("AI BRAIN: High risk detected! ACTION: Activate critical systems.");
                    send(v2vTransmitterId, 0.0, 3000, "V2V_ALERT");
                }

            } catch (Exception e) {
                System.out.println("Could not connect to a Python CI server. Error: " + e.getMessage());
            }

        } else if (ev.getTag() == 9999) {
            System.out.println("AdaptiveAllocatorModule shutting down.");
        }
    }

    @Override
    public void shutdownEntity() {
        System.out.println("AdaptiveAllocatorModule stopped.");
    }
}