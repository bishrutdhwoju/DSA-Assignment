package assignment;

import java.util.*;

// Edmonds-Karp algorithm to find maximum flow in a directed network using BFS
public class EdmondsKarpMaxFlow {

    public static void main(String[] args) {
        // 5 nodes: KTM=0, JA=1, JB=2, PH=3, BS=4
        MaxFlowNetwork network = new MaxFlowNetwork(5);

        // Add edges with capacities between cities
        network.addEdge(0, 1, 10); // KTM -> JA
        network.addEdge(0, 2, 15); // KTM -> JB
        network.addEdge(1, 0, 10); // JA -> KTM
        network.addEdge(1, 3, 8);  // JA -> PH
        network.addEdge(1, 4, 5);  // JA -> BS
        network.addEdge(2, 0, 15); // JB -> KTM
        network.addEdge(2, 1, 4);  // JB -> JA
        network.addEdge(2, 4, 12); // JB -> BS
        network.addEdge(3, 1, 8);  // PH -> JA
        network.addEdge(3, 4, 6);  // PH -> BS
        network.addEdge(4, 1, 5);  // BS -> JA
        network.addEdge(4, 2, 12); // BS -> JB
        network.addEdge(4, 3, 6);  // BS -> PH

        // Max flow from KTM (0) to BS (4) -> expected: 23
        System.out.println(network.computeMaxFlow(0, 4));
    }

    // Represents a directed edge with destination, reverse edge index, and remaining capacity
    static class FlowEdge {
        int destination;
        int reverseIndex;
        int remainingCapacity;

        FlowEdge(int destination, int reverseIndex, int remainingCapacity) {
            this.destination = destination;
            this.reverseIndex = reverseIndex;
            this.remainingCapacity = remainingCapacity;
        }
    }

    // Flow network using adjacency list representation
    static class MaxFlowNetwork {
        List<FlowEdge>[] adjacencyList;

        MaxFlowNetwork(int nodeCount) {
            adjacencyList = new ArrayList[nodeCount];
            for (int i = 0; i < nodeCount; i++) adjacencyList[i] = new ArrayList<>();
        }

        // Adds a forward edge and a reverse edge (for residual graph)
        void addEdge(int source, int target, int capacity) {
            FlowEdge forwardEdge = new FlowEdge(target, adjacencyList[target].size(), capacity);
            FlowEdge reverseEdge = new FlowEdge(source, adjacencyList[source].size(), 0);
            adjacencyList[source].add(forwardEdge);
            adjacencyList[target].add(reverseEdge);
        }

        // Computes max flow using BFS to find augmenting paths
        int computeMaxFlow(int source, int sink) {
            int totalFlow = 0;
            int nodeCount = adjacencyList.length;

            while (true) {
                int[] parentNode = new int[nodeCount];      // tracks BFS parent of each node
                int[] parentEdgeIndex = new int[nodeCount];  // tracks which edge led to each node
                Arrays.fill(parentNode, -1);

                // BFS from source to sink
                Queue<Integer> bfsQueue = new ArrayDeque<>();
                bfsQueue.add(source);
                parentNode[source] = source;

                while (!bfsQueue.isEmpty() && parentNode[sink] == -1) {
                    int currentNode = bfsQueue.poll();
                    for (int edgeIdx = 0; edgeIdx < adjacencyList[currentNode].size(); edgeIdx++) {
                        FlowEdge edge = adjacencyList[currentNode].get(edgeIdx);
                        if (parentNode[edge.destination] == -1 && edge.remainingCapacity > 0) {
                            parentNode[edge.destination] = currentNode;
                            parentEdgeIndex[edge.destination] = edgeIdx;
                            bfsQueue.add(edge.destination);
                        }
                    }
                }

                // No more augmenting paths found
                if (parentNode[sink] == -1) break;

                // Find bottleneck flow along the path
                int bottleneckFlow = Integer.MAX_VALUE;
                for (int node = sink; node != source; node = parentNode[node]) {
                    FlowEdge edge = adjacencyList[parentNode[node]].get(parentEdgeIndex[node]);
                    bottleneckFlow = Math.min(bottleneckFlow, edge.remainingCapacity);
                }

                // Update residual capacities along the path
                for (int node = sink; node != source; node = parentNode[node]) {
                    FlowEdge edge = adjacencyList[parentNode[node]].get(parentEdgeIndex[node]);
                    edge.remainingCapacity -= bottleneckFlow;
                    adjacencyList[node].get(edge.reverseIndex).remainingCapacity += bottleneckFlow;
                }

                totalFlow += bottleneckFlow;
            }

            return totalFlow;
        }
    }
}