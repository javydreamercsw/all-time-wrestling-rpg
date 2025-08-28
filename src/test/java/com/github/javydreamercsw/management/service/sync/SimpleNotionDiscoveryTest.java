package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.base.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Simple test to discover what databases actually exist in the Notion workspace. This test doesn't
 * depend on NotionSyncService constructor.
 */
@Slf4j
class SimpleNotionDiscoveryTest extends BaseTest {

  @Test
  @EnabledIf("isNotionTokenAvailable")
  void discoverNotionDatabases() {
    System.out.println("🔍 Discovering available databases in Notion workspace...");

    try {
      // Get NotionHandler instance
      var notionHandler = com.github.javydreamercsw.base.ai.notion.NotionHandler.getInstance();

      // Test known databases that we know exist
      String[] knownDatabases = {
        "Matches",
        "Shows",
        "Wrestlers",
        "Factions",
        "Teams",
        "Championships",
        "Seasons",
        "Show Types"
      };

      // Test potential injury-related database names
      String[] injuryDatabases = {"Injuries", "Injury", "Wrestler Injuries", "Health", "Medical"};

      System.out.println("\n📋 Testing known existing databases:");
      for (String dbName : knownDatabases) {
        testDatabase(notionHandler, dbName);
      }

      System.out.println("\n🏥 Testing potential injury databases:");
      for (String dbName : injuryDatabases) {
        testDatabase(notionHandler, dbName);
      }

      // Try to load some actual data to see structure
      System.out.println("\n📊 Testing data loading from known databases:");
      try {
        var matches = notionHandler.loadAllMatches();
        System.out.println("✅ Matches: Found " + matches.size() + " matches");
        if (!matches.isEmpty()) {
          var firstMatch = matches.get(0);
          System.out.println(
              "   Sample match properties: " + firstMatch.getRawProperties().keySet());
        }
      } catch (Exception e) {
        System.out.println("❌ Failed to load matches: " + e.getMessage());
      }

    } catch (Exception e) {
      System.out.println("❌ Failed to discover databases: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void testDatabase(Object notionHandler, String dbName) {
    try {
      // Use reflection to access the private getDatabaseId method
      java.lang.reflect.Method method =
          notionHandler.getClass().getDeclaredMethod("getDatabaseId", String.class);
      method.setAccessible(true);
      String dbId = (String) method.invoke(notionHandler, dbName);

      if (dbId != null) {
        System.out.println("✅ Found: " + dbName + " (ID: " + dbId + ")");
      } else {
        System.out.println("❌ Not found: " + dbName);
      }
    } catch (Exception e) {
      System.out.println("❓ Error checking " + dbName + ": " + e.getMessage());
    }
  }
}
