package assignment;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

// Swing app that suggests tourist itineraries using greedy and brute-force strategies
public class TouristOptimizerApp extends JFrame {

    private final JTextField totalTimeField = new JTextField("6");
    private final JTextField maxBudgetField = new JTextField("500");
    private final JTextField interestTagsField = new JTextField("culture,heritage");
    private final JTextArea resultOutput = new JTextArea(12, 40);
    private final RouteMapPanel routeMapPanel = new RouteMapPanel();
    private final List<TouristSpot> availableSpots = createSampleSpots();

    public TouristOptimizerApp() {
        super("Tourist Spot Optimizer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel inputPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        inputPanel.add(new JLabel("Total time (hours):"));
        inputPanel.add(totalTimeField);
        inputPanel.add(new JLabel("Max budget (Rs.):"));
        inputPanel.add(maxBudgetField);
        inputPanel.add(new JLabel("Interest tags (comma):"));
        inputPanel.add(interestTagsField);

        JButton suggestButton = new JButton("Suggest Itinerary");
        suggestButton.addActionListener(event -> solveAndDisplay());

        resultOutput.setEditable(false);

        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.add(inputPanel, BorderLayout.NORTH);
        leftPanel.add(suggestButton, BorderLayout.CENTER);
        leftPanel.add(new JScrollPane(resultOutput), BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, routeMapPanel);
        splitPane.setResizeWeight(0.55);

        setContentPane(splitPane);
        pack();
        setLocationRelativeTo(null);
    }

    // Counts how many user interest tags match this spot
    private static int countInterestMatches(TouristSpot spot, Set<String> userInterests) {
        int matchCount = 0;
        for (String tag : userInterests) if (spot.tags.contains(tag)) matchCount++;
        return matchCount;
    }

    // Euclidean distance between two spots (simplified)
    private static double calculateDistance(TouristSpot spotA, TouristSpot spotB) {
        double latDiff = spotA.latitude - spotB.latitude;
        double lonDiff = spotA.longitude - spotB.longitude;
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff);
    }

