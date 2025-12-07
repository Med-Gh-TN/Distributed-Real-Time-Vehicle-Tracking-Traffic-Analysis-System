package com.traffic.core.server;

import com.traffic.common.interfaces.ITrafficService;
import com.traffic.core.services.TrafficServiceImpl;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RmiServer {
    public static void main(String[] args) {
        try {
            System.out.println(">>> Starting Traffic Control System...");

            // 1. Start JMS (Safely)
            new Thread(() -> {
                try {
                    System.out.println(">>> [INIT] Connecting to Police JMS...");
                    JmsAlertListener policeStation = new JmsAlertListener();
                    policeStation.startListening();
                } catch (Exception e) {
                    // SILENT CATCH: Don't crash or spam logs if ActiveMQ is missing
                    System.out.println("!!! [WARN] ActiveMQ not found. Police Alerts disabled.");
                }
            }).start();

            // 2. Start RMI Services
            ITrafficService service = new TrafficServiceImpl();
            Registry registry = LocateRegistry.createRegistry(1090);
            registry.rebind("TrafficService", service);

            // 3. Start Jobs
            BackgroundJobManager jobManager = new BackgroundJobManager(service);
            jobManager.startJobs();

            System.out.println(">>> SERVER READY. Waiting for vehicles...");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}