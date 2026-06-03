# Database Maintenance Guide

This document outlines the maintenance procedures for the All Time Wrestling RPG database.

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
