# Notion Sync Troubleshooting Guide

## Overview

This guide helps diagnose and resolve common Notion synchronization issues in the All Time Wrestling RPG system.

## Common Sync Issues and Solutions

### 1. NOTION_TOKEN Issues

**Symptoms:**
- "NOTION_TOKEN not available" errors
- Sync operations fail immediately
- 401 Unauthorized errors

**Solutions:**

```bash
# Check if token is set
echo $NOTION_TOKEN

# Set token for current session
export NOTION_TOKEN=your_token_here

# For permanent setup, add to ~/.bashrc or ~/.zshrc
echo 'export NOTION_TOKEN=your_token_here' >> ~/.bashrc
```

**Verification:**

```bash
# Test token validity
curl -H "Authorization: Bearer $NOTION_TOKEN" \
	-H "Notion-Version: 2022-06-28" \
	https://api.notion.com/v1/users/me
```

### 2. Network Connectivity Issues

**Symptoms:**
- Connection timeout errors
- Intermittent sync failures
- "Failed to synchronize after Xms" messages

**Solutions:**
- Check internet connectivity
- Verify firewall settings allow HTTPS to api.notion.com
- Consider proxy configuration if behind corporate firewall

**Network Test:**

```bash
# Test Notion API connectivity
curl -I https://api.notion.com/v1/users/me
```

### 3. Rate Limiting Issues

**Symptoms:**
- 429 Too Many Requests errors
- Sync operations slow down or fail
- Intermittent success/failure patterns

**Solutions:**
- The system includes built-in rate limiting
- Reduce sync frequency in configuration
- Avoid concurrent sync operations

**Configuration:**

```yaml
notion:
sync:
	scheduler:
	interval: 3600000  # Increase interval (1 hour)
```

### 4. Database Connection Issues

**Symptoms:**
- "Database connection failed" errors
- Sync retrieves data but fails to save
- Transaction rollback messages

**Solutions:**
- Check database connectivity
- Verify sufficient database connections available
- Check disk space for H2 database files

**Database Health Check:**

```bash
# Check H2 database file
ls -la data/management-db.mv.db

# Check disk space
df -h
```

### 5. Data Mapping Issues

**Symptoms:**
- "Failed to convert show page to DTO" errors
- Partial sync success with some items skipped
- Data validation errors

**Solutions:**
- Check Notion database schema matches expected format
- Verify required properties exist in Notion pages
- Review error logs for specific mapping failures

## Sync Monitoring and Diagnostics

### 1. Enable Debug Logging

Add to `application.properties`:

```properties
# Enable detailed sync logging
logging.level.com.github.javydreamercsw.management.service.sync=DEBUG
logging.level.com.github.javydreamercsw.base.ai.notion=DEBUG
```

### 2. Monitor Sync Progress

**Via UI:**
- Navigate to Sync Management view
- Monitor real-time progress and logs
- Check sync history and results

**Via API:**

```bash
# Trigger manual sync
curl -X POST http://localhost:8080/api/sync/notion/trigger

# Check specific entity sync
curl -X POST http://localhost:8080/api/sync/notion/trigger/shows
```

### 3. Log Analysis

**Key Log Patterns:**

```bash
# Successful sync
grep "✅.*sync completed" logs/application.log

# Failed syncs
grep "❌.*sync failed" logs/application.log

# Rate limiting
grep "429" logs/application.log

# Connection issues
grep "timeout\|connection" logs/application.log
```

## Performance Optimization

### 1. Sync Configuration Tuning

```yaml
notion:
sync:
	enabled: true
	entities: ["shows"]  # Sync only needed entities
	scheduler:
	enabled: true
	interval: 7200000  # 2 hours
	backup:
	enabled: false     # Disable if not needed
```

### 2. Database Optimization

- Ensure adequate heap memory: `-Xmx2g`
- Monitor database file size growth
- Consider periodic database maintenance

### 3. Network Optimization

- Use stable internet connection
- Consider running sync during off-peak hours
- Monitor for network interruptions

## Error Recovery Procedures

### 1. Stuck Sync Operations

```bash
# Restart application to clear stuck operations
./mvnw spring-boot:stop
./mvnw spring-boot:start

# Or kill specific processes
ps aux | grep java
kill -9 <process_id>
```

### 2. Corrupted Sync State

```bash
# Clear sync progress tracking (if needed)
# This resets all active sync operations
rm -f data/sync-progress.tmp
```

### 3. Database Recovery

```bash
# Backup current database
cp data/management-db.mv.db data/management-db.backup

# If corruption suspected, let H2 rebuild indexes
rm data/management-db.trace.db
```

## Preventive Measures

### 1. Regular Monitoring

- Set up log monitoring for sync failures
- Monitor sync success rates
- Track sync performance metrics

### 2. Configuration Management

- Use environment-specific configurations
- Validate NOTION_TOKEN before deployment
- Test sync operations in staging environment

### 3. Backup Strategy

- Enable sync backups for critical data
- Regular database backups
- Document recovery procedures

## Support and Debugging

### 1. Collect Diagnostic Information

```bash
# System information
java -version
echo $NOTION_TOKEN | cut -c1-10  # First 10 chars only

# Application logs
tail -n 100 logs/application.log | grep -E "(sync|notion|error)"

# Database status
ls -la data/
```

### 2. Test Sync Components

```bash
# Test individual sync operations
curl -X POST http://localhost:8080/api/sync/notion/shows
curl -X POST http://localhost:8080/api/sync/notion/wrestlers
curl -X POST http://localhost:8080/api/sync/notion/factions
```

### 3. Validate Configuration

```bash
# Check sync configuration
curl http://localhost:8080/api/sync/notion/status
```

## Common Resolution Steps

1. **Check NOTION_TOKEN** - Most common issue
2. **Verify network connectivity** - Test API access
3. **Review error logs** - Look for specific error patterns
4. **Restart sync operations** - Clear stuck states
5. **Validate data mapping** - Check Notion schema
6. **Monitor resource usage** - Memory, disk, network
7. **Test individual components** - Isolate problem areas

## Contact and Support

For persistent issues:
1. Collect diagnostic information above
2. Check GitHub issues for similar problems
3. Review application logs for detailed error messages
4. Test with minimal configuration to isolate issues
