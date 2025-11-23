package com.example.redisclient;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A simple connection pool for RedisClient instances.
 * Reuses connections to avoid port exhaustion during high-load scenarios.
 */
public class RedisConnectionPool implements AutoCloseable {

    private final String host;
    private final int port;
    private final BlockingQueue<RedisClient> pool;
    private volatile boolean closed = false;

    public RedisConnectionPool(String host, int port, int maxConnections) throws IOException {
        this.host = host;
        this.port = port;
        this.pool = new ArrayBlockingQueue<>(maxConnections);

        // Pre-create connections
        for (int i = 0; i < maxConnections; i++) {
            pool.offer(new RedisClient(host, port));
        }
    }

    /**
     * Borrow a connection from the pool
     * 
     * @param timeout Maximum time to wait for a connection
     * @param unit    Time unit
     * @return A RedisClient instance, or null if timeout expires
     */
    public RedisClient borrowConnection(long timeout, TimeUnit unit) throws InterruptedException {
        if (closed) {
            throw new IllegalStateException("Pool is closed");
        }

        RedisClient client = pool.poll(timeout, unit);

        // If connection is dead, create a new one
        if (client != null && !client.isConnected()) {
            try {
                client.reconnect();
            } catch (IOException e) {
                // If reconnect fails, try to create a new one
                try {
                    client = new RedisClient(host, port);
                } catch (IOException ex) {
                    return null;
                }
            }
        }

        return client;
    }

    /**
     * Return a connection to the pool
     */
    public void returnConnection(RedisClient client) {
        if (client != null && !closed) {
            pool.offer(client);
        }
    }

    /**
     * Get the current number of available connections
     */
    public int availableConnections() {
        return pool.size();
    }

    @Override
    public void close() {
        closed = true;
        RedisClient client;
        while ((client = pool.poll()) != null) {
            client.close();
        }
    }
}
