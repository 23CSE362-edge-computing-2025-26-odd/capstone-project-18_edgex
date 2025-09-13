package iFogSimulator;

import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

/**
 * AdaptiveAllocatorModule
 *
 * Receives context, computes a priority score, and instructs the V2V module if needed.
 */
public class AdaptiveAllocatorModule extends SimEntity {

    private String appId;
    private PythonClient pythonClient;
    private int v2vTransmitterId; // ID of the V2V module to send alerts to

    public AdaptiveAllocatorModule(String name, String appId, int v2vTransmitterId) {
        super(name);
        this.appId = appId;
        this.pythonClient = new PythonClient("http://127.0.0.1:5000");
        this.v2vTransmitterId = v2vTransmitterId;
    }

    @Override
    public void startEntity() {
        System.out.println("AdaptiveAllocatorModule started.");
    }

    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == 2000) {
            Object data = ev.getData();
            if (data instanceof String) {
                String contextLabel = (String) data;
                System.out.println("adaptive-allocator-module received context event: " + contextLabel);

                // local simple priority computation (fallback)
                double priority = computePriorityFromLabel(contextLabel);

                // Optionally, call Python model to refine
                try {
                    double pyPriority = pythonClient.predictPriority(0.5, 0.2, 60.0, 0.3);
                    // average them for demo
                    priority = (priority + pyPriority) / 2.0;
                } catch (Exception e) {
                    // ignore if Python server not available; keep local priority
                    System.out.println("Could not connect to Python server. Using local priority only.");
                }

                System.out.printf("AI BRAIN: Calculated Priority Score = %.2f%n", priority);
                if (priority >= 0.75) {
                    System.out.println("AI BRAIN: High risk detected! ACTION: Activate critical systems.");
                    // send V2V alert to the V2V transmitter module
                    send(v2vTransmitterId, 0.0, 3000, "V2V_ALERT");
                } else if (priority >= 0.35) {
                    System.out.println("AI BRAIN: Medium risk. ACTION: Suggest slow down / conserve energy.");
                } else {
                    System.out.println("AI BRAIN: Low risk. ACTION: Conserve energy.");
                }
            }
        } else if (ev.getTag() == 9999) {
            // shutdown
            System.out.println("AdaptiveAllocatorModule shutting down.");
        }
    }

    private double computePriorityFromLabel(String label) {
        switch (label) {
            case "CONTEXT_TUPLE_HIGH_CLEAR": return 0.68;
            case "CONTEXT_TUPLE_LOW_RAINY": return 0.45;
            case "CONTEXT_TUPLE_MEDIUM_CLOUDY": return 0.30;
            default: return 0.10;
        }
    }

    @Override
    public void shutdownEntity() {
        System.out.println("AdaptiveAllocatorModule stopped.");
    }
}