    // Converts "HH:MM" to total minutes from midnight
    private static int parseTimeToMinutes(String hoursMinutes) {
        String[] parts = hoursMinutes.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    // Parses comma-separated tags into a lowercase set
    private static Set<String> parseInterestTags(String tagsInput) {
        Set<String> tagSet = new HashSet<>();
        for (String tag : tagsInput.split(",")) {
            String trimmedTag = tag.trim().toLowerCase();
            if (!trimmedTag.isEmpty()) tagSet.add(trimmedTag);
        }
        return tagSet;
    }

    // Returns top K spots sorted by interest match count
    private static List<TouristSpot> getTopCandidates(List<TouristSpot> allSpots, Set<String> userInterests, int maxCandidates) {
        List<TouristSpot> sorted = new ArrayList<>(allSpots);
        sorted.sort((a, b) -> Integer.compare(countInterestMatches(b, userInterests), countInterestMatches(a, userInterests)));
        return sorted.subList(0, Math.min(maxCandidates, sorted.size()));
    }

    // Generates all permutations of the list
    private static List<List<TouristSpot>> generatePermutations(List<TouristSpot> items) {
        List<List<TouristSpot>> allPermutations = new ArrayList<>();
        permuteRecursive(items, 0, allPermutations);
        return allPermutations;
    }

    // Recursive permutation generator using swaps
    private static void permuteRecursive(List<TouristSpot> items, int currentIndex, List<List<TouristSpot>> allPermutations) {
        if (currentIndex == items.size()) {
            allPermutations.add(new ArrayList<>(items));
            return;
        }
        for (int swapIndex = currentIndex; swapIndex < items.size(); swapIndex++) {
            Collections.swap(items, currentIndex, swapIndex);
            permuteRecursive(items, currentIndex + 1, allPermutations);
            Collections.swap(items, currentIndex, swapIndex);
        }
    }

    // Sample tourist spots in the Kathmandu Valley
    private static List<TouristSpot> createSampleSpots() {
        return List.of(
                new TouristSpot("Pashupatinath Temple", 27.7104, 85.3488, 100, "06:00", "18:00", "culture", "religious"),
                new TouristSpot("Swayambhunath Stupa", 27.7149, 85.2906, 200, "07:00", "17:00", "culture", "heritage"),
                new TouristSpot("Garden of Dreams", 27.7125, 85.3170, 150, "09:00", "21:00", "nature", "relaxation"),
                new TouristSpot("Chandragiri Hills", 27.6616, 85.2458, 700, "09:00", "17:00", "nature", "adventure"),
                new TouristSpot("Kathmandu Durbar Square", 27.7048, 85.3076, 100, "10:00", "17:00", "culture", "heritage")
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TouristOptimizerApp().setVisible(true));
    }

    // Runs both strategies and displays results
    private void solveAndDisplay() {
        int availableTimeHours = Integer.parseInt(totalTimeField.getText().trim());
        int budgetLimit = Integer.parseInt(maxBudgetField.getText().trim());
        Set<String> userInterests = parseInterestTags(interestTagsField.getText());

        // Greedy heuristic route
        List<TouristSpot> greedyRoute = buildGreedyRoute(availableSpots, availableTimeHours, budgetLimit, userInterests);

        // Brute-force on top 6 candidates
        List<TouristSpot> topCandidates = getTopCandidates(availableSpots, userInterests, 6);
        List<TouristSpot> optimalRoute = findBestRouteByBruteForce(topCandidates, availableTimeHours, budgetLimit, userInterests);

        resultOutput.setText("");
        resultOutput.append("Heuristic itinerary:\n");
        displayRoute(greedyRoute, userInterests);

        resultOutput.append("\nBrute force (on <=6 spots):\n");
        displayRoute(optimalRoute, userInterests);

        routeMapPanel.setRoute(greedyRoute);
        routeMapPanel.repaint();
    }

    // Prints route details to output area
    private void displayRoute(List<TouristSpot> route, Set<String> userInterests) {
        int totalEntryFee = 0;
        double totalDistance = 0;

        for (int i = 0; i < route.size(); i++) {
            TouristSpot spot = route.get(i);
            int matchCount = countInterestMatches(spot, userInterests);
            totalEntryFee += spot.entryFee;
            if (i > 0) totalDistance += calculateDistance(route.get(i - 1), spot);
            resultOutput.append(String.format("%d) %s | fee=%d | match=%d\n", i + 1, spot.name, spot.entryFee, matchCount));
        }
        resultOutput.append(String.format("Total fee: %d | Approx distance: %.3f\n", totalEntryFee, totalDistance));
    }

    // Greedy: pick the next best unvisited spot by score until constraints hit
    private List<TouristSpot> buildGreedyRoute(List<TouristSpot> allSpots, int availableTimeHours, int budgetLimit, Set<String> userInterests) {
        List<TouristSpot> unvisitedSpots = new ArrayList<>(allSpots);
        List<TouristSpot> selectedRoute = new ArrayList<>();
        int remainingBudget = budgetLimit;
        int remainingMinutes = availableTimeHours * 60;
        TouristSpot currentLocation = null;

        while (!unvisitedSpots.isEmpty()) {
            TouristSpot bestNextSpot = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (TouristSpot candidateSpot : unvisitedSpots) {
                if (candidateSpot.entryFee > remainingBudget) continue;
                int visitDuration = 60;
                int travelTime = (currentLocation == null) ? 0 : (int) Math.round(calculateDistance(currentLocation, candidateSpot) * 200);
                if (visitDuration + travelTime > remainingMinutes) continue;

                int matchCount = countInterestMatches(candidateSpot, userInterests);
                double distancePenalty = (currentLocation == null) ? 0 : calculateDistance(currentLocation, candidateSpot);
                double score = matchCount * 10 - distancePenalty - (candidateSpot.entryFee / 200.0);

                if (score > bestScore) {
                    bestScore = score;
                    bestNextSpot = candidateSpot;
                }
            }

            if (bestNextSpot == null) break;
            selectedRoute.add(bestNextSpot);
            unvisitedSpots.remove(bestNextSpot);

            remainingBudget -= bestNextSpot.entryFee;
            int travelTime = (currentLocation == null) ? 0 : (int) Math.round(calculateDistance(currentLocation, bestNextSpot) * 200);
            remainingMinutes -= (60 + travelTime);
            currentLocation = bestNextSpot;
        }

        return selectedRoute;
    }

    // Brute-force: tries all permutations and prefix lengths to find the best valid route
    private List<TouristSpot> findBestRouteByBruteForce(List<TouristSpot> candidateSpots, int availableTimeHours, int budgetLimit, Set<String> userInterests) {
        List<TouristSpot> bestRoute = new ArrayList<>();
        double bestScore = Double.NEGATIVE_INFINITY;

        List<TouristSpot> mutableCandidates = new ArrayList<>(candidateSpots);
        for (List<TouristSpot> permutation : generatePermutations(mutableCandidates)) {
            for (int routeLength = 1; routeLength <= permutation.size(); routeLength++) {
                List<TouristSpot> candidateRoute = permutation.subList(0, routeLength);
                Double score = evaluateRouteIfValid(candidateRoute, availableTimeHours, budgetLimit, userInterests);
                if (score != null && score > bestScore) {
                    bestScore = score;
                    bestRoute = new ArrayList<>(candidateRoute);
                }
            }
        }
        return bestRoute;
    }

    // Returns route score if valid, null if it violates time or budget constraints
    private Double evaluateRouteIfValid(List<TouristSpot> route, int availableTimeHours, int budgetLimit, Set<String> userInterests) {
        int accumulatedFee = 0;
        int totalAvailableMinutes = availableTimeHours * 60;
        int minutesUsed = 0;
        TouristSpot previousSpot = null;
        int totalMatchScore = 0;
        double totalDistance = 0;

        for (TouristSpot spot : route) {
            accumulatedFee += spot.entryFee;
            if (accumulatedFee > budgetLimit) return null;

            int travelTime = (previousSpot == null) ? 0 : (int) Math.round(calculateDistance(previousSpot, spot) * 200);
            minutesUsed += travelTime + 60;
            if (minutesUsed > totalAvailableMinutes) return null;

            totalMatchScore += countInterestMatches(spot, userInterests);
            if (previousSpot != null) totalDistance += calculateDistance(previousSpot, spot);
            previousSpot = spot;
        }

        return totalMatchScore * 10 - totalDistance - (accumulatedFee / 200.0);
    }

    // Tourist spot with location, fee, hours, and interest tags
    static class TouristSpot {
        String name;
        double latitude, longitude;
        int entryFee;
        int openingTimeMinutes, closingTimeMinutes;
        Set<String> tags;

        TouristSpot(String name, double latitude, double longitude, int entryFee, String openTime, String closeTime, String... tags) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.entryFee = entryFee;
            this.openingTimeMinutes = parseTimeToMinutes(openTime);
            this.closingTimeMinutes = parseTimeToMinutes(closeTime);
            this.tags = new HashSet<>();
            for (String tag : tags) this.tags.add(tag.toLowerCase());
        }
    }

