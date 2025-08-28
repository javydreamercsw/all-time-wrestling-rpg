package com.github.javydreamercsw.management.util;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;

/**
 * Utility to discover what databases actually exist in the Notion workspace. Run this to understand
 * the real structure before implementing sync features.
 */
public class NotionDatabaseDiscovery {

  public static void main(String[] args) {
    System.out.println("🔍 Discovering available databases in Notion workspace...");

    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      System.out.println(
          "❌ NOTION_TOKEN not available. Set it as environment variable or system property.");
      System.out.println("💡 Usage: java -DNOTION_TOKEN=xxx NotionDatabaseDiscovery");
      return;
    }

    try {
      // Get NotionHandler instance
      NotionHandler notionHandler = NotionHandler.getInstance();

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
      String[] injuryDatabases = {
        "Injuries",
        "Injury",
        "Wrestler Injuries",
        "Health",
        "Medical",
        "Injury Log",
        "Injury Tracker",
        "Injury Reports"
      };

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

      System.out.println("\n🎯 Summary:");
      System.out.println("- If 'Injuries' database exists, we can implement sync");
      System.out.println("- If not, we need to create it or skip the feature");
      System.out.println("- Use the found databases to create realistic sample data");

    } catch (Exception e) {
      System.out.println("❌ Failed to discover databases: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void testDatabase(NotionHandler notionHandler, String dbName) {
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
