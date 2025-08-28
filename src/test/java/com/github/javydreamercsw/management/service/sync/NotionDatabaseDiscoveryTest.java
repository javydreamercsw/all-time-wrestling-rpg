package com.github.javydreamercsw.management.service.sync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/** Test to discover what databases actually exist in the Notion workspace. */
class NotionDatabaseDiscoveryTest extends BaseSyncTest {

  @Test
  @EnabledIf("isNotionTokenAvailable")
  void discoverAvailableDatabases() {
    System.out.println("🔍 Discovering available databases in Notion workspace...");

    try {
      // Try to access NotionHandler to see what databases are available
      var notionHandler = com.github.javydreamercsw.base.ai.notion.NotionHandler.getInstance();

      // Try to get database IDs for known entities
      String[] knownDatabases = {
        "Matches",
        "Shows",
        "Wrestlers",
        "Factions",
        "Teams",
        "Injuries",
        "Championships",
        "Seasons",
        "Show Types"
      };

      for (String dbName : knownDatabases) {
        try {
          // Use reflection to access the private getDatabaseId method
          java.lang.reflect.Method method =
              notionHandler.getClass().getDeclaredMethod("getDatabaseId", String.class);
          method.setAccessible(true);
          String dbId = (String) method.invoke(notionHandler, dbName);

          if (dbId != null) {
            System.out.println("✅ Found database: " + dbName + " (ID: " + dbId + ")");
          } else {
            System.out.println("❌ Database not found: " + dbName);
          }
        } catch (Exception e) {
          System.out.println("❓ Error checking database " + dbName + ": " + e.getMessage());
        }
      }

    } catch (Exception e) {
      System.out.println("❌ Failed to discover databases: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
