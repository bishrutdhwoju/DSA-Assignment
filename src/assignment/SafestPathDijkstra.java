package assignment;

import java.util.*;

// Finds the safest path between cities using modified Dijkstra's algorithm
// Uses -log(probability) transformation to convert product-maximization to sum-minimization
public class SafestPathDijkstra {

    // Computes safety probability from source city to every other city
    public static Map<String, Double> safestFrom(String sourceCity, Map<String, List<CityEdge>> graph) {
        // distance = -log(probability), so minimizing sum = maximizing product
        Map<String, Double> transformedDistance = new HashMap<>();
        for (String city : graph.keySet()) transformedDistance.put(city, Double.POSITIVE_INFINITY);
        transformedDistance.put(sourceCity, 0.0);

        PriorityQueue<String> priorityQueue = new PriorityQueue<>(Comparator.comparingDouble(transformedDistance::get));
        priorityQueue.add(sourceCity);

        while (!priorityQueue.isEmpty()) {
            String currentCity = priorityQueue.poll();
            double currentDistance = transformedDistance.get(currentCity);

            for (CityEdge edge : graph.getOrDefault(currentCity, List.of())) {
                double edgeWeight = -Math.log(edge.safetyProbability);
                double candidateDistance = currentDistance + edgeWeight;

                // Relax edge if shorter path found
                if (candidateDistance < transformedDistance.getOrDefault(edge.destinationCity, Double.POSITIVE_INFINITY)) {
                    transformedDistance.put(edge.destinationCity, candidateDistance);
                    priorityQueue.add(edge.destinationCity);
                }
            }
        }

        // Convert back: probability = exp(-distance)
        Map<String, Double> safetyProbabilities = new HashMap<>();
        for (Map.Entry<String, Double> entry : transformedDistance.entrySet()) {
            double distance = entry.getValue();
            safetyProbabilities.put(entry.getKey(), Double.isInfinite(distance) ? 0.0 : Math.exp(-distance));
        }
        return safetyProbabilities;
    }

    public static void main(String[] args) {
        // Build sample road network with safety probabilities
        Map<String, List<CityEdge>> graph = new HashMap<>();
        addEdge(graph, "KTM", "JA", 0.90);
        addEdge(graph, "KTM", "JB", 0.80);
        addEdge(graph, "JA", "KTM", 0.90);
        addEdge(graph, "JA", "PH", 0.95);
        addEdge(graph, "JA", "BS", 0.70);
        addEdge(graph, "JB", "KTM", 0.80);
        addEdge(graph, "JB", "JA", 0.60);
        addEdge(graph, "JB", "BS", 0.90);
        addEdge(graph, "PH", "JA", 0.95);
        addEdge(graph, "PH", "BS", 0.85);
        addEdge(graph, "BS", "JA", 0.70);
        addEdge(graph, "BS", "JB", 0.90);
        addEdge(graph, "BS", "PH", 0.85);

        System.out.println(safestFrom("KTM", graph));
    }

    // Adds a directed edge and ensures destination exists in graph
    private static void addEdge(Map<String, List<CityEdge>> graph, String fromCity, String toCity, double safetyProbability) {
        graph.computeIfAbsent(fromCity, key -> new ArrayList<>()).add(new CityEdge(toCity, safetyProbability));
        graph.putIfAbsent(toCity, new ArrayList<>());
    }

    // Edge with destination city and safety probability (0.0 to 1.0)
    static class CityEdge {
        String destinationCity;
        double safetyProbability;

        CityEdge(String destinationCity, double safetyProbability) {
            this.destinationCity = destinationCity;
            this.safetyProbability = safetyProbability;
        }
    }
}