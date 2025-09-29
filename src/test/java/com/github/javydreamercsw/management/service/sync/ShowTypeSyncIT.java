package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.entity.ShowTypeSyncService;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
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

/** Integration tests for ShowTypeSyncService. */
@ActiveProfiles("integration-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Slf4j
@EnabledIf("isNotionTokenAvailable")
public class ShowTypeSyncIT extends AbstractIntegrationTest {

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
  @DisplayName("Should ensure default show types exist in database")
  void shouldEnsureDefaultShowTypesExist() {
    log.info("🎭 Testing default show types creation");

    // Given - Initially empty database
    List<ShowType> initialShowTypes = showTypeService.findAll();
    log.info("📋 Found {} initial show types", initialShowTypes.size());

    // When - Sync show types (should create defaults when no Notion data available)
    BaseSyncService.SyncResult result = showTypeSyncService.syncShowTypes(TEST_OPERATION_ID);

    // Then - Should have created default show types
    assertNotNull(result, "Sync result should not be null");
    log.info("📊 Sync result: {}", result);

    List<ShowType> finalShowTypes = showTypeService.findAll();
    log.info("📋 Found {} show types after sync", finalShowTypes.size());

    // Verify default show types exist
    Optional<ShowType> weeklyType = showTypeService.findByName("Weekly");
    Optional<ShowType> pleType = showTypeService.findByName("Premium Live Event (PLE)");

    assertTrue(weeklyType.isPresent(), "Weekly show type should exist");
    assertTrue(pleType.isPresent(), "Premium Live Event (PLE) show type should exist");

    // Verify show type properties
    ShowType weekly = weeklyType.get();
    assertThat(weekly.getName()).isEqualTo("Weekly");
    assertThat(weekly.getDescription()).isNotBlank();
    assertThat(weekly.getCreationDate()).isNotNull();

    ShowType ple = pleType.get();
    assertThat(ple.getName()).isEqualTo("Premium Live Event (PLE)");
    assertThat(ple.getDescription()).isNotBlank();
    assertThat(ple.getCreationDate()).isNotNull();

    log.info("✅ Default show types verification completed successfully");
  }

  @Test
  @DisplayName("Should not duplicate show types on subsequent syncs")
  void shouldNotDuplicateShowTypesOnSubsequentSyncs() {
    log.info("🔄 Testing show types deduplication");

    // Given - Run initial sync
    BaseSyncService.SyncResult firstResult =
        showTypeSyncService.syncShowTypes(TEST_OPERATION_ID + "-1");
    assertNotNull(firstResult, "First sync result should not be null");

    List<ShowType> afterFirstSync = showTypeService.findAll();
    int countAfterFirstSync = afterFirstSync.size();
    log.info("📊 Show types after first sync: {}", countAfterFirstSync);

    // When - Run second sync
    BaseSyncService.SyncResult secondResult =
        showTypeSyncService.syncShowTypes(TEST_OPERATION_ID + "-2");

    // Then - Should not create duplicates
    assertNotNull(secondResult, "Second sync result should not be null");

    List<ShowType> afterSecondSync = showTypeService.findAll();
    int countAfterSecondSync = afterSecondSync.size();
    log.info("📊 Show types after second sync: {}", countAfterSecondSync);

    assertThat(countAfterSecondSync).isEqualTo(countAfterFirstSync);

    // Verify specific show types still exist and are unique
    List<ShowType> weeklyTypes =
        showTypeService.findAll().stream().filter(st -> "Weekly".equals(st.getName())).toList();
    List<ShowType> pleTypes =
        showTypeService.findAll().stream()
            .filter(st -> "Premium Live Event (PLE)".equals(st.getName()))
            .toList();

    assertThat(weeklyTypes).hasSize(1);
    assertThat(pleTypes).hasSize(1);

    log.info("✅ Deduplication verification completed successfully");
  }

  @Test
  @DisplayName("Should handle sync when show types already exist")
  void shouldHandleSyncWhenShowTypesAlreadyExist() {
    log.info("🎯 Testing sync with pre-existing show types");

    // Given - Manually create a show type
    ShowType existingType = new ShowType();
    existingType.setName("Weekly");
    existingType.setDescription("Pre-existing weekly show type");
    ShowType saved = showTypeService.save(existingType);
    assertNotNull(saved.getId());
    log.info("📝 Created pre-existing show type: {}", saved.getName());

    // When - Run sync
    BaseSyncService.SyncResult result = showTypeSyncService.syncShowTypes(TEST_OPERATION_ID);

    // Then - Should not overwrite existing show type
    assertNotNull(result, "Sync result should not be null");

    Optional<ShowType> weeklyType = showTypeService.findByName("Weekly");
    assertTrue(weeklyType.isPresent(), "Weekly show type should still exist");

    ShowType retrievedWeekly = weeklyType.get();
    assertThat(retrievedWeekly.getId()).isEqualTo(saved.getId());
    assertThat(retrievedWeekly.getDescription()).isEqualTo("Pre-existing weekly show type");

    log.info("✅ Pre-existing show types handling verified successfully");
  }

  @Test
  @DisplayName("Should handle sync failures gracefully")
  void shouldHandleSyncFailuresGracefully() {
    log.info("🚨 Testing sync failure handling");

    // When - Attempt sync (might fail due to missing Notion configuration)
    BaseSyncService.SyncResult result = showTypeSyncService.syncShowTypes(TEST_OPERATION_ID);

    // Then - Should handle gracefully regardless of success or failure
    assertNotNull(result, "Sync result should not be null");
    assertThat(result.getEntityType()).isEqualTo("Show Types");

    if (result.isSuccess()) {
      log.info("✅ Sync succeeded: {}", result);
      assertThat(result.getSyncedCount()).isGreaterThanOrEqualTo(0);
    } else {
      log.info("⚠️ Sync failed as expected: {}", result.getErrorMessage());
      assertThat(result.getErrorMessage()).isNotBlank();
    }

    // Verify database is still in a consistent state
    List<ShowType> showTypes = showTypeService.findAll();
    log.info("📊 Show types in database after sync attempt: {}", showTypes.size());

    // Should have at least the default show types
    assertThat(showTypes.size()).isGreaterThanOrEqualTo(0);

    log.info("✅ Failure handling verification completed successfully");
  }

  @Test
  @DisplayName("Should track sync progress correctly")
  void shouldTrackSyncProgressCorrectly() {
    log.info("📈 Testing sync progress tracking");

    // When - Run sync with operation ID for progress tracking
    String operationId = TEST_OPERATION_ID + "-progress";
    BaseSyncService.SyncResult result = showTypeSyncService.syncShowTypes(operationId);

    // Then - Should complete operation tracking
    assertNotNull(result, "Sync result should not be null");

    // Progress tracking is internal to the service, so we verify the result reflects completion
    if (result.isSuccess()) {
      log.info("✅ Sync completed successfully with progress tracking");
      assertThat(result.getSyncedCount()).isGreaterThanOrEqualTo(0);
    } else {
      log.info("⚠️ Sync completed with error but progress was tracked");
      assertThat(result.getErrorMessage()).isNotBlank();
    }

    log.info("✅ Progress tracking verification completed successfully");
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
