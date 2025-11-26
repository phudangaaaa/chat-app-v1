@echo off
REM Script to run ChatServer on Windows

echo Starting Chat Server...

cd ChatServer

REM Compile and run
mvn clean compile exec:java -Dexec.mainClass="com.chatapp.server.ChatServer"

pause
