# Database Maintenance Guide

This document outlines the maintenance procedures for the All Time Wrestling RPG database.

## MySQL Automated Backups (macOS)

The application uses a shell script and `launchd` to perform daily automated backups of the MySQL database.

### 1. Backup Script

The backup script is located at:
`/Users/javierortiz/scripts/mysql_backup.sh`

It performs the following actions:
- Ensures the backup directory exists.
- Executes `mysqldump` for all databases.
- Stores backups in `/Users/javierortiz/mysql_backups`.
- **Retention Policy:** Automatically deletes backups older than 7 days.

### 2. Schedule

The backup is scheduled via a `launchd` agent:
- **Plist Location:** `/Users/javierortiz/Library/LaunchAgents/com.mysql.backup.plist`
- **Frequency:** Daily at **2:00 AM**.

### 3. Credentials

Credentials for the backup are securely stored in the user's home directory:
`~/.my.cnf`

```ini
[client]
user=root
password=********
```

### 4. Manual Verification

To manually run the backup or verify the schedule status:

```bash
# Run the script manually
/Users/javierortiz/scripts/mysql_backup.sh

# Check the launchd status
launchctl list | grep com.mysql.backup
```

### 5. Troubleshooting

Logs for the automated backups can be found in the backup directory:
- Output log: `/Users/javierortiz/mysql_backups/backup.log`
- Error log: `/Users/javierortiz/mysql_backups/backup.err`
