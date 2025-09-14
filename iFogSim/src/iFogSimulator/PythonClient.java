package iFogSimulator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * PythonClient - UPDATED
 *
 * This client now communicates with two separate CI microservices:
 * 1. The GA Classifier on port 5000 to get a priority label.
 * 2. The Fuzzy Allocator on port 6000 to get a resource decision.
 */
public class PythonClient {

    private final String gaServerUrl = "http://127.0.0.1:5000";
    private final String fuzzyServerUrl = "http://127.0.0.1:6000";

    public PythonClient() {
        // Constructor is now empty
    }

    /**
     * Calls the GA server to get a priority classification.
     * @return The priority label as a String (e.g., "HIGH", "MEDIUM", "LOW").
     */
    public String predictPriority(double drowsiness, double rain, double speed, double humidity) throws Exception {
        String endpoint = "/predict_priority";
        String payload = String.format(
                "{\"drowsiness\": %.4f, \"rain\": %.4f, \"speed\": %.2f, \"humidity\": %.4f}",
                drowsiness, rain, speed, humidity
        );

        String response = post(gaServerUrl + endpoint, payload);

        // Parse JSON response like {"priority": "HIGH"}
        if (response != null && response.contains("priority")) {
            return response.split(":")[1].replace("\"", "").replace("}", "").trim();
        }
        return "LOW"; // Fallback
    }

    /**
     * Calls the Fuzzy Logic server to get a resource allocation decision.
     * @param priorityLabel The label obtained from the GA server.
     * @return The full decision string from the fuzzy server.
     */
    public String allocateResources(String priorityLabel) throws Exception {
        String endpoint = "/allocate_resources";
        String payload = String.format("{\"priority\": \"%s\"}", priorityLabel);

        String response = post(fuzzyServerUrl + endpoint, payload);

        // Parse decision from a complex JSON, for logging we just return the 'decision' field
        if (response != null && response.contains("decision")) {
            // A simple parse for the main decision string
            String decision = response.split("\"decision\": \"")[1];
            return decision.substring(0, decision.length() - 2); // Remove trailing "}"
        }
        return "No allocation decision received.";
    }

    private String post(String urlString, String jsonPayload) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes());
            os.flush();
        }

        if (conn.getResponseCode() != 200) {
            conn.disconnect();
            throw new RuntimeException("Python server at " + urlString + " returned status: " + conn.getResponseCode());
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            conn.disconnect();
            return sb.toString();
        }
    }
}