#!/bin/bash
JAR_FILE="all-time-wrestling-rpg-1.7.0-SNAPSHOT.jar"
echo "Starting All Time Wrestling RPG..."
java -jar "$JAR_FILE" --atw.desktop.enabled=true --spring.profiles.active=prod
if [ $? -ne 0 ]; then
    echo "An error occurred. Press any key to exit."
    read -n 1
fi
