package com.traffic.core.server;

import com.traffic.common.interfaces.ITrafficService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handles the "Jobs" box in the architecture (Slide 3).
 * Runs tasks in the background without blocking the main RMI Server.
 */
public class BackgroundJobManager {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ITrafficService trafficService;

    public BackgroundJobManager(ITrafficService service) {
        this.trafficService = service;
    }

    public void startJobs() {
        // Run the Archival/Cleanup job every 10 seconds
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println(">>> [JOB] Running Scheduled Archival & Health Check...");
                int vehicleCount = trafficService.performMaintenanceCleanup();
                System.out.println(">>> [JOB] System Health: " + vehicleCount + " active vehicles tracking.");
            } catch (Exception e) {
                System.err.println(">>> [JOB] Error during maintenance: " + e.getMessage());
            }
        }, 5, 10, TimeUnit.SECONDS); // Start after 5s, repeat every 10s
    }

    public void stopJobs() {
        scheduler.shutdown();
    }
}