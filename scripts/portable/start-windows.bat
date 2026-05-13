@echo off
setlocal
for %%f in (all-time-wrestling-rpg-*.jar) do set JAR_FILE=%%f
echo Starting All Time Wrestling RPG...
java -jar %JAR_FILE% --atw.desktop.enabled=true --spring.profiles.active=prod,h2
if %ERRORLEVEL% neq 0 pause
endlocal
