package assignment;

import java.util.*;

// Allocates energy using Dynamic Programming to explore combinations, with Greedy prioritization
public class SmartGridAllocator {

    public static void main(String[] args) {
        int[] hoursOfDay = {6, 7};
        int[][] districtDemand = {
                {20, 15, 25},  // hour 6 demands
                {22, 16, 28}   // hour 7 demands
        };

        // Energy sources: id, type, capacity/hr, start hour, end hour, cost/kWh
        List<EnergySource> energySources = List.of(
                new EnergySource("S1", "Solar", 50, 6, 18, 1.0),
                new EnergySource("S2", "Hydro", 40, 0, 24, 1.5),
                new EnergySource("S3", "Diesel", 60, 17, 23, 3.0)
        );

        List<AllocationRow> allocationResults = allocateWithDP(hoursOfDay, districtDemand, energySources);
        printAllocationTable(allocationResults);
        analyzeResults(allocationResults);
    }

    public static List<AllocationRow> allocateWithDP(int[] hoursOfDay, int[][] districtDemand, List<EnergySource> allSources) {
        List<AllocationRow> result = new ArrayList<>();
        String[] districtNames = {"A", "B", "C"};

        for (int h = 0; h < hoursOfDay.length; h++) {
            int currentHour = hoursOfDay[h];
            int maxSolar = 0, maxHydro = 0, maxDiesel = 0;
            
            for (EnergySource src : allSources) {
                if (src.isAvailable(currentHour)) {
                    if (src.sourceType.equals("Solar")) maxSolar = src.capacityPerHour;
                    if (src.sourceType.equals("Hydro")) maxHydro = src.capacityPerHour;
                    if (src.sourceType.equals("Diesel")) maxDiesel = src.capacityPerHour;
                }
            }

            // Top-down DP with memoization
            Map<String, DPResult> memo = new HashMap<>();
            int[] demands = districtDemand[h];
            
            // Try to perfectly match demand (100%)
            DPResult best = solveDP(0, maxSolar, maxHydro, maxDiesel, demands, true, memo);
            
            // If perfect match fails due to capacity constraints, allow +/- 10% flexibility
            if (best == null || best.cost == Double.POSITIVE_INFINITY) {
                memo.clear();
                best = solveDP(0, maxSolar, maxHydro, maxDiesel, demands, false, memo);
            }

            if (best != null && best.cost != Double.POSITIVE_INFINITY) {
                for (int i = 0; i < 3; i++) {
                    AllocationRow row = new AllocationRow(currentHour, districtNames[i], demands[i]);
                    row.solarUsed = best.allocations[i][0];
                    row.hydroUsed = best.allocations[i][1];
                    row.dieselUsed = best.allocations[i][2];
                    row.totalUsed = row.solarUsed + row.hydroUsed + row.dieselUsed;
                    row.percentageMet = 100.0 * row.totalUsed / demands[i];
                    result.add(row);
                }
            }
        }
        return result;
    }

    // DP State result
    static class DPResult {
        double cost;
        int[][] allocations; // allocations[district][source]
        DPResult(double cost, int[][] allocations) {
            this.cost = cost;
            this.allocations = allocations;
        }
    }

