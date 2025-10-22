package iFogSimulator;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PythonClient {

    private final String aiServerUrl = "http://127.0.0.1:5000";
    private final String fuzzyServerUrl = "http://127.0.0.1:8080";

    public PythonClient() {}

    /**
     * This method now accepts the correct features and builds the correct JSON payload.
     */
    public String predictPriority(double trafficDensity, int timeOfDay, int weather, double visibility) throws Exception {
        String endpoint = "/predict_priority";

        String payload = String.format(
                "{\"traffic_density\": %.2f, \"time_of_day\": %d, \"weather\": %d, \"visibility\": %.2f}",
                trafficDensity, timeOfDay, weather, visibility
        );

        String response = post(aiServerUrl + endpoint, payload);
        JSONObject jsonResponse = new JSONObject(response);
        return jsonResponse.getString("priority");
    }

    public String allocateResources(String priorityLabel) throws Exception {
        String endpoint = "/allocate_resources";
        String payload = String.format("{\"priority\": \"%s\"}", priorityLabel);
        String response = post(fuzzyServerUrl + endpoint, payload);
        return response; // Return the full JSON string
    }

    private String post(String urlString, String jsonPayload) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes());
            os.flush();
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    errorResponse.append(line);
                }
                throw new RuntimeException("Python server returned HTTP Status " + status + " with message: " + errorResponse.toString());
            }
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