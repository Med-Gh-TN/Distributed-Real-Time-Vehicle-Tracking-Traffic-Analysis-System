package com.traffic.client;

import com.traffic.common.interfaces.ITrafficService;
import com.traffic.common.models.VehicleDetails;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A "Smart" Client that asks the server for a route before driving.
 * Demonstrates: RMI (Request/Response) + Dijkstra Logic + Telemetry Ingestion.
 */
public class SmartDriverClient {

    // Mock GPS Database: Maps City Names to Lat/Lon
    private static final Map<String, double[]> GPS_DATA = new HashMap<>();

    static {
        GPS_DATA.put("Tunis", new double[]{36.8065, 10.1815});
        GPS_DATA.put("Ben Arous", new double[]{36.7531, 10.2188});
        GPS_DATA.put("Ariana", new double[]{36.8665, 10.1647});
        GPS_DATA.put("Hammamet", new double[]{36.4000, 10.6167});
        GPS_DATA.put("Nabeul", new double[]{36.4561, 10.7376});
        GPS_DATA.put("Sousse", new double[]{35.8256, 10.6084});
        GPS_DATA.put("Monastir", new double[]{35.7643, 10.8113});
        GPS_DATA.put("Sfax", new double[]{34.7406, 10.7603});
        GPS_DATA.put("Gabes", new double[]{33.8815, 10.0982});
        GPS_DATA.put("Zaghouan", new double[]{36.4029, 10.1429});
        GPS_DATA.put("Kairouan", new double[]{35.6781, 10.0963});
    }

    public static void main(String[] args) {
        try {
            // 1. Connect to RMI Server
            Registry registry = LocateRegistry.getRegistry("localhost", 1090);
            ITrafficService engine = (ITrafficService) registry.lookup("TrafficService");

            // 2. Define Mission
            String vehicleId = "TUN-SMART-01";
            String start = "Tunis";
            String destination = "Sfax";

            System.out.println(">>> [CLIENT] Asking Server for optimal route: " + start + " -> " + destination);

            // 3. Get Route from Dijkstra Algorithm (The "Brain")
            List<String> route = engine.getOptimalRoute(start, destination);
            System.out.println(">>> [CLIENT] Route Received: " + route);

            // 4. Simulate Driving the Route
            driveRoute(engine, vehicleId, route);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void driveRoute(ITrafficService engine, String vehicleId, List<String> route) throws Exception {
        for (int i = 0; i < route.size() - 1; i++) {
            String currentCity = route.get(i);
            String nextCity = route.get(i + 1);

            double[] startCoords = GPS_DATA.get(currentCity);
            double[] endCoords = GPS_DATA.get(nextCity);

            if (startCoords == null || endCoords == null) {
                System.err.println("!!! GPS Error: Unknown coordinates for " + currentCity + " or " + nextCity);
                continue;
            }

            System.out.println("--- 🚗 Driving segment: " + currentCity + " -> " + nextCity + " ---");

            // Interpolate movement between cities (10 steps per city link)
            int steps = 10;
            double latStep = (endCoords[0] - startCoords[0]) / steps;
            double lonStep = (endCoords[1] - startCoords[1]) / steps;

            double currentLat = startCoords[0];
            double currentLon = startCoords[1];

            for (int step = 0; step <= steps; step++) {
                // Update position
                currentLat += latStep;
                currentLon += lonStep;

                // Simulate random speed (some speeding!)
                double speed = 80 + (Math.random() * 30); // 80 - 110 km/h

                VehicleDetails telemetry = new VehicleDetails(vehicleId, currentLat, currentLon, speed);

                // Send to Server
                engine.updateVehiclePosition(telemetry);
                System.out.printf("   >> Position: %.4f, %.4f | Speed: %.1f km/h%n", currentLat, currentLon, speed);

                // Wait 1 second
                Thread.sleep(1000);
            }
        }
        System.out.println(">>> [MISSION COMPLETE] Arrived at Destination.");
    }
}