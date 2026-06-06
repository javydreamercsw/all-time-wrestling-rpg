# Database Maintenance Guide

This document outlines the maintenance procedures for the All Time Wrestling RPG database.

---

## Deploying to Railway (Docker)

Railway runs the application from the `Dockerfile` in the repository root. The image uses
embedded Jetty (no standalone Tomcat) and defaults to H2 unless MySQL environment variables
are provided.

### Required environment variables

Set these in your Railway service's **Variables** panel:

|                 Variable                  |          Description           |                      Example                       |
|-------------------------------------------|--------------------------------|----------------------------------------------------|
| `SPRING_PROFILES_ACTIVE`                  | Database profile               | `prod,mysql` (MySQL) or `prod,h2` (H2)             |
| `SPRING_DATASOURCE_URL`                   | JDBC URL of your MySQL service | `jdbc:mysql://mysql.railway.internal:3306/railway` |
| `SPRING_DATASOURCE_USERNAME`              | Database username              | `root`                                             |
| `SPRING_DATASOURCE_PASSWORD`              | Database password              | _(from Railway MySQL service)_                     |
| `ATW_STORAGE_BASE_DIR`                    | Root path for images and data  | `/data`                                            |
| `AI_CLAUDE_ENABLED` / `AI_CLAUDE_API_KEY` | Enable Claude AI narration     | `true` / your key                                  |
| `AI_GEMINI_ENABLED` / `AI_GEMINI_API_KEY` | Enable Gemini AI narration     | `true` / your key                                  |
| `AI_OPENAI_ENABLED` / `AI_OPENAI_API_KEY` | Enable OpenAI narration        | `true` / your key                                  |

> Railway injects `PORT` automatically; the app reads it via `server.port=${PORT:8080}`.
> Do **not** set `PORT` manually.

### Persistent image storage (Railway Volume)

The application stores AI-generated and uploaded images under `${ATW_STORAGE_BASE_DIR}`:

```
/data/images/generated/   ← AI-generated images
/data/images/defaults/    ← manually uploaded images
```

The `Dockerfile` declares `/data` as a `VOLUME`. Without a persistent volume, images are lost
every time the container restarts or redeploys.

**To attach a Railway Volume:**

1. In your Railway project, open the service → **Volumes** tab → **Add Volume**.
2. Set the **Mount Path** to `/data`.
3. Redeploy — the volume persists across all future deploys.

### Migrating existing images from a local install

If you have images on a local machine (typically `~/.atwrpg/images/`) and want to move them
to Railway:

```bash
# 1. SSH into the Railway container (requires Railway CLI)
railway shell

# 2. On your local machine, copy images to the running container via Railway's volume
#    Or use mysqldump / a one-time upload script
```

The simplest approach is to re-upload images through the application UI after deploying, or
to use `railway cp` once Railway CLI supports it.

### H2 mode (no MySQL)

Leaving `SPRING_PROFILES_ACTIVE=prod,h2` and attaching a volume at `/data` also persists the
H2 database file across restarts:

```
/data/db/atwrpg.mv.db   ← H2 database file (created automatically)
```

Set the datasource URL to a file-based H2 path inside the volume:

```
SPRING_DATASOURCE_URL=jdbc:h2:file:/data/db/atwrpg;AUTO_SERVER=FALSE
SPRING_DATASOURCE_USERNAME=sa
SPRING_DATASOURCE_PASSWORD=
```

> `AUTO_SERVER=FALSE` is required in Docker — the embedded server mode used for desktop
> installs does not work inside a single-container deployment.

### Local Docker run (testing the image)

```bash
# Build
docker build -t atwrpg .

# Run with H2 (no database required)
docker run --rm \
  -p 8080:8080 \
  -v "$HOME/.atwrpg:/data" \
  -e ATW_STORAGE_BASE_DIR=/data \
  atwrpg

# Run with MySQL
docker run --rm \
  -p 8080:8080 \
  -v "$HOME/.atwrpg:/data" \
  -e ATW_STORAGE_BASE_DIR=/data \
  -e SPRING_PROFILES_ACTIVE=prod,mysql \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/atwrpg \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=secret \
  atwrpg
```

