package com.traffic.gateway;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.traffic.common.interfaces.ITrafficService;
import com.traffic.common.models.VehicleDetails;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.Executors; // Import for Multi-threading

public class RestApiGateway {

    private static ITrafficService rmiService;

    public static void main(String[] args) throws IOException {
        try {
            // 1. Connect to RMI Backend
            Registry registry = LocateRegistry.getRegistry("localhost", 1090);
            rmiService = (ITrafficService) registry.lookup("TrafficService");
            System.out.println(">>> [GATEWAY] Connected to RMI Backend.");
        } catch (Exception e) {
            System.err.println("!!! [FATAL] Could not connect to RMI Server. Is RmiServer running?");
            return;
        }

        // 2. Start HTTP Server
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // 3. Register Endpoints
        server.createContext("/status", new StatusHandler());
        server.createContext("/api/report", new TelemetryHandler());
        server.createContext("/api/dashboard", new DashboardHandler());
        server.createContext("/api/route", new RouteHandler());
        server.createContext("/api/control", new ControlHandler()); // Traffic Light Control

        // 4. ENABLE MULTI-THREADING (Crucial for 1000+ Cars)
        // Uses a cached thread pool to handle concurrent requests automatically
        server.setExecutor(Executors.newCachedThreadPool());

        server.start();
        System.out.println(">>> [GATEWAY] HTTP REST API started on port 8080 (Multi-Threaded)");
    }

    // --- HELPER: CORS (Allows Browser Access) ---
    private static boolean handleCors(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type,Authorization");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    // --- HANDLERS ---

    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            String response = "{\"system\": \"online\"}";
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        }
    }

    static class TelemetryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            if ("POST".equals(exchange.getRequestMethod())) {
                String response = "{}";
                int code = 200;
                try {
                    String body = new Scanner(exchange.getRequestBody()).useDelimiter("\\A").next();
                    Map<String, Object> data = JsonUtils.parseJson(body);

                    String vId = String.valueOf(data.get("vehicleId"));
                    double lat = Double.parseDouble(String.valueOf(data.get("lat")));
                    double lon = Double.parseDouble(String.valueOf(data.get("lon")));
                    double speed = Double.parseDouble(String.valueOf(data.get("speed")));

                    rmiService.updateVehiclePosition(new VehicleDetails(vId, lat, lon, speed));
                    response = "{\"status\": \"ok\"}";
                } catch (Exception e) {
                    code = 400;
                    response = "{\"error\": \"Invalid Data\"}";
                }
                exchange.sendResponseHeaders(code, response.length());
                exchange.getResponseBody().write(response.getBytes());
            }
            exchange.close();
        }
    }

    static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    List<VehicleDetails> vehicles = rmiService.getAllVehicles();
                    String json = JsonUtils.toJson(vehicles);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, json.length());
                    exchange.getResponseBody().write(json.getBytes());
                } catch (Exception e) { exchange.sendResponseHeaders(500, -1); }
            }
            exchange.close();
        }
    }

    static class RouteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            String from = "Tunis";
            String to = "Sousse";

            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("to=")) {
                for(String p : query.split("&")) {
                    if(p.startsWith("to=")) to = p.split("=")[1];
                    if(p.startsWith("from=")) from = p.split("=")[1];
                }
            }

            try {
                List<String> route = rmiService.getOptimalRoute(from, to);
                String json = JsonUtils.toJson(route);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, json.length());
                exchange.getResponseBody().write(json.getBytes());
            } catch (Exception e) { exchange.sendResponseHeaders(500, -1); }
            exchange.close();
        }
    }

    // --- CONTROL HANDLER (For Traffic Lights) ---
    static class ControlHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;

            if ("POST".equals(exchange.getRequestMethod())) {
                String response = "{}";
                int code = 200;
                try {
                    String body = new Scanner(exchange.getRequestBody()).useDelimiter("\\A").next();
                    Map<String, Object> data = JsonUtils.parseJson(body);

                    String junctionId = String.valueOf(data.get("junctionId"));
                    String command = String.valueOf(data.get("command")); // "RED" or "GREEN"

                    System.out.println(">>> [GATEWAY] Sending Signal " + command + " to " + junctionId);

                    // Call RMI -> which calls CORBA
                    String result = rmiService.triggerTrafficLight(junctionId, command);

                    response = "{\"status\": \"executed\", \"hardware_response\": \"" + result + "\"}";
                } catch (Exception e) {
                    code = 500;
                    response = "{\"error\": \"" + e.getMessage() + "\"}";
                }
                exchange.sendResponseHeaders(code, response.length());
                exchange.getResponseBody().write(response.getBytes());
            }
            exchange.close();
        }
    }
}