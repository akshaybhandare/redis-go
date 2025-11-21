package com.example.redisclient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final int TOTAL_REQUESTS = 10000;
    // private static final int CONCURRENT_THREADS = 100; // Not used with virtual
    // threads

    public static void main(String[] args) {
        System.out.println("Starting Load Test with Virtual Threads...");
        System.out.println("Total Requests: " + TOTAL_REQUESTS);
        // System.out.println("Concurrent Threads: " + CONCURRENT_THREADS);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        // Use a Semaphore to limit concurrency to avoid overwhelming the server/OS
        final int MAX_CONCURRENCY = 200;
        java.util.concurrent.Semaphore semaphore = new java.util.concurrent.Semaphore(MAX_CONCURRENCY);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    semaphore.acquire();
                    try {
                        String key = "key-" + index;
                        String value = "value-" + index;

                        // PUT
                        if (setData(key, value, 60)) {
                            // GET
                            String retrieved = getData(key);
                            if (value.equals(retrieved)) {
                                successCount.incrementAndGet();
                            } else {
                                System.err.println("Mismatch for key: " + key);
                                failureCount.incrementAndGet();
                            }
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } finally {
                        semaphore.release();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    failureCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("\n--- Load Test Finished ---");
        System.out.println("Time Taken: " + duration + " ms");
        System.out.println("Successful Operations: " + successCount.get());
        System.out.println("Failed Operations: " + failureCount.get());
        System.out.println("Throughput: " + (TOTAL_REQUESTS * 1000.0 / duration) + " req/sec (approx)");
    }

    private static boolean setData(String key, String value, int ttl) throws IOException, InterruptedException {
        String requestBody = String.format("{\"key\":\"%s\", \"value\":\"%s\", \"ttl\": %d}", key, value, ttl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/set"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200 || response.statusCode() == 201;
    }

    private static String getData(String key) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/get?key=" + key))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            return null;
        }
    }
}
