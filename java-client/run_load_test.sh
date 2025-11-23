#!/bin/bash

# Clean and create output directory
rm -rf out
mkdir -p out

# Compile all Java files
javac src/main/java/com/example/redisclient/RedisClient.java -d out
javac -cp out src/main/java/com/example/redisclient/RedisConnectionPool.java -d out
javac -cp out src/main/java/com/example/redisclient/LoadTest.java -d out

# Run the load test
java -cp out com.example.redisclient.LoadTest
