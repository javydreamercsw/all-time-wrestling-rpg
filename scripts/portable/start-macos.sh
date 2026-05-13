#!/bin/bash
JAR_FILE=$(ls all-time-wrestling-rpg-*.jar | head -n 1)
echo "Starting All Time Wrestling RPG..."
java -jar "$JAR_FILE" --atw.desktop.enabled=true --spring.profiles.active=prod,h2
if [ $? -ne 0 ]; then
    echo "An error occurred. Press any key to exit."
    read -n 1
fi
