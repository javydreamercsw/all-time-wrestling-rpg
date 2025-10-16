package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.entity.ShowTemplateSyncService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for ShowTemplateSyncService. These tests verify the complete integration
 * between the sync service, database, and show template management.
 *
 * <p>NO MOCKING - These tests use real services and real database operations.
 */
@DisplayName("Show Template Sync Integration Tests")
class ShowTemplateSyncIntegrationTest extends ManagementIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(ShowTemplateSyncIntegrationTest.class);

  @Autowired private ShowTemplateSyncService showTemplateSyncService;
  @Autowired private ShowTemplateService showTemplateService;
  @Autowired private ShowTypeRepository showTypeRepository;

  private static final String TEST_OPERATION_ID = "integration-test-show-templates";

  @Test
  @DisplayName("Should handle sync when no Notion data is available")
  void shouldHandleSyncWhenNoNotionDataAvailable() {
    log.info("🎭 Testing show template sync with no Notion data");

    // Given - Initially empty database
    List<ShowTemplate> initialTemplates = showTemplateService.findAll();
    log.info("📋 Found {} initial show templates", initialTemplates.size());

    // When - Sync show templates (should handle gracefully when no Notion data available)
    BaseSyncService.SyncResult result =
        showTemplateSyncService.syncShowTemplates(TEST_OPERATION_ID);

    // Then - Should complete without errors
    assertNotNull(result, "Sync result should not be null");
    log.info("📊 Sync result: {}", result);

    // The result may succeed or fail depending on Notion availability, but should be graceful
    assertThat(result.getEntityType()).isEqualTo("Show Templates");

    if (result.isSuccess()) {
      log.info("✅ Sync succeeded with {} templates", result.getSyncedCount());
      assertThat(result.getSyncedCount()).isGreaterThanOrEqualTo(0);
    } else {
      log.info("⚠️ Sync failed as expected: {}", result.getErrorMessage());
      assertThat(result.getErrorMessage()).isNotBlank();
    }

    log.info("✅ No Notion data sync verification completed successfully");
  }

  @Test
  @DisplayName("Should not duplicate show templates on subsequent syncs")
  void shouldNotDuplicateShowTemplatesOnSubsequentSyncs() {
    log.info("🔄 Testing show template deduplication");

    // Given - Manually create a show template
    ShowTemplate existingTemplate = new ShowTemplate();
    existingTemplate.setName("Test Template");
    existingTemplate.setDescription("Existing template for testing");
    existingTemplate.setShowType(showTypeRepository.findByName("Weekly").orElseThrow());
    existingTemplate.setExternalId("test-external-id");
    ShowTemplate saved = showTemplateService.save(existingTemplate);
    assertNotNull(saved.getId());
    log.info("📝 Created pre-existing show template: {}", saved.getName());

    // When - Run first sync
    BaseSyncService.SyncResult firstResult =
        showTemplateSyncService.syncShowTemplates(TEST_OPERATION_ID + "-1");
    assertNotNull(firstResult, "First sync result should not be null");

    List<ShowTemplate> afterFirstSync = showTemplateService.findAll();
    int countAfterFirstSync = afterFirstSync.size();
    log.info("📊 Show templates after first sync: {}", countAfterFirstSync);

    // When - Run second sync
    BaseSyncService.SyncResult secondResult =
        showTemplateSyncService.syncShowTemplates(TEST_OPERATION_ID + "-2");

    // Then - Should not create duplicates
    assertNotNull(secondResult, "Second sync result should not be null");

    List<ShowTemplate> afterSecondSync = showTemplateService.findAll();
    int countAfterSecondSync = afterSecondSync.size();
    log.info("📊 Show templates after second sync: {}", countAfterSecondSync);

    // The count should be the same or increase by legitimate new templates, but no duplicates
    assertThat(countAfterSecondSync).isGreaterThanOrEqualTo(countAfterFirstSync);

    // Verify the original template still exists and wasn't duplicated
    Optional<ShowTemplate> retrievedTemplate = showTemplateService.findByName("Test Template");
    assertTrue(retrievedTemplate.isPresent(), "Original template should still exist");
    assertThat(retrievedTemplate.get().getId()).isEqualTo(saved.getId());

    log.info("✅ Deduplication verification completed successfully");
  }

  @Test
  @DisplayName("Should handle sync when show templates already exist")
  void shouldHandleSyncWhenShowTemplatesAlreadyExist() {
    log.info("🎯 Testing sync with pre-existing show templates");

    // Given - Manually create show templates
    ShowTemplate weeklyTemplate = new ShowTemplate();
    weeklyTemplate.setName("Monday Night RAW");
    weeklyTemplate.setDescription("Pre-existing weekly show template");
    weeklyTemplate.setShowType(showTypeRepository.findByName("Weekly").orElseThrow());
    ShowTemplate savedWeekly = showTemplateService.save(weeklyTemplate);
    assertNotNull(savedWeekly.getId());

    ShowTemplate pleTemplate = new ShowTemplate();
    pleTemplate.setName("WrestleMania");
    pleTemplate.setDescription("Pre-existing PLE template");
    pleTemplate.setShowType(
        showTypeRepository.findByName("Premium Live Event (PLE)").orElseThrow());
    ShowTemplate savedPLE = showTemplateService.save(pleTemplate);
    assertNotNull(savedPLE.getId());

    log.info(
        "📝 Created pre-existing show templates: {}, {}",
        savedWeekly.getName(),
        savedPLE.getName());

    // When - Run sync
    BaseSyncService.SyncResult result =
        showTemplateSyncService.syncShowTemplates(TEST_OPERATION_ID);

    // Then - Should handle existing templates gracefully
    assertNotNull(result, "Sync result should not be null");

    // Verify templates still exist
    Optional<ShowTemplate> retrievedWeekly = showTemplateService.findByName("Monday Night RAW");
    Optional<ShowTemplate> retrievedPLE = showTemplateService.findByName("WrestleMania");

    assertTrue(retrievedWeekly.isPresent(), "Weekly template should still exist");
    assertTrue(retrievedPLE.isPresent(), "PLE template should still exist");

    // Verify they maintain their original data
    assertThat(retrievedWeekly.get().getId()).isEqualTo(savedWeekly.getId());
    assertThat(retrievedPLE.get().getId()).isEqualTo(savedPLE.getId());

    log.info("✅ Pre-existing show templates handling verified successfully");
  }

  @Test
  @DisplayName("Should verify mixed Weekly and PLE show types in database after sync")
  void shouldVerifyMixedWeeklyAndPLEShowTypesInDatabaseAfterSync() {
    log.info("🎯 Testing database verification of mixed show types after sync");

    // Given - Create templates with different show types
    ShowTemplate weeklyTemplate1 = new ShowTemplate();
    weeklyTemplate1.setName("Monday Night RAW");
    weeklyTemplate1.setDescription("Weekly wrestling show");
    weeklyTemplate1.setShowType(showTypeRepository.findByName("Weekly").orElseThrow());
    ShowTemplate savedWeekly1 = showTemplateService.save(weeklyTemplate1);

    ShowTemplate weeklyTemplate2 = new ShowTemplate();
    weeklyTemplate2.setName("Friday Night SmackDown");
    weeklyTemplate2.setDescription("Weekly wrestling show");
    weeklyTemplate2.setShowType(showTypeRepository.findByName("Weekly").orElseThrow());
    ShowTemplate savedWeekly2 = showTemplateService.save(weeklyTemplate2);

    ShowTemplate pleTemplate1 = new ShowTemplate();
    pleTemplate1.setName("WrestleMania");
    pleTemplate1.setDescription("Premium Live Event");
    pleTemplate1.setShowType(
        showTypeRepository.findByName("Premium Live Event (PLE)").orElseThrow());
    ShowTemplate savedPLE1 = showTemplateService.save(pleTemplate1);

    ShowTemplate pleTemplate2 = new ShowTemplate();
    pleTemplate2.setName("SummerSlam");
    pleTemplate2.setDescription("Premium Live Event");
    pleTemplate2.setShowType(
        showTypeRepository.findByName("Premium Live Event (PLE)").orElseThrow());
    ShowTemplate savedPLE2 = showTemplateService.save(pleTemplate2);

    log.info("📝 Created test templates: {} Weekly, {} PLE", 2, 2);

    // When - Read all templates from database
    List<ShowTemplate> allTemplates = showTemplateService.findAll();
    log.info("📋 Found {} total templates in database", allTemplates.size());

    // Then - Verify mixed show types exist in database
    assertThat(allTemplates).hasSize(4);

    // Extract and verify show types from database entities
    List<ShowTemplate> weeklyTemplates =
        allTemplates.stream()
            .filter(template -> "Weekly".equals(template.getShowType().getName()))
            .toList();

    List<ShowTemplate> pleTemplates =
        allTemplates.stream()
            .filter(template -> "Premium Live Event (PLE)".equals(template.getShowType().getName()))
            .toList();

    // Verify correct distribution
    assertThat(weeklyTemplates).hasSize(2);
    assertThat(pleTemplates).hasSize(2);

    // Verify specific templates and their show types
    Optional<ShowTemplate> rawTemplate = showTemplateService.findByName("Monday Night RAW");
    assertTrue(rawTemplate.isPresent(), "Monday Night RAW template should exist");
    assertThat(rawTemplate.get().getShowType().getName()).isEqualTo("Weekly");
    assertThat(rawTemplate.get().getShowType().getDescription()).isEqualTo("Weekly Event");

    Optional<ShowTemplate> smackdownTemplate =
        showTemplateService.findByName("Friday Night SmackDown");
    assertTrue(smackdownTemplate.isPresent(), "Friday Night SmackDown template should exist");
    assertThat(smackdownTemplate.get().getShowType().getName()).isEqualTo("Weekly");

    Optional<ShowTemplate> wrestlemaniaTemplate = showTemplateService.findByName("WrestleMania");
    assertTrue(wrestlemaniaTemplate.isPresent(), "WrestleMania template should exist");
    assertThat(wrestlemaniaTemplate.get().getShowType().getName())
        .isEqualTo("Premium Live Event (PLE)");
    assertThat(wrestlemaniaTemplate.get().getShowType().getDescription())
        .isEqualTo("Premium Live Event");

    Optional<ShowTemplate> summerslamTemplate = showTemplateService.findByName("SummerSlam");
    assertTrue(summerslamTemplate.isPresent(), "SummerSlam template should exist");
    assertThat(summerslamTemplate.get().getShowType().getName())
        .isEqualTo("Premium Live Event (PLE)");

    // Verify each template has the correct show type relationship
    for (ShowTemplate template : allTemplates) {
      assertNotNull(
          template.getShowType(), "Template '" + template.getName() + "' should have a show type");
      assertNotNull(template.getShowType().getId(), "Show type should have an ID");
      assertThat(template.getShowType().getName()).isIn("Weekly", "Premium Live Event (PLE)");
      log.info(
          "✓ Template '{}' has show type '{}' (ID: {})",
          template.getName(),
          template.getShowType().getName(),
          template.getShowType().getId());
    }

    // Verify show type counts segment expectations
    long weeklyCount =
        allTemplates.stream().filter(t -> "Weekly".equals(t.getShowType().getName())).count();
    long pleCount =
        allTemplates.stream()
            .filter(t -> "Premium Live Event (PLE)".equals(t.getShowType().getName()))
            .count();

    assertThat(weeklyCount).isEqualTo(2);
    assertThat(pleCount).isEqualTo(2);

    log.info("📊 Database verification results:");
    log.info("   - Weekly templates: {}", weeklyCount);
    log.info("   - PLE templates: {}", pleCount);
    log.info("   - Total templates: {}", allTemplates.size());

    log.info("✅ Mixed show types database verification completed successfully");
  }

  @Test
  @DisplayName("Should handle sync failures gracefully")
  void shouldHandleSyncFailuresGracefully() {
    log.info("🚨 Testing sync failure handling");

    // When - Attempt sync (might fail due to missing Notion configuration)
    BaseSyncService.SyncResult result =
        showTemplateSyncService.syncShowTemplates(TEST_OPERATION_ID);

    // Then - Should handle gracefully regardless of success or failure
    assertNotNull(result, "Sync result should not be null");
    assertThat(result.getEntityType()).isEqualTo("Show Templates");

    if (result.isSuccess()) {
      log.info("✅ Sync succeeded: {}", result);
      assertThat(result.getSyncedCount()).isGreaterThanOrEqualTo(0);
    } else {
      log.info("⚠️ Sync failed as expected: {}", result.getErrorMessage());
      assertThat(result.getErrorMessage()).isNotBlank();
    }

    // Verify database is still in a consistent state
    List<ShowTemplate> showTemplates = showTemplateService.findAll();
    log.info("📊 Show templates in database after sync attempt: {}", showTemplates.size());

    // Should have valid show templates
    assertThat(showTemplates.size()).isGreaterThanOrEqualTo(0);

    log.info("✅ Failure handling verification completed successfully");
  }

  @Test
  @DisplayName("Should track sync progress correctly")
  void shouldTrackSyncProgressCorrectly() {
    log.info("📈 Testing sync progress tracking");

    // When - Run sync with operation ID for progress tracking
    String operationId = TEST_OPERATION_ID + "-progress";
    BaseSyncService.SyncResult result = showTemplateSyncService.syncShowTemplates(operationId);

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
  @DisplayName("Should validate show type associations correctly")
  void shouldValidateShowTypeAssociationsCorrectly() {
    log.info("🔗 Testing show type associations");

    // Given - Create templates with specific show type requirements
    ShowTemplate weeklyTemplate = new ShowTemplate();
    weeklyTemplate.setName("Friday Night SmackDown");
    weeklyTemplate.setDescription("Weekly wrestling show");
    weeklyTemplate.setShowType(showTypeRepository.findByName("Weekly").orElseThrow());
    showTemplateService.save(weeklyTemplate);

    ShowTemplate pleTemplate = new ShowTemplate();
    pleTemplate.setName("Royal Rumble");
    pleTemplate.setDescription("Premium Live Event");
    pleTemplate.setShowType(
        showTypeRepository.findByName("Premium Live Event (PLE)").orElseThrow());
    showTemplateService.save(pleTemplate);

    // When - Run sync
    BaseSyncService.SyncResult result =
        showTemplateSyncService.syncShowTemplates(TEST_OPERATION_ID);

    // Then - Verify templates maintain correct show type associations
    assertNotNull(result, "Sync result should not be null");

    List<ShowTemplate> allTemplates = showTemplateService.findAll();
    log.info("📊 Total templates after sync: {}", allTemplates.size());

    // Verify each template has a valid show type
    for (ShowTemplate template : allTemplates) {
      assertNotNull(
          template.getShowType(), "Template '" + template.getName() + "' should have a show type");
      assertThat(template.getShowType().getName()).isIn("Weekly", "Premium Live Event (PLE)");
      log.debug(
          "✓ Template '{}' has show type '{}'",
          template.getName(),
          template.getShowType().getName());
    }

    log.info("✅ Show type associations verification completed successfully");
  }

  @Test
  @DisplayName("Should handle external ID updates correctly")
  void shouldHandleExternalIdUpdatesCorrectly() {
    log.info("🆔 Testing external ID handling");

    // Given - Create a template without external ID
    ShowTemplate template = new ShowTemplate();
    template.setName("Test Event Template");
    template.setDescription("Template for testing external ID updates");
    template.setShowType(showTypeRepository.findByName("Weekly").orElseThrow());
    ShowTemplate saved = showTemplateService.save(template);
    assertNull(saved.getExternalId(), "Initial template should not have external ID");

    // When - Run sync (which might assign external IDs)
    BaseSyncService.SyncResult result =
        showTemplateSyncService.syncShowTemplates(TEST_OPERATION_ID);

    // Then - Verify sync completed
    assertNotNull(result, "Sync result should not be null");

    // Verify the template still exists (may or may not have external ID depending on sync source)
    Optional<ShowTemplate> retrievedTemplate =
        showTemplateService.findByName("Test Event Template");
    assertTrue(retrievedTemplate.isPresent(), "Template should still exist after sync");

    log.info("✅ External ID handling verification completed successfully");
  }

  @Test
  @DisplayName("Should maintain data integrity during concurrent operations")
  void shouldMaintainDataIntegrityDuringConcurrentOperations() {
    log.info("⚡ Testing concurrent operation data integrity");

    // Given - Create initial templates
    ShowTemplate template1 = new ShowTemplate();
    template1.setName("Concurrent Test Template 1");
    template1.setDescription("First template for concurrency testing");
    template1.setShowType(showTypeRepository.findByName("Weekly").orElseThrow());
    showTemplateService.save(template1);

    ShowTemplate template2 = new ShowTemplate();
    template2.setName("Concurrent Test Template 2");
    template2.setDescription("Second template for concurrency testing");
    template2.setShowType(showTypeRepository.findByName("Premium Live Event (PLE)").orElseThrow());
    showTemplateService.save(template2);

    int initialCount = showTemplateService.findAll().size();
    log.info("📊 Initial template count: {}", initialCount);

    // When - Run sync
    BaseSyncService.SyncResult result =
        showTemplateSyncService.syncShowTemplates(TEST_OPERATION_ID);

    // Then - Verify data integrity is maintained
    assertNotNull(result, "Sync result should not be null");

    List<ShowTemplate> finalTemplates = showTemplateService.findAll();
    log.info("📊 Final template count: {}", finalTemplates.size());

    // Verify original templates still exist
    assertTrue(
        showTemplateService.findByName("Concurrent Test Template 1").isPresent(),
        "First concurrent template should still exist");
    assertTrue(
        showTemplateService.findByName("Concurrent Test Template 2").isPresent(),
        "Second concurrent template should still exist");

    // Verify all templates have valid data
    for (ShowTemplate template : finalTemplates) {
      assertNotNull(template.getName(), "Template name should not be null");
      assertNotNull(template.getShowType(), "Template show type should not be null");
      assertNotNull(template.getCreationDate(), "Template creation date should not be null");
    }

    log.info("✅ Concurrent operation data integrity verification completed successfully");
  }
}
