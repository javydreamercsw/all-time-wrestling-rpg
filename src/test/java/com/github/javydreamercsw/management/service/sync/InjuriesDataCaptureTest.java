package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Test to capture real injury data from Notion to understand the actual structure. */
@SpringBootTest(properties = {"notion.sync.enabled=true", "notion.sync.entities.injuries=true"})
@ActiveProfiles("test")
class InjuriesDataCaptureTest {

  @Autowired private NotionSyncService notionSyncService;

  @Test
  @EnabledIf("isNotionTokenAvailable")
  void captureRealInjuryDataFromNotion() {
    System.out.println("🔍 Attempting to capture real injury data from Notion...");

    try {
      // Try to sync injuries to see what the real data looks like
      var result = notionSyncService.syncInjuries("injury-data-capture-test");

      System.out.println("✅ Injury sync result: " + (result.isSuccess() ? "SUCCESS" : "FAILED"));
      System.out.println("📊 Injuries processed: " + result.getSyncedCount());

      if (!result.isSuccess()) {
        System.out.println("❌ Error: " + result.getErrorMessage());
      }

    } catch (Exception e) {
      System.out.println("❌ Exception during injury sync: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** Condition method to check if Notion token is available. */
  static boolean isNotionTokenAvailable() {
    return EnvironmentVariableUtil.isNotionTokenAvailable();
  }
}