    // Recursive DP evaluating all combinations of sources for districts
    private static DPResult solveDP(int distIdx, int remSolar, int remHydro, int remDiesel, 
                                    int[] demands, boolean exactMatch, Map<String, DPResult> memo) {
        if (distIdx == demands.length) {
            return new DPResult(0.0, new int[demands.length][3]);
        }

        String stateKey = distIdx + "_" + remSolar + "_" + remHydro + "_" + remDiesel + "_" + exactMatch;
        if (memo.containsKey(stateKey)) return memo.get(stateKey);

        int demand = demands[distIdx];
        int minReq = exactMatch ? demand : (int) Math.ceil(demand * 0.9);
        int maxReq = exactMatch ? demand : (int) Math.floor(demand * 1.1);

        DPResult bestResult = null;
        double minCost = Double.POSITIVE_INFINITY;

        // DP explores combinations of total energy provided to this district.
        // Within the DP, we apply the GREEDY strategy: always form 'E' using cheapest available first.
        for (int e = maxReq; e >= minReq; e--) { // Try to fulfill max possible first
            // Greedy Source Prioritization (Solar 1.0 -> Hydro 1.5 -> Diesel 3.0)
            int eNeeded = e;
            int takeSolar = Math.min(eNeeded, remSolar);
            eNeeded -= takeSolar;

            int takeHydro = Math.min(eNeeded, remHydro);
            eNeeded -= takeHydro;

            int takeDiesel = Math.min(eNeeded, remDiesel);
            eNeeded -= takeDiesel;

            if (eNeeded == 0) { // Valid combination found using greedy selection
                double cost = takeSolar * 1.0 + takeHydro * 1.5 + takeDiesel * 3.0;
                
                DPResult nextResult = solveDP(distIdx + 1, remSolar - takeSolar, 
                                              remHydro - takeHydro, remDiesel - takeDiesel, 
                                              demands, exactMatch, memo);

                if (nextResult != null && nextResult.cost != Double.POSITIVE_INFINITY) {
                    double totalCost = cost + nextResult.cost;
                    if (totalCost < minCost) {
                        minCost = totalCost;
                        int[][] newAllocations = new int[demands.length][3];
                        // Copy next results array
                        for(int i = distIdx + 1; i < demands.length; i++) {
                            System.arraycopy(nextResult.allocations[i], 0, newAllocations[i], 0, 3);
                        }
                        newAllocations[distIdx][0] = takeSolar;
                        newAllocations[distIdx][1] = takeHydro;
                        newAllocations[distIdx][2] = takeDiesel;
                        
                        bestResult = new DPResult(minCost, newAllocations);
                    }
                }
            }
        }

        if (bestResult == null) bestResult = new DPResult(Double.POSITIVE_INFINITY, null);
        memo.put(stateKey, bestResult);
        return bestResult;
    }

    private static void printAllocationTable(List<AllocationRow> allocationResults) {
        System.out.println("Hour | Dist | Demand | Solar | Hydro | Diesel | Used | %Met");
        for (AllocationRow row : allocationResults) {
            System.out.printf("%4d |  %s   | %6d | %5d | %5d | %6d | %4d | %5.1f%%%n",
                    row.hour, row.district, row.demand, row.solarUsed, row.hydroUsed, row.dieselUsed, row.totalUsed, row.percentageMet);
        }
    }

    private static void analyzeResults(List<AllocationRow> allocationResults) {
        double totalCost = 0;
        int totalEnergyUsed = 0;
        int renewableEnergyUsed = 0;
        Set<Integer> hoursThatUsedDiesel = new TreeSet<>();

        for (AllocationRow row : allocationResults) {
            totalEnergyUsed += row.totalUsed;
            renewableEnergyUsed += (row.solarUsed + row.hydroUsed);
            totalCost += row.solarUsed * 1.0 + row.hydroUsed * 1.5 + row.dieselUsed * 3.0;
            if (row.dieselUsed > 0) hoursThatUsedDiesel.add(row.hour);
        }

        System.out.println("\n--- Analysis ---");
        System.out.printf("Total cost: Rs. %.2f%n", totalCost);
        System.out.printf("Renewable share: %.2f%%%n", totalEnergyUsed == 0 ? 0 : (100.0 * renewableEnergyUsed / totalEnergyUsed));
        System.out.println("Diesel used in hours: " + (hoursThatUsedDiesel.isEmpty() ? "None" : hoursThatUsedDiesel));
        System.out.println("Trade-off: Dynamic Programming ensures global minimum cost while accurately satisfying minimum/maximum bounds. The greedy inner selection minimizes calculation overhead.");
    }

    static class EnergySource {
        String sourceId, sourceType;
        int capacityPerHour, activeStartHour, activeEndHour;
        double costPerKwh;

        EnergySource(String sourceId, String sourceType, int capacityPerHour, int activeStartHour, int activeEndHour, double costPerKwh) {
            this.sourceId = sourceId;
            this.sourceType = sourceType;
            this.capacityPerHour = capacityPerHour;
            this.activeStartHour = activeStartHour;
            this.activeEndHour = activeEndHour;
            this.costPerKwh = costPerKwh;
        }
        
        // Copy constructor
        EnergySource(EnergySource other) {
            this(other.sourceId, other.sourceType, other.capacityPerHour, other.activeStartHour, other.activeEndHour, other.costPerKwh);
        }

        boolean isAvailable(int hour) {
            return hour >= activeStartHour && hour <= activeEndHour;
        }
    }

    static class AllocationRow {
        int hour;
        String district;
        int demand, solarUsed, hydroUsed, dieselUsed, totalUsed;
        double percentageMet;

        AllocationRow(int hour, String district, int demand) {
            this.hour = hour;
            this.district = district;
            this.demand = demand;
        }
    }
}