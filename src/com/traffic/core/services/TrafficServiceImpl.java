package com.traffic.core.services;

import com.traffic.common.interfaces.ITrafficService;
import com.traffic.common.models.VehicleDetails;
import com.traffic.core.bridge.LegacyTrafficLightSystem;
import com.traffic.core.server.JmsAlertProducer;
import com.traffic.data.SupabaseRestRepository;
import com.traffic.data.TelemetryRepository;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TrafficServiceImpl extends UnicastRemoteObject implements ITrafficService {

    private TelemetryRepository telemetryRepo;
    private Map<String, VehicleDetails> realTimeCache;
    private LegacyTrafficLightSystem legacyBridge;
    private JmsAlertProducer jmsProducer;
    private RouteOptimizer router; // Added for Dijkstra Algorithm

    public TrafficServiceImpl() throws RemoteException {
        super();
        // 1. Storage: Using Cloud Supabase (Java 8 Compatible REST)
        this.telemetryRepo = new SupabaseRestRepository();

        // 2. Hardware: Legacy CORBA Bridge
        this.legacyBridge = new LegacyTrafficLightSystem();

        // 3. State: In-Memory Real-time Cache
        this.realTimeCache = new ConcurrentHashMap<>();

        // 4. Alerts: ActiveMQ JMS Producer
        this.jmsProducer = new JmsAlertProducer();

        // 5. Logic: Initialize Route Optimizer (Dijkstra)
        this.router = new RouteOptimizer();
    }

    @Override
    public void updateVehiclePosition(VehicleDetails details) throws RemoteException {
        System.out.println("[RMI SERVER] Ingesting Data: " + details.getVehicleId());

        // Push to Cloud (Async)
        telemetryRepo.saveTelemetry(details);

        // Update Real-Time Cache
        realTimeCache.put(details.getVehicleId(), details);

        // Check for Speeding Violations
        if (details.getSpeed() > 90.0) {
            System.out.println("!!! [VIOLATION] Speeding detected: " + details.getSpeed() + " km/h");
            jmsProducer.sendSpeedingAlert(details.getVehicleId(), details.getSpeed());
        }
    }

    @Override
    public boolean isVehicleSpeeding(String vehicleId) throws RemoteException {
        if (!realTimeCache.containsKey(vehicleId)) return false;
        return realTimeCache.get(vehicleId).getSpeed() > 90.0;
    }

    @Override
    public String triggerTrafficLight(String junctionId, String command) throws RemoteException {
        String legacyCode = command.equalsIgnoreCase("GREEN") ? "0x01" : "0x00";
        legacyBridge.sendSignal(junctionId, legacyCode);
        return "Command " + command + " sent.";
    }

    @Override
    public List<VehicleDetails> getAllVehicles() throws RemoteException {
        return new ArrayList<>(realTimeCache.values());
    }

    @Override
    public int performMaintenanceCleanup() throws RemoteException {
        if (realTimeCache.size() > 100) realTimeCache.clear();
        return realTimeCache.size();
    }

    @Override
    public List<String> getOptimalRoute(String origin, String destination) throws RemoteException {
        System.out.println("[ROUTER] Calculating path from " + origin + " to " + destination);
        // Delegate to our new Dijkstra implementation
        return router.findShortestPath(origin, destination);
    }
}