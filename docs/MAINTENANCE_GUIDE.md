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
