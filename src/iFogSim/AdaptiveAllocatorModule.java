package iFogSim;

import java.util.HashMap;
import org.apache.commons.math3.util.Pair;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.scheduler.TupleScheduler;
import org.fog.application.AppModule;
import org.fog.entities.Tuple;
import org.fog.utils.FogUtils;

public class AdaptiveAllocatorModule extends AppModule {

    public AdaptiveAllocatorModule(String name, String appId, int userId) {
        super(
                FogUtils.generateEntityId(),
                name,
                appId,
                userId,
                200.0,
                512,
                1000L,
                1000L,
                "VMM",
                new TupleScheduler(200.0, 1),
                new HashMap<Pair<String,String>, SelectivityModel>()
        );
    }

    // This method is now correctly called by the modified FogDevice.java
    public void onTupleArrival(Tuple tuple) {
        String receivedType = tuple.getTupleType();

        // --- CHANGED FROM Log.printLine to System.out.println ---
        System.out.println("----------------------------------------------------------");
        System.out.println(getName() + " received context event: " + receivedType);

        double priorityScore = calculatePriority(receivedType);
        System.out.println("AI BRAIN: Calculated Priority Score = " + String.format("%.2f", priorityScore));

        if (priorityScore > 0.6) {
            System.out.println("AI BRAIN: High risk detected! ACTION: Activate critical systems.");
        } else {
            System.out.println("AI BRAIN: Low risk. ACTION: Conserve energy.");
        }
        System.out.println("----------------------------------------------------------");
    }

    private double calculatePriority(String contextType) {
        if (contextType == null || !contextType.startsWith("CONTEXT_TUPLE")) return 0.0;

        // In a real scenario, you'd parse data from the tuple's contents.
        // We simulate it by creating a random tuple type name in FogDevice.java
        boolean isHighTraffic = Math.random() < 0.5; // Simulate random traffic
        boolean isRainyWeather = Math.random() < 0.5; // Simulate random weather

        double trafficScore = isHighTraffic ? 0.9 : 0.1;
        double weatherScore = isRainyWeather ? 0.8 : 0.2;

        double trafficWeight = 0.7;
        double weatherWeight = 0.3;

        return (trafficScore * trafficWeight) + (weatherScore * weatherWeight);
    }
}