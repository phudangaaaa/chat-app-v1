@echo off
REM Script to run ChatClient with JavaFX on Windows

echo Starting Chat Client...

cd ChatClient

REM Run using Maven (recommended)
mvn clean javafx:run

pause
