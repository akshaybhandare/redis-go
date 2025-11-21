#!/bin/bash
mkdir -p out
javac src/main/java/com/example/redisclient/Main.java -d out
java -cp out com.example.redisclient.Main
