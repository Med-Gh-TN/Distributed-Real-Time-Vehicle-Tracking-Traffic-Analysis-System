package com.traffic.data;

import com.traffic.common.models.VehicleDetails;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SupabaseRestRepository implements TelemetryRepository {

    private static final String SUPABASE_URL = "https://rgoxuvgazwglosogvcpq.supabase.co";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJnb3h1dmdhendnbG9zb2d2Y3BxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjUxMjYxODMsImV4cCI6MjA4MDcwMjE4M30.Dh5oESESjlFnTfyEJ2knjxClXjkg0n4FXYimMoIzYB0";

    public SupabaseRestRepository() {
        System.out.println(">>> [SUPABASE] REST Connector Ready (Java 8 Mode).");
    }

    @Override
    public void saveTelemetry(VehicleDetails data) {
        new Thread(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/vehicle_telemetry");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", API_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Prefer", "return=minimal");
                conn.setDoOutput(true);

                String jsonBody = String.format(
                        "{\"vehicle_id\": \"%s\", \"speed\": %.2f, \"latitude\": %.4f, \"longitude\": %.4f}",
                        data.getVehicleId(), data.getSpeed(), data.getLatitude(), data.getLongitude()
                );

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                conn.getResponseCode(); // Trigger request
                conn.disconnect();
            } catch (Exception e) {
                System.err.println("!!! [CLOUD ERROR] " + e.getMessage());
            }
        }).start();
    }

    @Override
    public List<VehicleDetails> getHistory(String vehicleId) { return new ArrayList<>(); }
}