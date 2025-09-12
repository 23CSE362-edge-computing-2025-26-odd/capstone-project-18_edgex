import traci
import time

# --- SUMO Connection ---
# Make sure to start SUMO with the --remote-port option
# Example: sumo-gui -c config.sumocfg --remote-port 8813
sumo_cmd = ["sumo-gui", "-c", "config.sumocfg", "--remote-port", "8813"]
traci.start(sumo_cmd)

# --- Main Simulation Loop ---
step = 0
while step < 50:
    traci.simulationStep() # Advance the SUMO simulation by one step

    # Get data from SUMO for our car
    try:
        speed_mps = traci.vehicle.getSpeed("my_smart_car") # Speed in meters/second
        position = traci.vehicle.getPosition("my_smart_car")

        print(f"Step {step}: Car at position {position[0]:.2f}m, Speed: {speed_mps * 3.6:.2f} km/h")

        # --- AI Decision Logic (Placeholder) ---
        # Here you would feed the data into your trained model
        # For now, we'll just simulate a random decision
        if speed_mps < 10:
            decision = "High Risk - Low Speed Detected"
        else:
            decision = "Low Risk"

        print(f"AI Brain Decision: {decision}")

    except traci.TraCIException:
        # This happens when the car finishes its route and leaves the simulation
        print("Car has left the simulation.")
        break

    step += 1
    time.sleep(0.1) # Slow down the loop to make it observable

traci.close()
print("Simulation finished.")