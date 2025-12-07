package com.traffic.data;

import com.traffic.common.models.VehicleDetails;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * SUPABASE IMPLEMENTATION
 * Connects to the Cloud PostgreSQL instance via JDBC over SSL.
 */
public class SupabaseTelemetryRepository implements TelemetryRepository {

    // --- CONFIGURATION (REPLACE THESE VALUES) ---
    // Found in Supabase -> Settings -> Database -> Connection Info -> Host
    private static final String DB_HOST = "aws-0-eu-central-1.pooler.supabase.com";

    // The database name is almost always "postgres" in Supabase
    private static final String DB_NAME = "postgres";

    // Your Database Password (NOT your Supabase account password)
    private static final String DB_USER = "postgres.your_project_ref";
    private static final String DB_PASS = "Your_Strong_Database_Password";
    // --------------------------------------------

    private String connectionUrl;

    public SupabaseTelemetryRepository() {
        // Construct the JDBC URL with SSL enabled (Required for Supabase)
        this.connectionUrl = String.format("jdbc:postgresql://%s:5432/%s?sslmode=require", DB_HOST, DB_NAME);

        try {
            Class.forName("org.postgresql.Driver");
            System.out.println(">>> [SUPABASE] PostgreSQL Driver Loaded.");
        } catch (ClassNotFoundException e) {
            System.err.println("!!! [FATAL] PostgreSQL Driver missing. Did you add the Maven dependency?");
        }
    }

    @Override
    public void saveTelemetry(VehicleDetails data) {
        String sql = "INSERT INTO vehicle_telemetry (vehicle_id, latitude, longitude, speed) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(connectionUrl, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, data.getVehicleId());
            // Ensure you updated VehicleDetails.java to include these getters (See Step 4 of previous response)
            pstmt.setDouble(2, data.getLatitude());
            pstmt.setDouble(3, data.getLongitude());
            pstmt.setDouble(4, data.getSpeed());

            pstmt.executeUpdate();
            System.out.println(">>> [SUPABASE] Saved telemetry for " + data.getVehicleId());

        } catch (SQLException e) {
            System.err.println("!!! [SUPABASE ERROR] Write Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<VehicleDetails> getHistory(String vehicleId) {
        List<VehicleDetails> history = new ArrayList<>();
        String sql = "SELECT * FROM vehicle_telemetry WHERE vehicle_id = ? ORDER BY recorded_at DESC LIMIT 50";

        try (Connection conn = DriverManager.getConnection(connectionUrl, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, vehicleId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                VehicleDetails v = new VehicleDetails(
                        rs.getString("vehicle_id"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude"),
                        rs.getDouble("speed")
                );
                history.add(v);
            }
        } catch (SQLException e) {
            System.err.println("!!! [SUPABASE ERROR] Read Failed: " + e.getMessage());
        }
        return history;
    }
}