Mounting `~/.atwrpg` at `/data` reuses any images and (for H2 mode) the database already on
your machine.

---

## Deploying to Tomcat as a Windows Service

When Tomcat runs as a **Windows service** (installed via the Apache Tomcat Windows installer), it uses
the `Procrun` service wrapper (`Tomcat11.exe`) and **does not execute `setenv.bat`**. All JVM
options and environment variables must be configured through the Tomcat service manager GUI instead.

### Key difference from command-line startup

|          Startup method          | Reads `setenv.bat`? |          Configure via           |
|----------------------------------|---------------------|----------------------------------|
| `catalina.bat` / `startup.bat`   | ✅ Yes               | `setenv.bat`                     |
| Windows service (`Tomcat11.exe`) | ❌ No                | `tomcat11w.exe` Java Options tab |

### Configuring JVM options

Open the Tomcat Monitor GUI **as Administrator**:

```cmd
"C:\Program Files\Apache Software Foundation\Tomcat 11.0\bin\tomcat11w.exe" //ES//Tomcat11
```

Go to the **Java** tab and add the following to the **Java Options** box (one per line):

```
-Dspring.profiles.active=prod,mysql
-Dspring.datasource.url=jdbc:mysql://<host>:<port>/<database>
-Dspring.datasource.username=<db_user>
-Dspring.datasource.password=<db_password>
-Dspring.flyway.locations=classpath:db/migration/mysql
-Xms512m
-Xmx2g
```

> ⚠️ **Critical naming convention:** JVM system properties (`-D` flags) are **case-sensitive** and
> must use lowercase dot-notation. `-Dspring.profiles.active=prod,mysql` works.
> `-DSPRING_PROFILES_ACTIVE=prod,mysql` (uppercase) is silently ignored because Spring Boot only
> matches the exact property name, not the environment-variable relaxed-binding equivalent.

Set **Initial memory pool** to `512` MB and **Maximum memory pool** to `2048` MB in the same tab,
then click **Apply**. Restart the service for changes to take effect.

### Required Spring profiles

The application needs **both** `prod` and `mysql` active, with `mysql` last so its datasource
settings take precedence:

```
-Dspring.profiles.active=prod,mysql
```

`application.properties` defaults to `spring.profiles.active=h2`; the JVM option overrides it.

### Tomcat Manager roles for remote deploy

The Cargo Maven plugin (`remote-deploy` profile) uses the Tomcat Manager **text interface**
(`/manager/text`). The user in `conf/tomcat-users.xml` needs **both** roles:

```xml
<user username="<user>" password="<password>" roles="manager-gui,manager-script" />
```

`manager-gui` alone is not sufficient — the text API requires `manager-script`.

> **Note:** `tomcat-users.xml` is reloaded on each request; no restart is needed after editing it.
> The service **does** need a restart to pick up changes to JVM options.

### Image storage when running as LOCAL SERVICE

Tomcat's Windows service runs under the **LOCAL SERVICE** account. The application stores generated
and uploaded images in the user home directory:

```
C:\Windows\ServiceProfiles\LocalService\.atwrpg\images\generated\   ← AI-generated images
C:\Windows\ServiceProfiles\LocalService\.atwrpg\images\defaults\    ← uploaded/manual images
```

To migrate images from another machine, copy the contents of `~/.atwrpg/images/` on the source
machine into the corresponding folders above.

### First-deploy startup time

On the very first deployment against a fresh database, the `DataInitializer` seeds all reference
data (wrestlers, NPCs, cards, arenas, etc.) — this can take several minutes against a remote
database due to network latency.

On all **subsequent** deployments the initializer runs a dirty-check: it only writes records whose
fields have actually changed since the last run. Against an already-populated database with no
game-file changes, the init step completes in seconds.

