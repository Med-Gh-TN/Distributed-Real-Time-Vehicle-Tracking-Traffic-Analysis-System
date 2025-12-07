package com.traffic.common.models;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a snapshot of a vehicle's status.
 * Must implement Serializable to be sent via RMI.
 */
public class VehicleDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private String vehicleId;
    private double latitude;
    private double longitude;
    private double speed;
    private Date timestamp;

    public VehicleDetails(String vehicleId, double lat, double lon, double speed) {
        this.vehicleId = vehicleId;
        this.latitude = lat;
        this.longitude = lon;
        this.speed = speed;
        this.timestamp = new Date();
    }

    @Override
    public String toString() {
        return "Vehicle[" + vehicleId + "] @ " + latitude + "," + longitude + " Speed: " + speed + "km/h";
    }

    // --- GETTERS (Required for Database/Supabase Logic) ---
    public String getVehicleId() { return vehicleId; }

    public double getSpeed() { return speed; }

    public double getLatitude() { return latitude; }

    public double getLongitude() { return longitude; }

    public Date getTimestamp() { return timestamp; }
}