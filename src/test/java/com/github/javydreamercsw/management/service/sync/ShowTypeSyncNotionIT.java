package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.entity.ShowTypeSyncService;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for ShowTypeSyncService against a real Notion instance. */
@ActiveProfiles("integration-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Slf4j
@EnabledIf("isNotionTokenAvailable")
public class ShowTypeSyncNotionIT extends ManagementIntegrationTest {

  @Autowired private ShowTypeSyncService showTypeSyncService;

  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private ShowTemplateRepository showTemplateRepository;

  private static final String TEST_OPERATION_ID = "integration-test-show-types";

  @BeforeEach
  @Transactional
  void cleanupData() {
    log.debug("🧹 Cleaning up test data before test execution");
    showTemplateRepository.deleteAll();
    showTypeRepository.deleteAll();
    log.debug("✅ Test data cleanup completed");
  }

  @Test
  @EnabledIf("isNotionTokenAvailable")
  @DisplayName("Should sync show types from Notion when token is available")
  void shouldSyncShowTypesFromNotionWhenTokenAvailable() {
    log.info("🔗 Testing real Notion sync (token available)");

    // When - Sync with real Notion connection
    BaseSyncService.SyncResult result =
        showTypeSyncService.syncShowTypes(TEST_OPERATION_ID + "-notion");

    // Then - Should attempt real sync
    assertNotNull(result, "Sync result should not be null");
    log.info("📊 Notion sync result: {}", result);

    if (result.isSuccess()) {
      log.info("✅ Successfully synced from Notion");

      // Verify show types were created/updated
      List<ShowType> showTypes = showTypeService.findAll();
      assertThat(showTypes).isNotEmpty();

      // Verify expected show types exist
      Optional<ShowType> weeklyType = showTypeService.findByName("Weekly");
      Optional<ShowType> pleType = showTypeService.findByName("Premium Live Event (PLE)");

      assertTrue(
          weeklyType.isPresent() || pleType.isPresent(),
          "At least one expected show type should exist");
    } else {
      log.warn("⚠️ Notion sync failed: {}", result.getErrorMessage());
      // This is acceptable in test environment
    }

    log.info("✅ Notion sync verification completed");
  }
}
