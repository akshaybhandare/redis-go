#!/bin/bash
mkdir -p out
javac src/main/java/com/example/redisclient/LoadTest.java -d out
java -cp out com.example.redisclient.LoadTest
