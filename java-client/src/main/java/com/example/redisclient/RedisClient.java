package com.example.redisclient;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * A simple Redis client that communicates using the RESP (Redis Serialization
 * Protocol).
 * Supports basic commands: SET, GET, DEL, PING
 */
public class RedisClient implements AutoCloseable {

    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader reader;
    private OutputStream writer;

    public RedisClient(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        connect();
    }

    private void connect() throws IOException {
        this.socket = new Socket(host, port);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = socket.getOutputStream();
    }

    /**
     * Send a PING command to test connectivity
     */
    public String ping() throws IOException {
        sendCommand("PING");
        return readSimpleString();
    }

    /**
     * Set a key-value pair with optional TTL
     * 
     * @param key   The key to set
     * @param value The value to store
     * @param ttl   Time-to-live in seconds (0 for no expiration)
     * @return true if successful
     */
    public boolean set(String key, String value, int ttl) throws IOException {
        if (ttl > 0) {
            sendCommand("SET", key, value, String.valueOf(ttl));
        } else {
            sendCommand("SET", key, value);
        }
        String response = readSimpleString();
        return "OK".equals(response);
    }

    /**
     * Get the value for a key
     * 
     * @param key The key to retrieve
     * @return The value, or null if key doesn't exist
     */
    public String get(String key) throws IOException {
        sendCommand("GET", key);
        return readBulkString();
    }

    /**
     * Delete a key
     * 
     * @param key The key to delete
     * @return true if successful
     */
    public boolean del(String key) throws IOException {
        sendCommand("DEL", key);
        String response = readSimpleString();
        return "OK".equals(response);
    }

    /**
     * Send a command using RESP protocol
     * Format: *<number of arguments>\r\n$<length of argument 1>\r\n<argument
     * 1>\r\n...
     */
    private void sendCommand(String... args) throws IOException {
        StringBuilder command = new StringBuilder();

        // Array header
        command.append("*").append(args.length).append("\r\n");

        // Each argument as bulk string
        for (String arg : args) {
            byte[] argBytes = arg.getBytes(StandardCharsets.UTF_8);
            command.append("$").append(argBytes.length).append("\r\n");
            command.append(arg).append("\r\n");
        }

        writer.write(command.toString().getBytes(StandardCharsets.UTF_8));
        writer.flush();
    }

    /**
     * Read a simple string response (starts with '+')
     */
    private String readSimpleString() throws IOException {
        String line = reader.readLine();
        if (line == null || line.isEmpty()) {
            throw new IOException("Empty response from server");
        }

        if (line.startsWith("+")) {
            return line.substring(1).trim();
        } else if (line.startsWith("-")) {
            throw new IOException("Redis error: " + line.substring(1));
        } else {
            throw new IOException("Unexpected response type: " + line);
        }
    }

    /**
     * Read a bulk string response (starts with '$')
     */
    private String readBulkString() throws IOException {
        String line = reader.readLine();
        if (line == null || line.isEmpty()) {
            throw new IOException("Empty response from server");
        }

        if (line.startsWith("$")) {
            int length = Integer.parseInt(line.substring(1));

            if (length == -1) {
                return null; // Key doesn't exist
            }

            // Read the actual data
            char[] data = new char[length];
            int totalRead = 0;
            while (totalRead < length) {
                int read = reader.read(data, totalRead, length - totalRead);
                if (read == -1) {
                    throw new IOException("Unexpected end of stream");
                }
                totalRead += read;
            }

            // Read the trailing \r\n
            reader.readLine();

            return new String(data);
        } else if (line.startsWith("-")) {
            throw new IOException("Redis error: " + line.substring(1));
        } else {
            throw new IOException("Unexpected response type: " + line);
        }
    }

    /**
     * Check if the connection is still alive
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Reconnect if connection is lost
     */
    public void reconnect() throws IOException {
        close();
        connect();
    }

    @Override
    public void close() {
        try {
            if (reader != null)
                reader.close();
            if (writer != null)
                writer.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            // Ignore close errors
        }
    }
}
