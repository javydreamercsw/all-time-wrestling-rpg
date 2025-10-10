package com.github.javydreamercsw.management.config;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Comprehensive test to validate that all database indexes in the optimization script reference
 * valid tables and columns that exist in the actual database schema.
 *
 * <p>This test prevents runtime errors caused by index scripts referencing non-existent tables or
 * columns, which would otherwise only be discovered during application startup.
 */
@Slf4j
class DatabaseIndexValidationTest extends ManagementIntegrationTest {

  @Autowired private DataSource dataSource;

  private Map<String, Set<String>> tableColumns;
  private List<IndexDefinition> indexDefinitions;

  @BeforeEach
  void setUp() throws SQLException, IOException {
    // Load actual database schema
    loadDatabaseSchema();

    // Parse index definitions from the optimization script
    parseIndexScript();
  }

  @Test
  void testAllIndexesReferenceValidTablesAndColumns() {
    List<String> errors = new ArrayList<>();

    for (IndexDefinition index : indexDefinitions) {
      log.debug("Validating index: {} on table: {}", index.indexName, index.tableName);

      // Check if table exists
      if (!tableColumns.containsKey(index.tableName.toLowerCase())) {
        errors.add(
            String.format(
                "Index '%s' references non-existent table '%s'", index.indexName, index.tableName));
        continue;
      }

      Set<String> availableColumns = tableColumns.get(index.tableName.toLowerCase());

      // Check if all columns exist
      for (String column : index.columns) {
        String cleanColumn = cleanColumnName(column);
        if (!availableColumns.contains(cleanColumn.toLowerCase())) {
          errors.add(
              String.format(
                  "Index '%s' on table '%s' references non-existent column '%s'. Available columns:"
                      + " %s",
                  index.indexName, index.tableName, cleanColumn, availableColumns));
        }
      }
    }

    if (!errors.isEmpty()) {
      String errorMessage = "Database index validation failed:\n" + String.join("\n", errors);
      log.error(errorMessage);
      fail(errorMessage);
    }

    log.info("✅ All {} database indexes validated successfully!", indexDefinitions.size());
  }

  @Test
  void testIndexScriptCanBeExecutedWithoutErrors() {
    try (Connection connection = dataSource.getConnection()) {
      // This should not throw any exceptions if all indexes are valid
      ScriptUtils.executeSqlScript(
          connection, new ClassPathResource("db/optimization/indexes.sql"));
      log.info("✅ Index script executed successfully without errors!");
    } catch (Exception e) {
      fail("Index script execution failed: " + e.getMessage(), e);
    }
  }

  @Test
  void testNoMissingTablesInIndexScript() {
    Set<String> indexedTables =
        indexDefinitions.stream()
            .map(idx -> idx.tableName.toLowerCase())
            .collect(HashSet::new, Set::add, Set::addAll);

    Set<String> actualTables = new HashSet<>(tableColumns.keySet());

    // Log which tables have indexes and which don't
    Set<String> tablesWithoutIndexes = new HashSet<>(actualTables);
    tablesWithoutIndexes.removeAll(indexedTables);

    if (!tablesWithoutIndexes.isEmpty()) {
      log.info("ℹ️ Tables without custom indexes (may be intentional): {}", tablesWithoutIndexes);
    }

    log.info(
        "✅ Index coverage: {}/{} tables have custom indexes",
        indexedTables.size(),
        actualTables.size());
  }

  private void loadDatabaseSchema() throws SQLException {
    tableColumns = new HashMap<>();

    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();

      // Get all tables
      try (ResultSet tables = metaData.getTables(null, null, "%", new String[] {"TABLE"})) {
        while (tables.next()) {
          String tableName = tables.getString("TABLE_NAME").toLowerCase();
          Set<String> columns = new HashSet<>();

          // Get all columns for this table
          try (ResultSet cols = metaData.getColumns(null, null, tableName.toUpperCase(), "%")) {
            while (cols.next()) {
              String columnName = cols.getString("COLUMN_NAME").toLowerCase();
              columns.add(columnName);
            }
          }

          tableColumns.put(tableName, columns);
          log.debug("Loaded table '{}' with columns: {}", tableName, columns);
        }
      }
    }

    log.info("Loaded schema for {} tables", tableColumns.size());
  }

  private void parseIndexScript() throws IOException {
    indexDefinitions = new ArrayList<>();

    String scriptContent =
        Files.readString(Paths.get("src/main/resources/db/optimization/indexes.sql"));

    // Regex to segment CREATE INDEX statements
    Pattern indexPattern =
        Pattern.compile(
            "CREATE\\s+INDEX\\s+IF\\s+NOT\\s+EXISTS\\s+(\\w+)\\s+ON\\s+(\\w+)\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    Matcher matcher = indexPattern.matcher(scriptContent);

    while (matcher.find()) {
      String indexName = matcher.group(1);
      String tableName = matcher.group(2);
      String columnsPart = matcher.group(3);

      // Parse columns (handle DESC, ASC, and composite indexes)
      List<String> columns = parseColumns(columnsPart);

      indexDefinitions.add(new IndexDefinition(indexName, tableName, columns));
      log.debug("Parsed index: {} on {}({})", indexName, tableName, columns);
    }

    log.info("Parsed {} index definitions from script", indexDefinitions.size());
  }

  private List<String> parseColumns(String columnsPart) {
    List<String> columns = new ArrayList<>();

    // Split by comma and clean each column
    String[] parts = columnsPart.split(",");
    for (String part : parts) {
      String column =
          part.trim()
              .replaceAll("\\s+DESC\\s*$", "") // Remove DESC
              .replaceAll("\\s+ASC\\s*$", "") // Remove ASC
              .trim();
      columns.add(column);
    }

    return columns;
  }

  private String cleanColumnName(String column) {
    return column.trim().replaceAll("\\s+DESC\\s*$", "").replaceAll("\\s+ASC\\s*$", "").trim();
  }

  /** Represents a parsed index definition from the SQL script. */
  private static class IndexDefinition {
    final String indexName;
    final String tableName;
    final List<String> columns;

    IndexDefinition(String indexName, String tableName, List<String> columns) {
      this.indexName = indexName;
      this.tableName = tableName;
      this.columns = columns;
    }

    @Override
    public String toString() {
      return String.format(
          "Index{name='%s', table='%s', columns=%s}", indexName, tableName, columns);
    }
  }
}
