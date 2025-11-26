@echo off
docker compose up -d booking_db
start /b gradlew.bat build --continuous
gradlew.bat bootRun
