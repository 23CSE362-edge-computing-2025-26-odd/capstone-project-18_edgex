.package iFogSimulator;

/**
 * PowerModel - NEW HELPER CLASS
 *
 * This class defines the power consumption for different sensor modes
 * and tracks the total energy consumed during the simulation.
 */
public class PowerModel {

    // Power consumption values in milliWatts (mW) for a given time unit
    public static final double FULL_POWER_MW = 5.0;
    public static final double REDUCED_POWER_MW = 2.0;
    public static final double STANDBY_POWER_MW = 0.5;

    // Static variable to accumulate total energy consumption
    private static double totalEnergyConsumed_mJ = 0.0;

    /**
     * Adds energy consumption for a given time duration and power mode.
     * @param powerIn_mW The power consumption in mW (e.g., FULL_POWER_MW)
     * @param timeDuration The duration for which the power is consumed
     */
    public static void addEnergyConsumption(double powerIn_mW, double timeDuration) {
        double energy_mJ = powerIn_mW * timeDuration;
        totalEnergyConsumed_mJ += energy_mJ;
    }

    /**
     * Returns the total accumulated energy consumption.
     * @return Total energy in milliJoules (mJ)
     */
    public static double getTotalEnergyConsumed() {
        return totalEnergyConsumed_mJ;
    }

    /**
     * Resets the energy counter, typically for a new simulation run.
     */
    public static void reset() {
        totalEnergyConsumed_mJ = 0.0;
    }
}