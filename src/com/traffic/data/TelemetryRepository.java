package com.traffic.data;

import com.traffic.common.models.VehicleDetails;
import java.util.List;

public interface TelemetryRepository {
    // "Write" is the most important operation for Telemetry
    void saveTelemetry(VehicleDetails data);

    // Simple retrieval for analytics
    List<VehicleDetails> getHistory(String vehicleId);
}