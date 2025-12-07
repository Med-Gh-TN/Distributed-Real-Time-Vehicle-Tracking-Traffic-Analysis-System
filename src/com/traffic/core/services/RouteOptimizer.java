package com.traffic.core.services;

import java.util.*;

/**
 * Implements Dijkstra's Algorithm to find the optimal route.
 * Weights represent "Traffic Density" (Higher = Slower).
 */
public class RouteOptimizer {

    // Graph: City -> (Neighbor -> Cost)
    private final Map<String, Map<String, Integer>> graph = new HashMap<>();

    public RouteOptimizer() {
        // Initialize the Map of Tunisia (Nodes and Edges)
        buildGraph();
    }

    private void buildGraph() {
        // 1. Define Cities and Connections (Mocking a real map)
        addRoute("Tunis", "Ben Arous", 10);
        addRoute("Tunis", "Ariana", 5);
        addRoute("Ben Arous", "Hammamet", 40);
        addRoute("Hammamet", "Sousse", 30);
        addRoute("Hammamet", "Nabeul", 15);
        addRoute("Sousse", "Sfax", 90);
        addRoute("Sousse", "Monastir", 20);
        addRoute("Sfax", "Gabes", 100);

        // Alternative inland route
        addRoute("Tunis", "Zaghouan", 35);
        addRoute("Zaghouan", "Kairouan", 50);
        addRoute("Kairouan", "Sfax", 70);
    }

    private void addRoute(String from, String to, int cost) {
        graph.computeIfAbsent(from, k -> new HashMap<>()).put(to, cost);
        graph.computeIfAbsent(to, k -> new HashMap<>()).put(from, cost); // Bidirectional
    }

    public List<String> findShortestPath(String start, String end) {
        if (!graph.containsKey(start) || !graph.containsKey(end)) {
            return Collections.singletonList("Error: Unknown City");
        }

        // Dijkstra's Algorithm Structures
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(node -> node.cost));
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        Set<String> visited = new HashSet<>();

        // Init
        for (String city : graph.keySet()) {
            distances.put(city, Integer.MAX_VALUE);
        }
        distances.put(start, 0);
        pq.add(new Node(start, 0));

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            String currentCity = current.name;

            if (visited.contains(currentCity)) continue;
            visited.add(currentCity);

            if (currentCity.equals(end)) break; // Found destination

            // Check neighbors
            Map<String, Integer> neighbors = graph.getOrDefault(currentCity, new HashMap<>());
            for (Map.Entry<String, Integer> neighbor : neighbors.entrySet()) {
                String neighborName = neighbor.getKey();
                int newDist = distances.get(currentCity) + neighbor.getValue();

                if (newDist < distances.get(neighborName)) {
                    distances.put(neighborName, newDist);
                    previous.put(neighborName, currentCity);
                    pq.add(new Node(neighborName, newDist));
                }
            }
        }

        // Reconstruct Path
        List<String> path = new LinkedList<>();
        for (String at = end; at != null; at = previous.get(at)) {
            path.add(0, at);
        }

        if (path.isEmpty() || !path.get(0).equals(start)) {
            return Collections.singletonList("No Route Found");
        }
        return path;
    }

    // Helper Class for PriorityQueue
    private static class Node {
        String name;
        int cost;
        Node(String name, int cost) { this.name = name; this.cost = cost; }
    }
}