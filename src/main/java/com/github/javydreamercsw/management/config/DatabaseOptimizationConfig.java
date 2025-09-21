package com.github.javydreamercsw.management.config;

import java.sql.Connection;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Database optimization configuration that creates indexes and optimizes database performance. This
 * configuration runs after the application is ready to ensure all tables are created.
 */
@Configuration
@Slf4j
@Order(100)
@DependsOn("flywayCommandLineRunner")
public class DatabaseOptimizationConfig implements ApplicationRunner {

  private final DataSource dataSource;
  private final JdbcTemplate jdbcTemplate;

  public DatabaseOptimizationConfig(DataSource dataSource, JdbcTemplate jdbcTemplate) {
    this.dataSource = dataSource;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    // Run all post-startup tasks
    createDatabaseIndexes();
    optimizeConnectionSettings();
    analyzeQueryPerformance();
  }

  /**
   * Creates database indexes after the application is ready. This ensures all JPA entities have
   * been processed and tables created.
   */
  public void createDatabaseIndexes() {
    log.info("🚀 Starting database optimization - creating indexes...");

    try (Connection connection = dataSource.getConnection()) {
      // Execute the database optimization script
      ScriptUtils.executeSqlScript(
          connection, new ClassPathResource("db/optimization/indexes.sql"));

      log.info("✅ Database indexes created successfully");

      // Log current database statistics
      logDatabaseStatistics();

    } catch (Exception e) {
      log.warn("⚠️ Failed to create some database indexes: {}", e.getMessage());
      log.debug("Index creation error details", e);
    }
  }

  /** Logs database statistics for monitoring performance. */
  private void logDatabaseStatistics() {
    try {
      // Get table counts for key entities
      Integer wrestlerCount =
          jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wrestler", Integer.class);
      Integer showCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM show", Integer.class);
      Integer rivalryCount =
          jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rivalry", Integer.class);
      Integer injuryTypeCount =
          jdbcTemplate.queryForObject("SELECT COUNT(*) FROM injury_type", Integer.class);
      Integer factionCount =
          jdbcTemplate.queryForObject("SELECT COUNT(*) FROM faction", Integer.class);
      Integer showTemplateCount =
          jdbcTemplate.queryForObject("SELECT COUNT(*) FROM show_template", Integer.class);
      Integer deckCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM deck", Integer.class);
      Integer cardCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM card", Integer.class);
      Integer segmentCount =
          jdbcTemplate.queryForObject("SELECT COUNT(*) FROM segment", Integer.class);

      log.info("📊 Database Statistics:");
      log.info("   - Wrestlers: {}", wrestlerCount);
      log.info("   - Shows: {}", showCount);
      log.info("   - Rivalries: {}", rivalryCount);
      log.info("   - Injury Types: {}", injuryTypeCount);
      log.info("   - Factions: {}", factionCount);
      log.info("   - Show Templates: {}", showTemplateCount);
      log.info("   - Decks: {}", deckCount);
      log.info("   - Cards: {}", cardCount);
      log.info("   - Segments: {}", segmentCount);

      // Check if indexes exist (H2 specific query)
      try {
        Integer indexCount =
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_SCHEMA = 'PUBLIC'",
                Integer.class);
        log.info("   - Database Indexes: {}", indexCount);
      } catch (Exception e) {
        log.debug("Could not retrieve index count: {}", e.getMessage());
      }

    } catch (Exception e) {
      log.debug("Could not retrieve database statistics: {}", e.getMessage());
    }
  }

  /**
   * Analyzes query performance and suggests optimizations. This method can be called periodically
   * to monitor performance.
   */
  public void analyzeQueryPerformance() {
    log.info("🔍 Analyzing query performance...");

    try {
      // Check for slow queries (H2 doesn't have built-in slow query log, but we can check table
      // sizes)
      checkLargeTablePerformance();

      // Suggest optimizations based on data patterns
      suggestOptimizations();

    } catch (Exception e) {
      log.warn("Failed to analyze query performance: {}", e.getMessage());
    }
  }

  private void checkLargeTablePerformance() {
    try {
      // Check tables that might benefit from additional indexes
      String[] largeTables = {"wrestler", "show", "rivalry", "injury", "multi_wrestler_feud"};

      for (String table : largeTables) {
        try {
          Integer count =
              jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);

          if (count != null && count > 1000) {
            log.info("⚡ Large table detected: {} ({} rows) - ensure proper indexing", table, count);
          }
        } catch (Exception e) {
          log.debug("Could not check table size for {}: {}", table, e.getMessage());
        }
      }
    } catch (Exception e) {
      log.debug("Error checking large table performance: {}", e.getMessage());
    }
  }

  private void suggestOptimizations() {
    log.info("💡 Performance Optimization Suggestions:");
    log.info("   - Enable query caching for frequently accessed data");
    log.info("   - Consider read replicas for heavy read workloads");
    log.info("   - Monitor slow queries and add indexes as needed");
    log.info("   - Use pagination for large result sets");
    log.info("   - Implement connection pooling for high concurrency");
  }

  /** Optimizes database connection settings for better performance. */
  public void optimizeConnectionSettings() {
    try {
      // Set optimal H2 database settings for performance
      jdbcTemplate.execute("SET DB_CLOSE_DELAY -1"); // Keep database open
      jdbcTemplate.execute("SET CACHE_SIZE 65536"); // 64MB cache
      jdbcTemplate.execute("SET LOCK_TIMEOUT 10000"); // 10 second lock timeout

      log.info("✅ Database connection settings optimized");

    } catch (Exception e) {
      log.debug("Could not optimize connection settings: {}", e.getMessage());
    }
  }
}