If for any reason you need to skip the initializer entirely (e.g. during debugging), add:

```
-Ddata.initializer.enabled=false
```

Or to skip sync methods only when data already exists:

```
-Ddata.initializer.skip-if-not-empty=true
```

---

## MySQL Setup & Migration (Windows)

This section covers installing MySQL on Windows, migrating your production database locally, and running the application against it.

### 1. Install MySQL on Windows

1. Download **MySQL Installer** from https://dev.mysql.com/downloads/installer/
2. Run the installer and choose **Developer Default** (includes MySQL Server, Workbench, and Shell).
3. During configuration, set a root password and note it down — you will need it in the steps below.
4. Ensure **MySQL Server** is added to `PATH` during installation (the installer offers this option).
5. Verify installation:

```powershell
mysql --version
```

### 2. Create the Application Database

Open a terminal (PowerShell or MySQL Shell) and run:

```
mysql -u root -p
```

```sql
CREATE DATABASE atw_rpg CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'atwrpg'@'localhost' IDENTIFIED BY 'YOUR_PASSWORD';
GRANT ALL PRIVILEGES ON atw_rpg.* TO 'atwrpg'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 3. Migrate Production Database to Local

**On the production server**, dump the database:

```bash
mysqldump -u root -p atw_rpg > atw_rpg_prod_backup.sql
```

Transfer the dump file to this Windows machine (via SCP, SFTP, USB, or cloud storage), then import it:

```powershell
mysql -u atwrpg -p atw_rpg < C:\path\to\atw_rpg_prod_backup.sql
```

### 4. Configure the Application for MySQL

Create or edit `src/main/resources/application-local.properties` (or set environment variables):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/atw_rpg?useSSL=false&serverTimezone=UTC
spring.datasource.username=atwrpg
spring.datasource.password=YOUR_PASSWORD
spring.profiles.include=mysql
```

Then start the app pointing at the local profile:

```powershell
./mvnw spring-boot:run "-Dspring-boot.run.profiles=local"
```

Navigate to http://localhost:8080/atw-rpg to verify the app is running against the migrated data.

### 5. Stop the App (Windows)

```powershell
# Find and kill the Java process
Get-Process -Name java | Stop-Process -Force
# Or press Ctrl+C in the terminal where it's running
```

---

## MySQL Automated Backups (Windows)

The Windows equivalent of the macOS backup uses a PowerShell script scheduled via **Task Scheduler**.

### 1. Backup Script

Save this script as `C:\scripts\mysql_backup.ps1`:

```powershell
$BackupDir = "$env:USERPROFILE\mysql_backups"
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$DumpPath  = "$BackupDir\all_databases_$Timestamp.sql"

if (-not (Test-Path $BackupDir)) { New-Item -ItemType Directory -Force $BackupDir | Out-Null }

# Adjust path to mysqldump if MySQL is not in PATH
$MySqlDump = "mysqldump"

# Credentials are read from ~\.my.cnf (see section 3 below)
& $MySqlDump --all-databases | Out-File -FilePath $DumpPath -Encoding utf8

# Retention policy: delete backups older than 7 days
Get-ChildItem "$BackupDir\all_databases_*.sql" |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-7) } |
    Remove-Item -Force
```

### 2. Schedule via Task Scheduler

Run this once in an elevated PowerShell session to register the daily 2 AM task:

```powershell
$action  = New-ScheduledTaskAction -Execute "powershell.exe" `
               -Argument "-NonInteractive -ExecutionPolicy Bypass -File C:\scripts\mysql_backup.ps1"
$trigger = New-ScheduledTaskTrigger -Daily -At "02:00"
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable
Register-ScheduledTask -TaskName "MySQLBackup" `
    -Action $action -Trigger $trigger -Settings $settings `
    -RunLevel Highest -Force
