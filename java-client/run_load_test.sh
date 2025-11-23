#!/bin/bash

set -e  # Exit on error

echo "=== Redis Load Test Setup ==="

# Clean and create output directory
echo "Cleaning output directory..."
rm -rf out
mkdir -p out

# Compile all Java files in one go (faster)
echo "Compiling Java files..."
javac -d out \
  src/main/java/com/example/redisclient/RedisClient.java \
  src/main/java/com/example/redisclient/RedisConnectionPool.java \
  src/main/java/com/example/redisclient/LoadTest.java

if [ $? -eq 0 ]; then
    echo "✓ Compilation successful"
    echo ""
    echo "=== Running Load Test ==="
    echo ""
    # Run the load test
    java -cp out com.example.redisclient.LoadTest
else
    echo "✗ Compilation failed"
    exit 1
fi
