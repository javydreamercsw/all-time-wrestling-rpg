@echo off
setlocal EnableDelayedExpansion

set "REPO=javydreamercsw/all-time-wrestling-rpg"
set "API_URL=https://api.github.com/repos/%REPO%/releases/latest"

:: ── Clean stale .tmp files older than 1 hour ─────────────────────────────
for %%f in (*.jar.tmp) do (
  forfiles /m "%%f" /d -0 /c "cmd /c del @file" 2>nul
)

:: ── Find current JAR ──────────────────────────────────────────────────────
set "CURRENT_JAR="
set "CURRENT_VER=0.0.0"
for %%f in (all-time-wrestling-rpg-*.jar) do (
  set "CURRENT_JAR=%%f"
  set "CURRENT_VER=%%~nf"
  :: Strip prefix to get just the version number
  set "CURRENT_VER=!CURRENT_VER:all-time-wrestling-rpg-=!"
)

:: ── Fetch latest release info via PowerShell ──────────────────────────────
echo Checking for updates...
for /f "delims=" %%i in ('powershell -NoProfile -Command "try { $r = Invoke-RestMethod -Uri '%API_URL%' -TimeoutSec 15 -ErrorAction Stop; Write-Output ($r.tag_name.TrimStart('v') + '|' + ($r.assets | Where-Object { $_.name -like '*.jar' -and $_.name -notlike '*.war' } | Select-Object -First 1 -ExpandProperty browser_download_url)) } catch { Write-Output 'ERROR' }" 2^>nul') do set "RELEASE_INFO=%%i"

if "%RELEASE_INFO%"=="" set "RELEASE_INFO=ERROR"
if "%RELEASE_INFO%"=="ERROR" (
  echo Could not reach GitHub -- launching existing version.
  goto :launch
)

for /f "tokens=1,2 delims=|" %%a in ("%RELEASE_INFO%") do (
  set "LATEST_VER=%%a"
  set "ASSET_URL=%%b"
)

if "%LATEST_VER%"=="" goto :launch
if "%ASSET_URL%"=="" goto :launch

:: ── Compare versions via PowerShell ──────────────────────────────────────
if "%CURRENT_JAR%"=="" (
  set "NEEDS_UPDATE=1"
) else (
  for /f %%r in ('powershell -NoProfile -Command "([version]'%LATEST_VER%' -gt [version]'%CURRENT_VER%')"') do set "NEWER=%%r"
  if /i "%NEWER%"=="True" (set "NEEDS_UPDATE=1") else (set "NEEDS_UPDATE=0")
)

if "%NEEDS_UPDATE%"=="0" (
  echo Already up to date ^(v%CURRENT_VER%^).
  goto :launch
)

:: ── Prompt user ───────────────────────────────────────────────────────────
if not "%CURRENT_JAR%"=="" (
  set /p "REPLY=Update v%LATEST_VER% available. Apply now? [y/N] "
  if /i not "!REPLY!"=="y" goto :launch
) else (
  echo No application JAR found. Downloading v%LATEST_VER%...
)

:: ── Download with PowerShell ──────────────────────────────────────────────
set "TMP_JAR=all-time-wrestling-rpg-%LATEST_VER%.jar.tmp"
set "NEW_JAR=all-time-wrestling-rpg-%LATEST_VER%.jar"

echo Downloading v%LATEST_VER%...
powershell -NoProfile -Command "Invoke-WebRequest -Uri '%ASSET_URL%' -OutFile '%TMP_JAR%' -TimeoutSec 600"

if errorlevel 1 (
  echo Download failed -- launching existing version.
  del /f /q "%TMP_JAR%" 2>nul
  goto :launch
)

:: Validate ZIP integrity
powershell -NoProfile -Command "Add-Type -AssemblyName System.IO.Compression.FileSystem; [System.IO.Compression.ZipFile]::OpenRead('%TMP_JAR%').Dispose()"
if errorlevel 1 (
  echo Downloaded file is corrupt -- keeping existing version.
  del /f /q "%TMP_JAR%" 2>nul
  goto :launch
)

if not "%CURRENT_JAR%"=="" ren "%CURRENT_JAR%" "%CURRENT_JAR%.old"
ren "%TMP_JAR%" "%NEW_JAR%"
set "CURRENT_JAR=%NEW_JAR%"
echo Update applied: v%LATEST_VER%

:launch
:: ── Find JAR to launch ────────────────────────────────────────────────────
set "JAR_FILE="
for %%f in (all-time-wrestling-rpg-*.jar) do set "JAR_FILE=%%f"

if "%JAR_FILE%"=="" (
  echo No application JAR found. Exiting.
  pause
  exit /b 1
)

echo Starting All Time Wrestling RPG ^(%JAR_FILE%^)...
java -jar "%JAR_FILE%" --atw.desktop.enabled=true --spring.profiles.active=prod,h2
set "EXIT_CODE=%ERRORLEVEL%"

if %EXIT_CODE% neq 0 (
  :: Attempt backup restore on crash
  for %%f in (*.jar.old) do (
    set "BACKUP=%%f"
    set "RESTORED=%%~nf"
    if not "!BACKUP!"=="" (
      echo Application crashed ^(exit %EXIT_CODE%^). Restoring previous version...
      ren "!BACKUP!" "!RESTORED!"
      del /f /q "%JAR_FILE%" 2>nul
      echo Restored. Please try launching again.
    )
  )
  pause
)

endlocal
exit /b %EXIT_CODE%
