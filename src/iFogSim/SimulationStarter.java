package iFogSim;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.placement.Controller;
import org.fog.entities.Sensor;
import java.util.List;

public class SimulationStarter extends SimEntity {

    private List<Sensor> sensors;
    private int controllerId;

    public SimulationStarter(String name, List<Sensor> sensors, int controllerId) {
        super(name);
        this.sensors = sensors;
        this.controllerId = controllerId;
    }

    @Override
    public void startEntity() {
        // Send a message to ourselves with a 1-second delay.
        // This ensures all other entities have finished their own startup routines.
        send(getId(), 1.0, CloudSimTags.VM_CREATE);
    }

    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == CloudSimTags.VM_CREATE) {
            System.out.println("Simulation Starter: Kicking off sensor events...");
            // Manually kick-start each sensor by sending a startup event to the controller
            for(Sensor sensor : sensors){
                send(controllerId, sensor.getTransmitDistribution().getNextValue(), CloudSimTags.VM_CREATE, sensor);
            }
        }
    }

    @Override
    public void shutdownEntity() {}
}