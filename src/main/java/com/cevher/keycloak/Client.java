package com.cevher.keycloak;

import org.jboss.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.HashMap;

public class Client {
    private static final Logger log = Logger.getLogger(Client.class);
    private static final String WEBHOOK_URL = "http://180.76.225.182:8081/";

    public static void sendUserData(String data, String eventName) throws IOException {
        try {
            // final String urlString = System.getenv(WEBHOOK_URL);
            // log.debugf("WEBHOOK_URL: %s", urlString);

            final String urlString = WEBHOOK_URL + eventName;

            // if (urlString == null || urlString.isEmpty()) {
            // throw new IllegalArgumentException("Environment variable WEBHOOK_URL is not
            // set or is empty.");
            // }

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

    public static void updateUserProfile(JsonArray profileData, String realm,
            Map<String, Map<String, String>> localeMap) throws IOException {
        try {
            // 构建URL
            final String urlString = WEBHOOK_URL + "p/user-profile/update-user-profile";
            URL url = URI.create(urlString).toURL();

            // 设置连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");

            // 构建请求体
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("profileData", new Gson().fromJson(profileData, List.class));
            requestMap.put("realm", realm);
            requestMap.put("localeMap", localeMap);

            String jsonBody = new Gson().toJson(requestMap);

            // 写入请求体
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            // 处理响应
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed : HTTP error code : " + responseCode);
            }

            // 读取响应
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                log.infof("Response from server: %s", response.toString());
            }

            conn.disconnect();
        } catch (IOException e) {
            throw new IOException("Failed to update user profile: " + e.getMessage(), e);
        }
    }
}