```

Verify the task was registered:

```powershell
Get-ScheduledTask -TaskName "MySQLBackup"
```

### 3. Credentials

MySQL credentials are read from `%USERPROFILE%\.my.cnf` (MySQL respects this on Windows too):

```ini
[client]
user=root
password=YOUR_PASSWORD
```

Restrict permissions on this file so only your user can read it.

### 4. Manual Verification

```powershell
# Run the backup manually
powershell -ExecutionPolicy Bypass -File C:\scripts\mysql_backup.ps1

# Check Task Scheduler log
Get-ScheduledTaskInfo -TaskName "MySQLBackup"
```

Backup files land in `%USERPROFILE%\mysql_backups\`.

### 5. Troubleshooting (Windows)

- **`mysqldump` not found**: Add `C:\Program Files\MySQL\MySQL Server X.X\bin` to your `PATH` environment variable, or use the full path in the script.
- **Access denied**: Ensure the `.my.cnf` credentials are correct and the user has `LOCK TABLES` and `SELECT` privileges on all databases.
- **Task does not run**: Open Task Scheduler UI (`taskschd.msc`), find *MySQLBackup*, and check the *History* tab for error codes.

---

## MySQL Automated Backups (macOS)

The application uses a shell script and `launchd` to perform daily automated backups of the MySQL database.

### 1. Backup Script

The backup script should be located at:
`~/scripts/mysql_backup.sh`

#### Script Content (`mysql_backup.sh`):

```bash
#!/bin/bash
BACKUP_DIR="$HOME/mysql_backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
# Update path to mysqldump if necessary
MYSQLDUMP="/opt/homebrew/opt/mysql-client/bin/mysqldump"

mkdir -p "$BACKUP_DIR"
"$MYSQLDUMP" --all-databases > "$BACKUP_DIR/all_databases_$TIMESTAMP.sql"

# Retention Policy: Automatically deletes backups older than 7 days.
find "$BACKUP_DIR" -type f -name "all_databases_*.sql" -mtime +7 -delete
```

Ensure the script is executable:

```bash
chmod +x ~/scripts/mysql_backup.sh
```

### 2. Schedule

The backup is scheduled via a `launchd` agent.

- **Plist Location:** `~/Library/LaunchAgents/com.mysql.backup.plist`
- **Frequency:** Daily at **2:00 AM**.

#### Plist Content (`com.mysql.backup.plist`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.mysql.backup</string>
    <key>ProgramArguments</key>
    <array>
        <string>/bin/bash</string>
        <string>/Users/yourusername/scripts/mysql_backup.sh</string>
    </array>
    <key>StartCalendarInterval</key>
    <dict>
        <key>Hour</key>
        <integer>2</integer>
        <key>Minute</key>
        <integer>0</integer>
    </dict>
    <key>StandardOutPath</key>
    <string>/Users/yourusername/mysql_backups/backup.log</string>
    <key>StandardErrorPath</key>
    <string>/Users/yourusername/mysql_backups/backup.err</string>
</dict>
</plist>
```

*Note: Replace `/Users/yourusername/` with your actual home directory path in the plist file, as `launchd` requires absolute paths.*

To load the agent:

```bash
launchctl load ~/Library/LaunchAgents/com.mysql.backup.plist
```

### 3. Credentials

Credentials for the backup are securely stored in the user's home directory:
`~/.my.cnf`

```ini
[client]
user=root
password=YOUR_PASSWORD
```

### 4. Manual Verification

To manually run the backup or verify the schedule status:

```bash
# Run the script manually
~/scripts/mysql_backup.sh

# Check the launchd status
launchctl list | grep com.mysql.backup
```

### 5. Troubleshooting

Logs for the automated backups can be found in the backup directory:
- Output log: `~/mysql_backups/backup.log`
- Error log: `~/mysql_backups/backup.err`

---

## MySQL Automated Backups (Windows)

The Windows equivalent uses a PowerShell script and **Task Scheduler** in place of the shell script and `launchd`.

### 1. Prerequisites

