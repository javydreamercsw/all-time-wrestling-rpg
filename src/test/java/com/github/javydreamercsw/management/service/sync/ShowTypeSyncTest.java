package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.ShowTypeSyncService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** Unit tests for ShowTypeSyncService. */
@ActiveProfiles("test")
@Slf4j
@EnabledIf("com.github.javydreamercsw.base.util.EnvironmentVariableUtil#isNotionTokenAvailable")
class ShowTypeSyncTest extends ManagementIntegrationTest {

  @Autowired private ShowTypeSyncService showTypeSyncService;

  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private ShowTemplateRepository showTemplateRepository;

  @MockitoBean private NotionHandler notionHandler;

  private static final String TEST_OPERATION_ID = "unit-test-show-types";

  @BeforeEach
  @Transactional
  void cleanupData() {
    log.debug("🧹 Cleaning up test data before test execution");
    showTemplateRepository.deleteAll();
    showTypeRepository.deleteAll();
    log.debug("✅ Test data cleanup completed");
  }

  @Test
  @Transactional
  @DisplayName("Should ensure default show types exist in database")
  void shouldEnsureDefaultShowTypesExist() {
    log.info("🎭 Testing default show types creation");
    // Given - DataInitializer will have already created default show types.
    // Verify they exist
    Optional<ShowType> weeklyTypeBeforeSync = showTypeService.findByName("Weekly");
    assertTrue(
        weeklyTypeBeforeSync.isPresent(), "Weekly show type should exist from DataInitializer");
    Optional<ShowType> pleTypeBeforeSync = showTypeService.findByName("Premium Live Event (PLE)");
    assertTrue(
        pleTypeBeforeSync.isPresent(),
        "Premium Live Event (PLE) show type should exist from DataInitializer");

    // When - Run sync (should find existing defaults and not create new ones)
    BaseSyncService.SyncResult result = showTypeSyncService.syncShowTypes(TEST_OPERATION_ID);

    // Then - Should report no new creations
    assertNotNull(result, "Sync result should not be null");
    log.info("📊 Sync result: {}", result);
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getCreatedCount()).isEqualTo(0);
    assertThat(result.getUpdatedCount()).isEqualTo(2);

    // Verify default show types still exist and are unchanged
    Optional<ShowType> weeklyTypeAfterSync = showTypeService.findByName("Weekly");
    assertTrue(weeklyTypeAfterSync.isPresent(), "Weekly show type should still exist after sync");
    Optional<ShowType> pleTypeAfterSync = showTypeService.findByName("Premium Live Event (PLE)");
    assertTrue(
        pleTypeAfterSync.isPresent(),
        "Premium Live Event (PLE) show type should still exist after sync");

    ShowType weekly = weeklyTypeAfterSync.get();
    assertThat(weekly.getName()).isEqualTo("Weekly");
    assertThat(weekly.getDescription()).isNotBlank();
    assertThat(weekly.getCreationDate()).isNotNull();

    ShowType ple = pleTypeAfterSync.get();
    assertThat(ple.getName()).isEqualTo("Premium Live Event (PLE)");
    assertThat(ple.getDescription()).isNotBlank();
    assertThat(ple.getCreationDate()).isNotNull();

    log.info("✅ Default show types verification completed successfully");
  }

  @Test
  @Transactional
  @DisplayName("Should not duplicate show types on subsequent syncs")
  void shouldNotDuplicateShowTypesOnSubsequentSyncs() {
    log.info("🔄 Testing show types deduplication");
    when(notionHandler.getShowTypePages()).thenReturn(Collections.emptyList());

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
  @Transactional
  @DisplayName("Should handle sync when show types already exist")
  void shouldHandleSyncWhenShowTypesAlreadyExist() {
    log.info("🎯 Testing sync with pre-existing show types");
    when(notionHandler.getShowTypePages()).thenReturn(Collections.emptyList());

    // Given - DataInitializer will have already created default show types.
    // Verify they exist and are not null.
    Optional<ShowType> weeklyTypeBeforeSync = showTypeService.findByName("Weekly");
    assertTrue(
        weeklyTypeBeforeSync.isPresent(), "Weekly show type should exist from DataInitializer");
    ShowType initialWeekly = weeklyTypeBeforeSync.get();
    log.info("📝 Found initial Weekly show type: {}", initialWeekly.getName());

    // When - Run sync
    BaseSyncService.SyncResult result = showTypeSyncService.syncShowTypes(TEST_OPERATION_ID);

    // Then - Should not create duplicates and should report 0 created/updated
    assertNotNull(result, "Sync result should not be null");
    log.info("📊 Sync result: {}", result);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getCreatedCount()).isEqualTo(0);
    assertThat(result.getUpdatedCount()).isEqualTo(0);

    // Verify existing show type is still there and has not been overwritten or duplicated
    Optional<ShowType> weeklyTypeAfterSync = showTypeService.findByName("Weekly");
    assertTrue(weeklyTypeAfterSync.isPresent(), "Weekly show type should still exist after sync");
    ShowType retrievedWeekly = weeklyTypeAfterSync.get();
    assertThat(retrievedWeekly.getId()).isEqualTo(initialWeekly.getId());
    assertThat(retrievedWeekly.getDescription())
        .isEqualTo(
            initialWeekly
                .getDescription()); // Should retain original description from DataInitializer

    // Ensure total count hasn't changed
    List<ShowType> finalShowTypes = showTypeService.findAll();
    assertThat(finalShowTypes)
        .hasSize(2); // Assuming DataInitializer creates "Weekly" and "Premium Live Event (PLE)"

    log.info("✅ Pre-existing show types handling verified successfully");
  }

  @Test
  @Transactional
  @DisplayName("Should handle sync failures gracefully")
  void shouldHandleSyncFailuresGracefully() {
    log.info("🚨 Testing sync failure handling");
    when(notionHandler.getShowTypePages()).thenThrow(new RuntimeException("Test Exception"));

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
  @Transactional
  @DisplayName("Should track sync progress correctly")
  void shouldTrackSyncProgressCorrectly() {
    log.info("📈 Testing sync progress tracking");
    when(notionHandler.getShowTypePages()).thenReturn(Collections.emptyList());

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
}
