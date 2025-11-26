#!/bin/bash

# Script to run ChatServer

echo "Starting Chat Server..."

cd ChatServer

# Compile and run
mvn clean compile exec:java -Dexec.mainClass="com.chatapp.server.ChatServer"
