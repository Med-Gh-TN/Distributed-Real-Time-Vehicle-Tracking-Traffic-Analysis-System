package com.traffic.client;

import com.traffic.common.interfaces.ITrafficService;
import com.traffic.common.models.VehicleDetails;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestClient {
    public static void main(String[] args) {
        try {
            // 1. Connect to Backend
            Registry registry = LocateRegistry.getRegistry("localhost", 1090);
            ITrafficService engine = (ITrafficService) registry.lookup("TrafficService");
            System.out.println("--- 🚗 Simulation Started: Driving down Avenue Habib Bourguiba ---");

            // 2. Simulation Loop
            // Start Coordinates (Tunis Center)
            double lat = 36.8000;
            double lon = 10.1800;
            String vehicleId = "TUN-2025-X";

            for (int i = 0; i < 20; i++) {
                // Simulate movement (roughly moving North-East)
                lat += 0.0005;
                lon += 0.0005;

                // Simulate varying speed
                double speed = 60 + (Math.random() * 50); // Speed between 60 and 110

                VehicleDetails car = new VehicleDetails(vehicleId, lat, lon, speed);

                // Send to Server
                engine.updateVehiclePosition(car);

                System.out.printf(">> Sent: %s | Speed: %.1f km/h%n", vehicleId, speed);

                if (speed > 90) {
                    System.out.println("   [!] Speeding violation generated!");
                }

                // Wait 1 second before next update
                Thread.sleep(1000);
            }

            System.out.println("--- End of Simulation ---");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}