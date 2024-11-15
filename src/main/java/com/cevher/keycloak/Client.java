package com.cevher.keycloak;

import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class Client {
    private static final Logger log = Logger.getLogger(Client.class);
    private static final String WEBHOOK_URL = "http://180.76.225.182:4567/";

    public static void postService(String data, String eventName) throws IOException {
        try {
//            final String urlString = System.getenv(WEBHOOK_URL);
//            log.debugf("WEBHOOK_URL: %s", urlString);

            final String urlString = WEBHOOK_URL + eventName;


//            if (urlString == null || urlString.isEmpty()) {
//                throw new IllegalArgumentException("Environment variable WEBHOOK_URL is not set or is empty.");
//            }

            URL url = URI.create(urlString).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");

            OutputStream os = conn.getOutputStream();
            os.write(data.getBytes());
            os.flush();

            final int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_CREATED && responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed : HTTP error code : " + responseCode);
            }

            final BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String output;
            log.debugf("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
                log.debugf("Input from Server: %s", output);
            }
            conn.disconnect();
        } catch (IOException e) {
            throw new IOException("Failed to post service: " + e.getMessage(), e);
        }
    }
}
