package com.traffic.common.interfaces;

import com.traffic.common.models.VehicleDetails;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ITrafficService extends Remote {

    // --- REAL-TIME INGESTION ---
    void updateVehiclePosition(VehicleDetails details) throws RemoteException;

    // --- ANALYTICS ---
    boolean isVehicleSpeeding(String vehicleId) throws RemoteException;

    // --- LEGACY BRIDGE ---
    String triggerTrafficLight(String junctionId, String command) throws RemoteException;

    // --- DASHBOARD ---
    List<VehicleDetails> getAllVehicles() throws RemoteException;

    // --- MAINTENANCE JOBS ---
    int performMaintenanceCleanup() throws RemoteException;

    // --- ROUTE OPTIMIZATION ---
    List<String> getOptimalRoute(String origin, String destination) throws RemoteException;
}