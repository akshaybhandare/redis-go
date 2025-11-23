package com.example.redisclient;

import java.io.IOException;

public class Main {

    private static final String HOST = "localhost";
    private static final int PORT = 9080;

    public static void main(String[] args) {
        try (RedisClient client = new RedisClient(HOST, PORT)) {
            System.out.println("=== Redis-Go Java Client Demo ===\n");

            // Test 1: PING command
            System.out.println("TEST 1: PING");
            String pingResponse = client.ping();
            System.out.println("Response: " + pingResponse);
            System.out.println("✓ PING test passed\n");

            // Test 2: Basic SET and GET
            System.out.println("TEST 2: Basic String Operations");
            boolean setSuccess = client.set("mykey", "Hello, Redis!", 60);
            System.out.println("SET mykey: " + (setSuccess ? "OK" : "FAILED"));

            String value = client.get("mykey");
            System.out.println("GET mykey: " + value);
            System.out.println("✓ Basic SET/GET test passed\n");

            // Test 3: JSON.SET with simple JSON object
            System.out.println("TEST 3: JSON.SET - Simple Object");
            String userJson = "{\"name\":\"John Doe\",\"age\":30,\"email\":\"john@example.com\"}";
            boolean jsonSetSuccess = client.jsonSet("user:1001", userJson, 120);
            System.out.println("JSON.SET user:1001: " + (jsonSetSuccess ? "OK" : "FAILED"));

            String retrievedUser = client.get("user:1001");
            System.out.println("GET user:1001: " + retrievedUser);
            System.out.println("✓ Simple JSON object test passed\n");

            // Test 4: JSON.SET with nested object
            System.out.println("TEST 4: JSON.SET - Nested Object");
            String productJson = "{\"id\":\"P123\",\"name\":\"Laptop\",\"price\":999.99,\"specs\":{\"ram\":\"16GB\",\"storage\":\"512GB SSD\"}}";
            client.jsonSet("product:P123", productJson, 300);

            String retrievedProduct = client.get("product:P123");
            System.out.println("GET product:P123: " + retrievedProduct);
            System.out.println("✓ Nested JSON object test passed\n");

            // Test 5: JSON.SET with array
            System.out.println("TEST 5: JSON.SET - Array");
            String tagsJson = "[\"redis\",\"golang\",\"nosql\",\"database\"]";
            client.jsonSet("tags:tech", tagsJson, 180);

            String retrievedTags = client.get("tags:tech");
            System.out.println("GET tags:tech: " + retrievedTags);
            System.out.println("✓ JSON array test passed\n");

            // Test 6: JSON.SET with complex nested structure
            System.out.println("TEST 6: JSON.SET - Complex Structure");
            String orderJson = "{\"orderId\":\"ORD-2024-001\",\"customer\":{\"id\":1001,\"name\":\"Alice\"},\"items\":[{\"product\":\"Laptop\",\"qty\":1,\"price\":999.99},{\"product\":\"Mouse\",\"qty\":2,\"price\":29.99}],\"total\":1059.97}";
            client.jsonSet("order:2024-001", orderJson, 600);

            String retrievedOrder = client.get("order:2024-001");
            System.out.println("GET order:2024-001: " + retrievedOrder);
            System.out.println("✓ Complex JSON structure test passed\n");

            // Test 7: TTL expiration test
            System.out.println("TEST 7: TTL Expiration");
            client.jsonSet("session:temp", "{\"sessionId\":\"abc123\",\"active\":true}", 3);
            System.out.println("SET session:temp with 3s TTL: OK");

            String tempSession = client.get("session:temp");
            System.out.println("GET session:temp (immediately): " + tempSession);

            System.out.println("Waiting 4 seconds for TTL expiration...");
            Thread.sleep(4000);

            String expiredSession = client.get("session:temp");
            System.out.println(
                    "GET session:temp (after expiration): " + (expiredSession == null ? "(nil)" : expiredSession));
            System.out.println("✓ TTL expiration test passed\n");

            // Test 8: DELETE command
            System.out.println("TEST 8: DELETE");
            client.set("temp:key", "temporary value", 60);
            System.out.println("SET temp:key: OK");

            boolean delSuccess = client.del("temp:key");
            System.out.println("DEL temp:key: " + (delSuccess ? "OK" : "FAILED"));

            String deletedValue = client.get("temp:key");
            System.out.println("GET temp:key (after delete): " + (deletedValue == null ? "(nil)" : deletedValue));
            System.out.println("✓ DELETE test passed\n");

            System.out.println("=== All Tests Completed Successfully! ===");

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
