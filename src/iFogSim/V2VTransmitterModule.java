    package iFogSim;

    import org.cloudbus.cloudsim.core.CloudSim;
    import org.fog.application.AppModule;
    import org.fog.entities.Tuple;
    import org.fog.scheduler.TupleScheduler;
    import org.fog.utils.FogEvents;
    import org.fog.utils.FogUtils;

    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Random;

    public class V2VTransmitterModule extends AppModule {

        private int nearbyVehicleId;
        private Random random = new Random();

        public V2VTransmitterModule(String name, String appId, int userId, int nearbyVehicleId) {
            super(FogUtils.generateEntityId(), name, appId, userId, 100, 10, 1000, 10000, "VMM", new TupleScheduler(100, 1), new HashMap<>());
            this.nearbyVehicleId = nearbyVehicleId;
        }

        @Override
        public List<Tuple> getResultantTuples(String inputTupleType, Tuple inputTuple, int sourceDeviceId, int sourceModuleId) {
            if (inputTupleType.equals("DROWSINESS_ALERT")) {
                // Simulate a 10% chance of drowsiness being detected during each check
                if (random.nextDouble() < 0.1) {
                    System.out.println("Drowsiness DETECTED by " + getName() + "! Transmitting V2V_ALERT.");

                    Tuple v2vTuple = new Tuple(getAppId(), FogUtils.generateTupleId(), Tuple.UP, 10, 1, 20, 100, null, null, null);
                    v2vTuple.setTupleType("V2V_ALERT");
                    v2vTuple.setSrcModuleName(getName());
                    v2vTuple.setDestModuleName("alert-receiver");

                    // Send the tuple DIRECTLY to the nearby vehicle, bypassing normal routing
                    CloudSim.send(getId(), nearbyVehicleId, 5.0, FogEvents.TUPLE_ARRIVAL, v2vTuple);
                } else {
                    System.out.println("Drowsiness check by " + getName() + ": Driver is awake. No alert sent.");
                }
            }
            // Return an empty list because we handled the sending manually
            return new ArrayList<>();
        }
    }