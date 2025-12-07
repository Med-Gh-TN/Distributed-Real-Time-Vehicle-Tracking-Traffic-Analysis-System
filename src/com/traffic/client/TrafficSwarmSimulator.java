package com.traffic.client;

import com.traffic.common.interfaces.ITrafficService;
import com.traffic.common.models.VehicleDetails;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * THE SWARM: Simulates 1,000 autonomous vehicles.
 * STRESS TESTS the RMI Server and Supabase ingestion.
 */
public class TrafficSwarmSimulator {

    // TUNISIA BOUNDING BOX (Roughly)
    private static final double MIN_LAT = 33.00;
    private static final double MAX_LAT = 37.00;
    private static final double MIN_LON = 9.00;
    private static final double MAX_LON = 11.50;

    public static void main(String[] args) {
        try {
            System.out.println(">>> [SWARM] Connecting to Traffic Control...");
            Registry registry = LocateRegistry.getRegistry("localhost", 1090);
            ITrafficService engine = (ITrafficService) registry.lookup("TrafficService");

            int carCount = 1000; // THE LOAD
            ExecutorService pool = Executors.newFixedThreadPool(100); // 100 active threads handling 1000 cars

            System.out.println(">>> [SWARM] Launching " + carCount + " vehicles...");

            for (int i = 0; i < carCount; i++) {
                final int id = i;
                pool.execute(() -> runCar(engine, "BOT-" + id));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runCar(ITrafficService engine, String vehicleId) {
        Random rand = new Random();

        // Pick random start location in Tunisia
        double lat = MIN_LAT + (MAX_LAT - MIN_LAT) * rand.nextDouble();
        double lon = MIN_LON + (MAX_LON - MIN_LON) * rand.nextDouble();

        // Random drift direction
        double latDrift = (rand.nextDouble() - 0.5) * 0.001;
        double lonDrift = (rand.nextDouble() - 0.5) * 0.001;

        try {
            while (true) {
                // Move
                lat += latDrift;
                lon += lonDrift;

                // Random Speed (Mostly normal, occasional burst)
                double speed = 60 + rand.nextInt(40); // Base 60-100
                if (rand.nextInt(100) > 95) speed += 50; // 5% chance of SUPER SPEED (150km/h)

                // Bounce off borders
                if (lat < MIN_LAT || lat > MAX_LAT) latDrift *= -1;
                if (lon < MIN_LON || lon > MAX_LON) lonDrift *= -1;

                // Send Data
                // Note: We skip the Gateway and hit RMI directly for maximum performance
                engine.updateVehiclePosition(new VehicleDetails(vehicleId, lat, lon, speed));

                // Sleep random time (0.5s to 2s) to desynchronize the swarm
                Thread.sleep(500 + rand.nextInt(1500));
            }
        } catch (Exception e) {
            System.err.println("Bot " + vehicleId + " died: " + e.getMessage());
        }
    }
}