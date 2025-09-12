package iFogSim;

import java.util.*;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.*;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.fog.application.AppEdge;
import org.fog.application.Application;
import org.fog.application.AppModule;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.*;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.TupleScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;
import org.cloudbus.cloudsim.core.CloudSimTags;

public class vehicleSimulation {

    static List<FogDevice> fogDevices = new ArrayList<>();
    static List<Sensor> sensors = new ArrayList<>();
    static String appId = "EdgeDriveApp";

    public static void main(String[] args) {
        try {
            Log.disable();
            CloudSim.init(1, Calendar.getInstance(), false);
            FogBroker broker = new FogBroker("broker");

            Application application = createApplication(appId, broker.getId());
            createFogDevices();

            Controller controller = new Controller("master-controller", fogDevices, sensors, new ArrayList<>());

            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
            moduleMapping.addModuleToDevice("context-generator-module", "car-edge-1");
            moduleMapping.addModuleToDevice("adaptive-allocator-module", "car-edge-1");
            moduleMapping.addModuleToDevice("v2v-transmitter", "car-edge-1");
            moduleMapping.addModuleToDevice("alert-receiver", "nearby-vehicle-2");

            controller.submitApplication(application, new ModulePlacementMapping(fogDevices, application, moduleMapping));

            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

            // --- MANUAL SENSOR STARTUP ---
            // This bypasses the controller and directly starts the sensor event loop.
            for(Sensor sensor : sensors){
                // CORRECT
                CloudSim.send(controller.getId(), sensor.getId(), 0.1, CloudSimTags.VM_CREATE, null);
            }
            // ---------------------------

            System.out.println("Starting Vehicular Edge Simulation...");
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            System.out.println("Vehicular Edge Simulation finished!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Application createApplication(String appId, int userId) {
        Application application = Application.createApplication(appId, userId);

        application.addAppModule(new ContextGeneratorModule("context-generator-module", appId, userId));
        application.addAppModule(new AdaptiveAllocatorModule("adaptive-allocator-module", appId, userId));
        // We do not need the custom V2VTransmitterModule with this fix
        application.addAppModule(new AppModule(FogUtils.generateEntityId(), "v2v-transmitter", appId, userId, 100, 10, 1000, 10000, "VMM", new TupleScheduler(100, 1), new HashMap<>()));
        application.addAppModule(new AppModule(FogUtils.generateEntityId(), "alert-receiver", appId, userId, 100, 10, 1000, 10000, "VMM", new TupleScheduler(100, 1), new HashMap<>()));

        application.addAppEdge("PROXIMITY_DATA", "context-generator-module", 50, 10, "PROXIMITY_DATA", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("VISIBILITY_DATA", "context-generator-module", 50, 10, "VISIBILITY_DATA", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("DROWSINESS_ALERT", "v2v-transmitter", 50, 20, "DROWSINESS_ALERT", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("context-generator-module", "adaptive-allocator-module", 100, 50, "CONTEXT_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("v2v-transmitter", "alert-receiver", 10, 20, "V2V_ALERT", Tuple.UP, AppEdge.MODULE);

        application.addTupleMapping("context-generator-module", "PROXIMITY_DATA", "CONTEXT_TUPLE", new FractionalSelectivity(1.0));
        application.addTupleMapping("context-generator-module", "VISIBILITY_DATA", "CONTEXT_TUPLE", new FractionalSelectivity(1.0));
        application.addTupleMapping("v2v-transmitter", "DROWSINESS_ALERT", "V2V_ALERT", new FractionalSelectivity(1.0));

        sensors.add(new Sensor("proximity-sensor", "PROXIMITY_DATA", userId, appId, new DeterministicDistribution(5.0)));
        sensors.add(new Sensor("visibility-sensor", "VISIBILITY_DATA", userId, appId, new DeterministicDistribution(10.0)));
        sensors.add(new Sensor("drowsiness-sensor", "DROWSINESS_ALERT", userId, appId, new DeterministicDistribution(30.0)));

        return application;
    }

    private static void createFogDevices() {
        FogDevice cloud = createFogDevice("cloud", 44800, 40000, 10000, 0.01, 16*103, 16*83.25);
        fogDevices.add(cloud);

        FogDevice carEdgeDevice = createFogDevice("car-edge-1", 4000, 4000, 10000, 0.0, 87.53, 82.44);
        carEdgeDevice.setParentId(cloud.getId());
        carEdgeDevice.setUplinkLatency(100.0);
        carEdgeDevice.setUplinkBandwidth(5000);
        fogDevices.add(carEdgeDevice);

        FogDevice nearbyVehicle = createFogDevice("nearby-vehicle-2", 2000, 4000, 10000, 0.0, 87.53, 82.44);
        nearbyVehicle.setParentId(cloud.getId());
        nearbyVehicle.setUplinkLatency(150.0);
        fogDevices.add(nearbyVehicle);

        if (!sensors.isEmpty()) {
            for(Sensor sensor : sensors)
                sensor.setGatewayDeviceId(carEdgeDevice.getId());
        }
    }

    private static FogDevice getFogDeviceByName(String name) {
        for (FogDevice device : fogDevices) {
            if (device.getName().equals(name)) {
                return device;
            }
        }
        return null;
    }

    private static FogDevice createFogDevice(String nodeName, long mips, int ram, long bw,
                                             double ratePerMips, double busyPower, double idlePower) {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(mips)));
        int hostId = FogUtils.generateEntityId();
        PowerHost host = new PowerHost(
                hostId, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), 1000000,
                peList, new VmSchedulerTimeShared(peList), new FogLinearPowerModel(busyPower, idlePower)
        );
        List<Host> hostList = new ArrayList<>();
        hostList.add(host);
        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                "x86", "Linux", "Xen", host, 10.0, 3.0, 0.05, 0.001, 0.0
        );
        FogDevice fogdevice = null;
        try {
            fogdevice = new FogDevice(
                    nodeName, characteristics, new AppModuleAllocationPolicy(hostList),
                    new LinkedList<>(), 10.0, bw, bw, 0, ratePerMips
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fogdevice;
    }
}