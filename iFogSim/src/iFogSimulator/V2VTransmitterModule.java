package iFogSimulator;

import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

/**
 * V2VTransmitterModule
 *
 * Receives V2V_ALERT signals from allocator and "sends" to other entities (simulated).
 */
public class V2VTransmitterModule extends SimEntity {

    private String appId;
    private int neighborId; // ID of neighbor entity in sim (if any)

    public V2VTransmitterModule(String name, String appId, int neighborId) {
        super(name);
        this.appId = appId;
        this.neighborId = neighborId;
    }

    @Override
    public void startEntity() {
        System.out.println("V2VTransmitterModule started. Ready to transmit V2V alerts.");
    }

    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == 3000) {
            Object data = ev.getData();
            if (data instanceof String && data.equals("V2V_ALERT")) {
                System.out.println("Drowsiness DETECTED by v2v-transmitter! Transmitting V2V_ALERT.");
                // simulate receiving at another entity
                // For demo, just print receiver event
                System.out.println("alert-receiver received context event: V2V_ALERT");
            }
        }
    }

    @Override
    public void shutdownEntity() {
        System.out.println("V2VTransmitterModule shutdown.");
    }
}