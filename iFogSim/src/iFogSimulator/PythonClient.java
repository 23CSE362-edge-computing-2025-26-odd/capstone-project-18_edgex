package iFogSimulator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * PythonClient
 *
 * Minimal HTTP client to call the Flask inference server.
 * - POST /predict_drowsiness  -> {"features":[...]}
 * - POST /predict_priority   -> {"drowsiness":0.5,"rain":0.2,"speed":60.0,"humidity":0.3}
 *
 * Returns JSON strings (very small).
 */
public class PythonClient {

    private final String baseUrl;

    public PythonClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public double predictPriority(double drowsiness, double rain, double speed, double humidity) throws Exception {
        String endpoint = "/predict_priority";
        String payload = String.format("{\"drowsiness\": %.4f, \"rain\": %.4f, \"speed\": %.2f, \"humidity\": %.4f}",
                drowsiness, rain, speed, humidity);
        String response = post(endpoint, payload);
        // crude parse: expect {"priority":0.6812}
        if (response == null || !response.contains("priority")) return 0.0;
        String num = response.replaceAll("[^0-9\\.]+", "");
        if (num.isEmpty()) return 0.0;
        return Double.parseDouble(num);
    }

    private String post(String endpoint, String jsonPayload) throws Exception {
        URL url = new URL(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(1500);
        conn.setReadTimeout(1500);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes());
            os.flush();
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            conn.disconnect();
            throw new RuntimeException("Python server returned status: " + status);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        conn.disconnect();
        return sb.toString();
    }
}