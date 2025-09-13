package iFogSimulator;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.entities.*;
import org.fog.placement.Controller; // <<< CORRECTED IMPORT
import org.fog.placement.ModuleMapping; // <<< ADDED IMPORT
import org.fog.placement.ModulePlacement;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * VehicleSimulation.java
 *
 * This version is corrected to be fully compatible with your specific iFogSim library.
 */
public class VehicleSimulation {

    static List<FogDevice> fogDevices = new ArrayList<>();
    static List<Sensor> sensors = new ArrayList<>();
    static List<Actuator> actuators = new ArrayList<>();

    public static void main(String[] args) {
        try {
            System.out.println("Starting Vehicular Edge Simulation...");

            // 1) Initialize CloudSim
            int numUser = 1;
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false;
            CloudSim.init(numUser, calendar, traceFlag);

            // 2) Create a Broker to get a valid user ID
            FogBroker broker = new FogBroker("broker");
            int brokerId = broker.getId();

            // 3) Create Fog devices
            createFogDevices();

            // 4) Create the Controller
            Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);

            // 5) Create the Application
            Application application = createApplication("vehicular-app", brokerId);

            // 6) Create Module Mapping and Placement Strategy
            // Your ModulePlacementEdgewards constructor requires a ModuleMapping object.
            // We create an empty one to allow it to calculate the placement dynamically.
            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
            ModulePlacement modulePlacement = new ModulePlacementEdgewards(fogDevices, sensors, actuators, application, moduleMapping);

            // 7) Submit the application to the Controller
            controller.submitApplication(application, 0, modulePlacement);

            // 8) Create the custom SimEntity modules and link them
            V2VTransmitterModule v2v = new V2VTransmitterModule("v2v-transmitter", application.getAppId(), -1);
            CloudSim.addEntity(v2v);

            AdaptiveAllocatorModule allocator = new AdaptiveAllocatorModule("adaptive-allocator", application.getAppId(), v2v.getId());
            CloudSim.addEntity(allocator);

            ContextGeneratorModule contextGenerator = new ContextGeneratorModule("context-generator", application.getAppId(), 2.0, allocator.getId());
            CloudSim.addEntity(contextGenerator);

            // 9) Start simulation
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            // 10) Print final results
            // Your Controller class prints all results automatically when the simulation stops.
            // We removed the manual print statements from here to prevent errors and duplicate output.
            System.out.println("\nVehicular Edge Simulation finished!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createFogDevices() {
        FogDevice cloud = createFogDevice("cloud", 10000, 40000, 100, 10000, 0, 0.01, 107.339, 83.4333);
        cloud.setParentId(-1);
        fogDevices.add(cloud);

        FogDevice carEdge1 = createFogDevice("car-edge-1", 4000, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
        carEdge1.setParentId(cloud.getId());
        carEdge1.setUplinkLatency(100);
        fogDevices.add(carEdge1);

        FogDevice nearbyVehicle2 = createFogDevice("nearby-vehicle-2", 3000, 2000, 10000, 10000, 2, 0.0, 87.53, 82.44);
        nearbyVehicle2.setParentId(carEdge1.getId());
        nearbyVehicle2.setUplinkLatency(2);
        fogDevices.add(nearbyVehicle2);
    }

    private static FogDevice createFogDevice(String nodeName, long mips,
                                             int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(mips)));

        int hostId = FogUtils.generateEntityId();
        long storage = 1000000;
        int bw = 10000;

        PowerHost host = new PowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList,
                new VmSchedulerTimeShared(peList),
                new FogLinearPowerModel(busyPower, idlePower)
        );

        List<Host> hostList = new ArrayList<>();
        hostList.add(host);

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;
        LinkedList<Storage> storageList = new LinkedList<>();

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        FogDevice fogdevice = null;
        try {
            // <<< CORRECTED CONSTRUCTOR CALL
            // Your FogDevice constructor does not take a PowerModel as the last argument.
            // The power model is already included in the PowerHost inside the 'characteristics'.
            fogdevice = new FogDevice(nodeName, characteristics,
                    new VmAllocationPolicySimple(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);

        } catch (Exception e) {
            e.printStackTrace();
        }

        fogdevice.setLevel(level);
        return fogdevice;
    }

    @SuppressWarnings({"serial"})
    private static Application createApplication(String appId, int userId) {
        Application application = Application.createApplication(appId, userId);

        application.addAppModule("context-module", 10);
        application.addAppModule("allocator-module", 10);
        application.addAppModule("v2v-module", 10);

        application.addAppEdge("SENSOR_DATA", "context-module", 1000, 500, "SENSOR_DATA", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("context-module", "allocator-module", 2000, 500, "CONTEXT_TUPLE", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("allocator-module", "v2v-module", 500, 500, "V2V_ALERT", Tuple.UP, AppEdge.MODULE);

        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {{
            add("SENSOR_DATA");
            add("context-module");
            add("allocator-module");
            add("v2v-module");
        }});

        List<AppLoop> loops = new ArrayList<AppLoop>() {{
            add(loop1);
        }};

        application.setLoops(loops);

        return application;
    }
}