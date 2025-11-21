package com.example.redisclient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class Main {

    private static final String BASE_URL = "http://localhost:8080";
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) {
        try {
            System.out.println("--- Starting Redis Client Demo ---");

            // 1. Set Data
            System.out.println("\n[ACTION] Setting data...");
            String key = "savedData";
            String value = "I_Survived!";
            int ttl = 60;

            boolean setSuccess = setData(key, value, ttl);
            if (setSuccess) {
                System.out.println("[SUCCESS] Data set successfully.");
            } else {
                System.out.println("[FAILURE] Failed to set data.");
            }

            // 2. Get Data
            System.out.println("\n[ACTION] Getting data...");
            String retrievedValue = getData(key);
            System.out.println("[RESULT] Retrieved Value: " + retrievedValue);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean setData(String key, String value, int ttl) throws IOException, InterruptedException {
        // Manual JSON construction to avoid external dependencies
        String requestBody = String.format("{\"key\":\"%s\", \"value\":\"%s\", \"ttl\": %d}", key, value, ttl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/set"))
                .timeout(Duration.ofMinutes(1))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Response Code: " + response.statusCode());
        System.out.println("Response Body: " + response.body());

        return response.statusCode() == 200 || response.statusCode() == 201;
    }

    private static String getData(String key) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/get?key=" + key))
                .timeout(Duration.ofMinutes(1))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Response Code: " + response.statusCode());

        if (response.statusCode() == 200) {
            // Assuming the server returns the raw value or a JSON.
            // Based on the user prompt, it seems to return the value directly or maybe
            // JSON.
            // Let's just return the body for now.
            return response.body();
        } else {
            return "Error: " + response.statusCode();
        }
    }
}