    // Panel that draws the route as dots connected by lines
    static class RouteMapPanel extends JPanel {
        private List<TouristSpot> route = List.of();

        void setRoute(List<TouristSpot> newRoute) {
            route = newRoute;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (route == null || route.isEmpty()) return;

            double minLat = route.stream().mapToDouble(spot -> spot.latitude).min().orElse(0);
            double maxLat = route.stream().mapToDouble(spot -> spot.latitude).max().orElse(1);
            double minLon = route.stream().mapToDouble(spot -> spot.longitude).min().orElse(0);
            double maxLon = route.stream().mapToDouble(spot -> spot.longitude).max().orElse(1);

            int panelWidth = getWidth(), panelHeight = getHeight();
            int padding = 30;
            int previousX = -1, previousY = -1;

            for (int i = 0; i < route.size(); i++) {
                TouristSpot spot = route.get(i);
                int pixelX = padding + (int) ((spot.latitude - minLat) / (maxLat - minLat + 1e-9) * (panelWidth - 2 * padding));
                int pixelY = padding + (int) ((spot.longitude - minLon) / (maxLon - minLon + 1e-9) * (panelHeight - 2 * padding));

                graphics.fillOval(pixelX - 5, pixelY - 5, 10, 10);
                graphics.drawString((i + 1) + "", pixelX + 8, pixelY);

                if (i > 0) graphics.drawLine(previousX, previousY, pixelX, pixelY);
                previousX = pixelX;
                previousY = pixelY;
            }
        }
    }
}