package com.example.redisclient;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTest {

    private static final String HOST = "localhost";
    private static final int PORT = 1122;
    private static final int POOL_SIZE = 50; // Reuse 50 connections

    private static final int TOTAL_REQUESTS = 10000;
    // private static final int CONCURRENT_THREADS = 100; // Not used with virtual
    // threads

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Load Test with Virtual Threads and Connection Pooling...");
        System.out.println("Total Requests: " + TOTAL_REQUESTS);
        System.out.println("Connection Pool Size: " + POOL_SIZE);

        // Create connection pool
        RedisConnectionPool pool = new RedisConnectionPool(HOST, PORT, POOL_SIZE);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        // Use a Semaphore to limit concurrency to avoid overwhelming the server/OS
        // final int MAX_CONCURRENCY = 200;
        // java.util.concurrent.Semaphore semaphore = new
        // java.util.concurrent.Semaphore(MAX_CONCURRENCY);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            final int index = i;
            executor.submit(() -> {
                RedisClient client = null;
                try {
                    // Borrow connection from pool
                    client = pool.borrowConnection(10, TimeUnit.SECONDS);

                    if (client == null) {
                        System.err.println("Failed to get connection from pool");
                        failureCount.incrementAndGet();
                        return;
                    }

                    String key = "key-" + index;
                    String value = "value-" + index;

                    // Create a new client for each request (connection pooling could be added
                    // later)
                    // try (RedisClient client = new RedisClient(HOST, PORT)) {
                    // SET
                    if (setData(client, key, value, 60)) {
                        // GET
                        String retrieved = getData(client, key);
                        if (value.equals(retrieved)) {
                            successCount.incrementAndGet();
                        } else {
                            System.err.println("Mismatch for key: " + key);
                            failureCount.incrementAndGet();
                        }
                    } else {
                        failureCount.incrementAndGet();
                    }
                    // }
                } catch (Exception e) {
                    System.err.println("Error processing request " + index + ": " + e.getMessage());
                    failureCount.incrementAndGet();
                } finally {
                    // Return connection to pool
                    if (client != null) {
                        pool.returnConnection(client);
                    }
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

        // Close the pool
        pool.close();

        System.out.println("\n--- Load Test Finished ---");
        System.out.println("Time Taken: " + duration + " ms");
        System.out.println("Successful Operations: " + successCount.get());
        System.out.println("Failed Operations: " + failureCount.get());
        System.out.println("Throughput: " + (TOTAL_REQUESTS * 1000.0 / duration) + " req/sec (approx)");
    }

    private static boolean setData(RedisClient client, String key, String value, int ttl) throws IOException {
        return client.set(key, value, ttl);
    }

    private static String getData(RedisClient client, String key) throws IOException {
        return client.get(key);
    }
}
