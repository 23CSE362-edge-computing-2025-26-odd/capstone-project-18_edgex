package iFogSimulator;

import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Random; // Import Random

public class AdaptiveAllocatorModule extends SimEntity {

    private String appId;
    private PythonClient pythonClient;
    private int v2vTransmitterId;
    private double simulationPeriod;
    private Random randomGenerator; // Add a random generator

    public AdaptiveAllocatorModule(String name, String appId, int v2vTransmitterId, double period) {
        super(name);
        this.appId = appId;
        this.pythonClient = new PythonClient();
        this.v2vTransmitterId = v2vTransmitterId;
        this.simulationPeriod = period;
        this.randomGenerator = new Random(); // Initialize the random generator
    }

    @Override
    public void startEntity() {
        System.out.println("AdaptiveAllocatorModule started.");
    }

    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == 2000) {
            String contextLabel = (String) ev.getData();
            System.out.println("\n----------------------------------------------------------");
            System.out.println("adaptive-allocator-module received context event: " + contextLabel);

            // Generate feature values
            double trafficDensity = getTrafficDensityForContext(contextLabel);
            int timeOfDay = getTimeOfDayForContext(contextLabel);
            int weather = getWeatherForContext(contextLabel);
            double visibility = getVisibilityForContext(contextLabel);
            double drowsiness = generateRandomDrowsiness(); // Generate random drowsiness

            // <<< MODIFIED: Print ALL simulated sensor values for this cycle >>>
            System.out.println("SIMULATED SENSORS:");
            System.out.printf("  - Traffic Density: %.1f\n", trafficDensity);
            System.out.printf("  - Time of Day    : %d (1=Day, 0=Night)\n", timeOfDay);
            System.out.printf("  - Weather        : %d (0=Clear, 1=Rainy, 2=Foggy)\n", weather);
            System.out.printf("  - Visibility     : %.1f\n", visibility);
            System.out.printf("  - Drowsiness     : %.1f\n", drowsiness);
            // <<< END OF MODIFICATION >>>

            try {
                // Get AI priority prediction
                System.out.println("AI BRAIN: Querying AI server for priority...");
                String priorityLabel = pythonClient.predictPriority(trafficDensity, timeOfDay, weather, visibility);
                System.out.println("AI BRAIN: Received Overall Priority = " + priorityLabel);

                // Get Fuzzy allocation
                System.out.println("AI BRAIN: Querying Fuzzy server for resource allocation...");
                String allocationDecisionJson = pythonClient.allocateResources(priorityLabel);
                System.out.println("AI BRAIN: Received Action = " + allocationDecisionJson);

                // --- Alert Logic ---
                boolean sendAlert = false;
                String alertReason = "";
                boolean highDrowsiness = drowsiness > 0.7;
                boolean highTraffic = trafficDensity > 0.7;

                if (highDrowsiness && highTraffic) {
                    sendAlert = true;
                    alertReason = "High Traffic and Drowsiness Detected";
                } else if (highDrowsiness) {
                    sendAlert = true;
                    alertReason = "High Drowsiness Detected - Driver Alert";
                } else if (highTraffic) {
                    sendAlert = true;
                    alertReason = "High Traffic Detected - Proceed with Caution";
                } else if ("HIGH".equalsIgnoreCase(priorityLabel)) {
                    sendAlert = true;
                    alertReason = "High risk scenario detected by AI";
                }

                // Assign power modes based on conditions AND AI prediction
                logEnergyConsumption(priorityLabel, drowsiness, highTraffic);

                // If any condition was met, send the specific alert
                if (sendAlert) {
                    System.out.println("ALERT TRIGGER: " + alertReason + "! Activating critical systems.");
                    send(v2vTransmitterId, 0.0, 3000, "V2V_ALERT: " + alertReason);
                }

            } catch (Exception e) {
                System.err.println("Could not connect to a Python CI server. Error: " + e.getMessage());
            }
        }
    }

    /**
     * Assigns power modes based on the AI's priority, overrides Proximity Sensor
     * based on traffic, and sets Drowsiness Sensor mode based on its specific value.
     */
    private void logEnergyConsumption(String overallPriority, double drowsinessValue, boolean isHighTraffic) {
        Map<String, String> sensorRoles = new HashMap<>();
        sensorRoles.put("Proximity Sensor", "CRITICAL");
        sensorRoles.put("Driver Drowsiness Sensor", "CRITICAL");
        sensorRoles.put("Environmental Sensor", "IMPORTANT");
        sensorRoles.put("Vehicle Communication Module", "IMPORTANT");

        Map<String, String> finalModes = new HashMap<>();
        Map<String, Double> finalPowerMw = new HashMap<>();

        // 1. Determine BASE power modes for all sensors EXCEPT Drowsiness Sensor
        for (Map.Entry<String, String> entry : sensorRoles.entrySet()) {
            String sensor = entry.getKey();
            if (sensor.equals("Driver Drowsiness Sensor")) {
                continue; // Skip drowsiness sensor here, handle it separately
            }

            String role = entry.getValue();
            String baseMode;
            double basePowerMw;

            switch (overallPriority) {
                case "HIGH":
                    if (role.equals("CRITICAL")) { baseMode = "FULL POWER"; basePowerMw = PowerModel.FULL_POWER_MW; }
                    else { baseMode = "REDUCED POWER"; basePowerMw = PowerModel.REDUCED_POWER_MW; }
                    break;
                case "MEDIUM":
                    if (role.equals("CRITICAL")) { baseMode = "REDUCED POWER"; basePowerMw = PowerModel.REDUCED_POWER_MW; }
                    else { baseMode = "STANDBY"; basePowerMw = PowerModel.STANDBY_POWER_MW; }
                    break;
                default: // LOW
                    baseMode = "STANDBY";
                    basePowerMw = PowerModel.STANDBY_POWER_MW;
                    break;
            }
            finalModes.put(sensor, baseMode);
            finalPowerMw.put(sensor, basePowerMw);
        }

        // 2. Apply OVERRIDES / Specific Logic

        // Override Proximity Sensor if traffic is high
        if (isHighTraffic) {
            finalModes.put("Proximity Sensor", "FULL POWER");
            finalPowerMw.put("Proximity Sensor", PowerModel.FULL_POWER_MW);
        }

        // Set Driver Drowsiness Sensor mode directly based on its value
        String drowsinessMode;
        double drowsinessPowerMw;
        if (drowsinessValue > 0.7) {
            drowsinessMode = "FULL POWER";
            drowsinessPowerMw = PowerModel.FULL_POWER_MW;
        } else if (drowsinessValue >= 0.5) { // 0.5, 0.6, 0.7
            drowsinessMode = "REDUCED POWER";
            drowsinessPowerMw = PowerModel.REDUCED_POWER_MW;
        } else { // < 0.5
            drowsinessMode = "STANDBY";
            drowsinessPowerMw = PowerModel.STANDBY_POWER_MW;
        }
        finalModes.put("Driver Drowsiness Sensor", drowsinessMode);
        finalPowerMw.put("Driver Drowsiness Sensor", drowsinessPowerMw);

        // 3. Log the final modes and calculate total energy for this cycle
        StringBuilder report = new StringBuilder("ENERGY MODEL: Assigning final power modes:\n");
        // Use sensorRoles to ensure consistent order in the report
        for (String sensor : sensorRoles.keySet()) {
            String mode = finalModes.get(sensor);
            double powerMw = finalPowerMw.get(sensor);
            PowerModel.addEnergyConsumption(powerMw, this.simulationPeriod);
            report.append(String.format("  - %-30s: %s\n", sensor, mode));
        }
        report.append(String.format("Total Energy: %.2f mJ", PowerModel.getTotalEnergyConsumed()));
        System.out.println(report.toString());
    }

    /**
     * Generates a random drowsiness value between 0.0 and 1.0 (1 decimal place).
     */
    private double generateRandomDrowsiness() {
        // Generate a random double between 0.0 (inclusive) and 1.0 (exclusive)
        double randomValue = randomGenerator.nextDouble();
        // Scale to 0-10, round to nearest integer, then divide by 10.0
        return Math.round(randomValue * 10.0) / 10.0;
    }

    // --- Helper methods for context features ---
    private double getTrafficDensityForContext(String context) {
        if ("CONTEXT_TUPLE_HIGH_CLEAR".equals(context)) return 0.8;
        if ("CONTEXT_TUPLE_LOW_RAINY".equals(context)) return 0.2;
        if ("CONTEXT_TUPLE_LOW_CLEAR".equals(context)) return 0.1;
        return 0.5; // MEDIUM_CLOUDY
    }
    private int getTimeOfDayForContext(String context) {
        if ("CONTEXT_TUPLE_LOW_RAINY".equals(context)) return 0; // Night
        return 1; // Day for all others
    }
    private int getWeatherForContext(String context) {
        if ("CONTEXT_TUPLE_HIGH_CLEAR".equals(context)) return 0; // Clear
        if ("CONTEXT_TUPLE_LOW_RAINY".equals(context)) return 1; // Rainy
        if ("CONTEXT_TUPLE_LOW_CLEAR".equals(context)) return 0; // Clear
        return 2; // MEDIUM_CLOUDY
    }
    private double getVisibilityForContext(String context) {
        if ("CONTEXT_TUPLE_HIGH_CLEAR".equals(context)) return 0.9;
        if ("CONTEXT_TUPLE_LOW_RAINY".equals(context)) return 0.3;
        if ("CONTEXT_TUPLE_LOW_CLEAR".equals(context)) return 1.0; // Perfect visibility
        return 0.6; // MEDIUM_CLOUDY
    }

    @Override
    public void shutdownEntity() {}
}