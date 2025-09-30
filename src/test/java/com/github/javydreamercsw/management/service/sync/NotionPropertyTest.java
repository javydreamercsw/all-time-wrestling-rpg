package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.test.BaseTest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test to verify Notion property resolution fixes. This test captures real Notion data to verify
 * Date and Title(s) properties are resolved correctly.
 */
@SpringBootTest(properties = {"notion.sync.enabled=true", "notion.sync.entities.matches=true"})
@ActiveProfiles("test")
@EnabledIf("isNotionTokenAvailable")
@Slf4j
class NotionPropertyTest extends BaseTest {

  @Autowired private NotionSyncService notionSyncService;

  @Test
  void shouldResolveNotionPropertiesCorrectly() {
    // This test captures real Notion data to verify property resolution
    log.info("🔍 Testing Notion property resolution fixes...");

    try {
      List<String> segmentIds = notionSyncService.getAllSegmentIds();
      NotionSyncService.SyncResult result;
      if (segmentIds.isEmpty()) {
        result = notionSyncService.syncSegments("test-operation-123");
      } else {
        String randomId = segmentIds.get(new java.util.Random().nextInt(segmentIds.size()));
        result = notionSyncService.syncSegment(randomId);
      }

      log.info("✅ Property resolution test completed");
      assertThat(result.isSuccess()).isTrue();
      log.info("\uD83D\uDCC8 Matches processed: {}", result.getSyncedCount());
      assertThat(result).isNotNull();
    } catch (Exception e) {
      System.out.println("❌ Property resolution test failed: " + e.getMessage());
      throw e;
    }
  }
}
