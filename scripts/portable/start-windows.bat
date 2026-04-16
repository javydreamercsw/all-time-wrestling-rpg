@echo off
setlocal
set JAR_FILE=all-time-wrestling-rpg-1.7.0-SNAPSHOT.jar
echo Starting All Time Wrestling RPG...
java -jar %JAR_FILE% --atw.desktop.enabled=true --spring.profiles.active=prod
if %ERRORLEVEL% neq 0 pause
endlocal