Install the MySQL client tools so `mysqldump.exe` is available. The easiest way is via [MySQL Installer for Windows](https://dev.mysql.com/downloads/installer/) — install **MySQL Shell** or **MySQL Client** only (no need for the full server).

After installation, `mysqldump.exe` is typically at:
`C:\Program Files\MySQL\MySQL Server 8.x\bin\mysqldump.exe`

### 2. Credentials File

Store credentials so the script does not hardcode your password. Create `%USERPROFILE%\.my.cnf`:

```ini
[client]
user=YOUR_RAILWAY_MYSQL_USER
password=YOUR_RAILWAY_MYSQL_PASSWORD
host=YOUR_RAILWAY_MYSQL_HOST
port=YOUR_RAILWAY_MYSQL_PORT
```

Get these values from your Railway MySQL service dashboard under **Connect**.

### 3. Backup Script

Create the script at `%USERPROFILE%\scripts\mysql_backup.ps1`:

```powershell
$BackupDir = "$env:USERPROFILE\mysql_backups"
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$MysqlDump = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe"
$LogFile   = "$BackupDir\backup.log"

# Create backup directory if it doesn't exist
New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null

# Run the dump (credentials read from %USERPROFILE%\.my.cnf)
$OutputFile = "$BackupDir\all_databases_$Timestamp.sql"
& $MysqlDump --defaults-file="$env:USERPROFILE\.my.cnf" --all-databases > $OutputFile 2>> $LogFile

if ($LASTEXITCODE -eq 0) {
    Add-Content $LogFile "[$Timestamp] Backup succeeded: $OutputFile"
} else {
    Add-Content $LogFile "[$Timestamp] Backup FAILED. Check log above for details."
}

# Retention policy: delete backups older than 7 days
Get-ChildItem -Path $BackupDir -Filter "all_databases_*.sql" |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-7) } |
    Remove-Item -Force
```

*Adjust `$MysqlDump` path if your MySQL version differs.*

### 4. Schedule via Task Scheduler

Run the following in an **elevated** PowerShell prompt (Run as Administrator) to register the daily 2 AM task:

```powershell
$Action  = New-ScheduledTaskAction `
    -Execute "powershell.exe" `
    -Argument "-NonInteractive -NoProfile -ExecutionPolicy Bypass -File `"$env:USERPROFILE\scripts\mysql_backup.ps1`""

$Trigger = New-ScheduledTaskTrigger -Daily -At "02:00"

$Settings = New-ScheduledTaskSettingsSet `
    -ExecutionTimeLimit (New-TimeSpan -Hours 1) `
    -StartWhenAvailable  # runs at next opportunity if machine was off at 2 AM

Register-ScheduledTask `
    -TaskName "MySQLBackup" `
    -Action   $Action `
    -Trigger  $Trigger `
    -Settings $Settings `
    -RunLevel Highest `
    -Description "Daily Railway MySQL backup to local machine"
```

`-StartWhenAvailable` is important — if your laptop is asleep at 2 AM, the task will run the next time it wakes up.

### 5. Manual Verification

```powershell
# Run the script manually to test
& "$env:USERPROFILE\scripts\mysql_backup.ps1"

# Check Task Scheduler status
Get-ScheduledTask -TaskName "MySQLBackup" | Get-ScheduledTaskInfo

# View recent log
Get-Content "$env:USERPROFILE\mysql_backups\backup.log" -Tail 20
```

### 6. Troubleshooting

- **`mysqldump` not found:** Verify the path in `$MysqlDump` matches your MySQL installation.
- **Access denied errors:** Double-check the credentials in `%USERPROFILE%\.my.cnf` match your Railway connection details exactly.
- **Task not running:** Open Task Scheduler UI (`taskschd.msc`), find `MySQLBackup`, and check the **History** tab for error codes.
- **Railway SSL requirement:** Railway MySQL may require SSL. If you get SSL errors, add `--ssl-mode=REQUIRED` to the `mysqldump` arguments.

