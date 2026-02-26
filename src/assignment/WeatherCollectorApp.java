package assignment;

import com.google.gson.Gson;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// Swing app that fetches weather data sequentially and in parallel, then compares latency
public class WeatherCollectorApp extends JFrame {
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"City", "Temp", "Humidity", "Pressure"}, 0
    );
    private final JLabel sequentialTimingLabel = new JLabel("Sequential: - ms");
    private final JLabel parallelTimingLabel = new JLabel("Parallel: - ms");
    private final LatencyBarChart latencyChart = new LatencyBarChart();

    private final List<String> targetCities = List.of("Kathmandu", "Pokhara", "Biratnagar", "Nepalgunj", "Dhangadhi");
    private final String API_KEY = "e64b8a4b997441b800f4d11bb96fd0db";
    private final Gson gson = new Gson();

    public WeatherCollectorApp() {
        super("Weather Data Collector");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTable weatherTable = new JTable(tableModel);
        JButton fetchButton = new JButton("Fetch Weather");
        fetchButton.addActionListener(event -> fetchAllCityWeather());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(fetchButton);
        topPanel.add(sequentialTimingLabel);
        topPanel.add(parallelTimingLabel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(weatherTable), latencyChart);
        splitPane.setResizeWeight(0.7);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        setSize(700, 500);
        setLocationRelativeTo(null);
    }

    // POJO mapping the OpenWeatherMap API response
    static class ApiResponse {
        MainBlock main;
    }

    // POJO for the "main" block containing temp, humidity, pressure
    static class MainBlock {
        double temp;
        int humidity;
        double pressure;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WeatherCollectorApp().setVisible(true));
    }

    // Fetches weather sequentially and in parallel, then updates the UI
    private void fetchAllCityWeather() {
        tableModel.setRowCount(0);
        sequentialTimingLabel.setText("Sequential: fetching...");
        parallelTimingLabel.setText("Parallel: fetching...");

        // Run network tasks in a background thread to prevent GUI freeezing
        new Thread(() -> {
            // Sequential fetch
            long sequentialStartTime = System.nanoTime();
            Map<String, WeatherData> sequentialResults = new LinkedHashMap<>();
            for (String city : targetCities) sequentialResults.put(city, fetchWeatherForCity(city));
            long sequentialElapsedMs = Duration.ofNanos(System.nanoTime() - sequentialStartTime).toMillis();

            // Parallel fetch
            long parallelStartTime = System.nanoTime();
            Map<String, WeatherData> parallelResults = fetchWeatherInParallel();
            long parallelElapsedMs = Duration.ofNanos(System.nanoTime() - parallelStartTime).toMillis();

            // Safely push UI updates back to the Event Dispatch Thread
            SwingUtilities.invokeLater(() -> {
                // Display parallel results in table
                for (String city : targetCities) {
                    WeatherData weather = parallelResults.get(city);
                    tableModel.addRow(new Object[]{city, weather.temperature, weather.humidity, weather.pressure});
                }

                sequentialTimingLabel.setText("Sequential: " + sequentialElapsedMs + " ms");
                parallelTimingLabel.setText("Parallel: " + parallelElapsedMs + " ms");
                latencyChart.updateValues(sequentialElapsedMs, parallelElapsedMs);
                latencyChart.repaint();
            });
        }).start();
    }

    // Fetches weather for all cities in parallel using a thread pool
    private Map<String, WeatherData> fetchWeatherInParallel() {
        ExecutorService threadPool = Executors.newFixedThreadPool(5);
        try {
            List<Callable<Map.Entry<String, WeatherData>>> fetchTasks = new ArrayList<>();
            for (String city : targetCities) {
                fetchTasks.add(() -> Map.entry(city, fetchWeatherForCity(city)));
            }

            List<Future<Map.Entry<String, WeatherData>>> futures = threadPool.invokeAll(fetchTasks);
            Map<String, WeatherData> results = new LinkedHashMap<>();
            for (Future<Map.Entry<String, WeatherData>> future : futures) {
                Map.Entry<String, WeatherData> entry = future.get();
                results.put(entry.getKey(), entry.getValue());
            }
            return results;
        } catch (Exception exception) {
            return buildErrorFallbackMap();
        } finally {
            threadPool.shutdownNow();
        }
    }

    // Fetches weather for a single city from OpenWeatherMap API
    private WeatherData fetchWeatherForCity(String cityName) {
        try {
            String requestUrl = "https://api.openweathermap.org/data/2.5/weather?q="
                    + cityName + "&appid=" + API_KEY + "&units=metric";

            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[DEBUG] " + cityName + " → HTTP " + response.statusCode());
            System.out.println("[DEBUG] Body: " + response.body().substring(0, Math.min(200, response.body().length())));

            // Parse JSON response using Gson
            ApiResponse apiResponse = gson.fromJson(response.body(), ApiResponse.class);
            String temperature = String.valueOf(apiResponse.main.temp);
            String humidity = String.valueOf(apiResponse.main.humidity);
            String pressure = String.valueOf(apiResponse.main.pressure);

            return new WeatherData(temperature, humidity, pressure);
        } catch (Exception exception) {
            System.err.println("[ERROR] Failed for " + cityName + ": " + exception.getMessage());
            exception.printStackTrace();
            return new WeatherData("ERR", "ERR", "ERR");
        }
    }

    // Returns error placeholders for all cities
    private Map<String, WeatherData> buildErrorFallbackMap() {
        Map<String, WeatherData> errorMap = new LinkedHashMap<>();
        for (String city : targetCities) errorMap.put(city, new WeatherData("ERR", "ERR", "ERR"));
        return errorMap;
    }

    // Holds weather readings for a single city
    static class WeatherData {
        String temperature, humidity, pressure;

        WeatherData(String temperature, String humidity, String pressure) {
            this.temperature = temperature;
            this.humidity = humidity;
            this.pressure = pressure;
        }
    }

    // Bar chart panel comparing sequential vs parallel latency
    static class LatencyBarChart extends JPanel {
        long sequentialMs = 0, parallelMs = 0;

        void updateValues(long sequential, long parallel) {
            sequentialMs = sequential;
            parallelMs = parallel;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            int panelWidth = getWidth(), panelHeight = getHeight();
            int baseLine = panelHeight - 40;
            long maxLatency = Math.max(1, Math.max(sequentialMs, parallelMs));
            int barWidth = 120;

            int sequentialBarHeight = (int) ((sequentialMs / (double) maxLatency) * (panelHeight - 80));
            int parallelBarHeight = (int) ((parallelMs / (double) maxLatency) * (panelHeight - 80));

            graphics.drawString("Latency Comparison", 20, 20);

            graphics.fillRect(80, baseLine - sequentialBarHeight, barWidth, sequentialBarHeight);
            graphics.drawString("Sequential (" + sequentialMs + "ms)", 80, baseLine + 15);

            graphics.fillRect(260, baseLine - parallelBarHeight, barWidth, parallelBarHeight);
            graphics.drawString("Parallel (" + parallelMs + "ms)", 260, baseLine + 15);
        }
    }
}