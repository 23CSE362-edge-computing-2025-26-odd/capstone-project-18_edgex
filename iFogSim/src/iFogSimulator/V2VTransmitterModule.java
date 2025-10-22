package iFogSimulator;

import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

public class V2VTransmitterModule extends SimEntity {

    private String appId;
    private int neighborId;

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
            // <<< MODIFIED: Handle the specific alert message >>>
            if (data instanceof String && ((String) data).startsWith("V2V_ALERT")) {
                String alertMessage = (String) data;
                String reason = alertMessage.replace("V2V_ALERT: ", "");
                System.out.println("V2V-TRANSMITTER: Broadcasting alert! REASON: " + reason);
                System.out.println("ALERT-RECEIVER: Received alert: " + reason);
            }
        }
    }

    @Override
    public void shutdownEntity() {
        System.out.println("V2VTransmitterModule shutdown.");
    }
}