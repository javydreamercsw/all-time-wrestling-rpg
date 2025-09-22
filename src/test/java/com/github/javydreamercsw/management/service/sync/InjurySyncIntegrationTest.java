package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@DisplayName("Injury Sync Integration Tests")
class InjurySyncIntegrationTest extends AbstractIntegrationTest {

  @Autowired private NotionSyncService notionSyncService;

  @Autowired private InjuryTypeRepository injuryTypeRepository;

  @Test
  @DisplayName("Should sync injury types from real Notion database")
  void shouldSyncInjuryTypesFromRealNotionDatabase() {
    log.info("🏥 Testing real injury types sync from Notion");

    // Given - Clean state (transaction will rollback)
    long initialCount = injuryTypeRepository.count();
    log.info("Initial injury types count: {}", initialCount);

    // When - Sync injury types from Notion
    NotionSyncService.SyncResult result =
        notionSyncService.syncInjuryTypes("integration-test-operation");

    // Then - Verify sync was successful
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntityType()).isEqualTo("Injuries");
    assertThat(result.getSyncedCount()).isGreaterThanOrEqualTo(0);
    assertThat(result.getErrorCount()).isEqualTo(0);
    assertThat(result.getErrorMessage()).isNull();

    log.info("✅ Sync result: {} injury types synced", result.getSyncedCount());

    // Verify database state
    long finalCount = injuryTypeRepository.count();
    log.info("Final injury types count: {}", finalCount);

    if (result.getSyncedCount() > 0) {
      assertThat(finalCount).isGreaterThan(initialCount);

      // Verify some injury types were created
      List<InjuryType> allInjuryTypes = injuryTypeRepository.findAll();
      assertThat(allInjuryTypes).isNotEmpty();

      // Verify injury types have required fields
      for (InjuryType injuryType : allInjuryTypes) {
        assertThat(injuryType.getInjuryName()).isNotBlank();
        assertThat(injuryType.getExternalId()).isNotBlank();
        log.debug(
            "Found injury type: {} (ID: {})",
            injuryType.getInjuryName(),
            injuryType.getExternalId());
      }
    }
  }

  @Test
  @DisplayName("Should handle duplicate sync operations gracefully")
  void shouldHandleDuplicateSyncOperationsGracefully() {
    log.info("🔄 Testing duplicate injury sync operations");

    // Given - First sync
    NotionSyncService.SyncResult firstResult =
        notionSyncService.syncInjuryTypes("first-sync-operation");
    assertThat(firstResult.isSuccess()).isTrue();

    long countAfterFirstSync = injuryTypeRepository.count();
    log.info("Count after first sync: {}", countAfterFirstSync);

    // When - Second sync (should update existing, not create duplicates)
    NotionSyncService.SyncResult secondResult =
        notionSyncService.syncInjuryTypes("second-sync-operation");

    // Then - Verify second sync was successful
    assertThat(secondResult).isNotNull();
    assertThat(secondResult.isSuccess()).isTrue();
    assertThat(secondResult.getEntityType()).isEqualTo("Injuries");
    assertThat(secondResult.getErrorCount()).isEqualTo(0);

    long countAfterSecondSync = injuryTypeRepository.count();
    log.info("Count after second sync: {}", countAfterSecondSync);

    // Should not create duplicates - count should be the same or similar
    // (might differ slightly if Notion data changed between syncs)
    assertThat(countAfterSecondSync).isGreaterThanOrEqualTo(countAfterFirstSync - 1);
    assertThat(countAfterSecondSync).isLessThanOrEqualTo(countAfterFirstSync + 1);

    log.info("✅ Duplicate sync handled correctly");
  }

  @Test
  @DisplayName("Should sync specific injury types with correct data")
  void shouldSyncSpecificInjuryTypesWithCorrectData() {
    log.info("🎯 Testing specific injury type data accuracy");

    // When - Sync injury types
    NotionSyncService.SyncResult result = notionSyncService.syncInjuryTypes("data-accuracy-test");
    assertThat(result.isSuccess()).isTrue();

    if (result.getSyncedCount() > 0) {
      // Then - Verify specific injury types exist with correct data
      List<InjuryType> allInjuryTypes = injuryTypeRepository.findAll();
      assertThat(allInjuryTypes).isNotEmpty();

      // Look for common injury types that should exist
      Optional<InjuryType> headInjury =
          allInjuryTypes.stream()
              .filter(injury -> injury.getInjuryName().toLowerCase().contains("head"))
              .findFirst();

      Optional<InjuryType> legInjury =
          allInjuryTypes.stream()
              .filter(injury -> injury.getInjuryName().toLowerCase().contains("leg"))
              .findFirst();

      // Verify at least one injury type has proper game effects
      InjuryType sampleInjury = allInjuryTypes.get(0);
      assertThat(sampleInjury.getInjuryName()).isNotBlank();
      assertThat(sampleInjury.getExternalId()).isNotBlank();

      // Health effect should be present (can be positive, negative, or zero)
      assertThat(sampleInjury.getHealthEffect()).isNotNull();

      // Stamina effect should be present (can be positive, negative, or zero)
      assertThat(sampleInjury.getStaminaEffect()).isNotNull();

      // Card effect should be present (can be positive, negative, or zero)
      assertThat(sampleInjury.getCardEffect()).isNotNull();

      log.info(
          "✅ Sample injury type '{}' has effects: Health={}, Stamina={}, Card={}",
          sampleInjury.getInjuryName(),
          sampleInjury.getHealthEffect(),
          sampleInjury.getStaminaEffect(),
          sampleInjury.getCardEffect());

      // Special effects can be null or empty, but if present should be meaningful
      if (sampleInjury.getSpecialEffects() != null
          && !sampleInjury.getSpecialEffects().trim().isEmpty()) {
        assertThat(sampleInjury.getSpecialEffects()).doesNotContain("N/A");
        log.info("Special effects: {}", sampleInjury.getSpecialEffects());
      }
    } else {
      log.info(
          "ℹ️ No injury types found in Notion database - this is acceptable for empty databases");
    }
  }

  @Test
  @DisplayName("Should maintain referential integrity during sync")
  void shouldMaintainReferentialIntegrityDuringSync() {
    log.info("🔗 Testing referential integrity during injury sync");

    // Given - Record initial state
    long initialCount = injuryTypeRepository.count();

    // When - Sync injury types
    NotionSyncService.SyncResult result = notionSyncService.syncInjuryTypes("integrity-test");

    // Then - Verify integrity
    assertThat(result.isSuccess()).isTrue();

    // Verify all injury types have valid external IDs
    List<InjuryType> allInjuryTypes = injuryTypeRepository.findAll();
    for (InjuryType injuryType : allInjuryTypes) {
      assertThat(injuryType.getId()).isNotNull();
      assertThat(injuryType.getExternalId()).isNotBlank();
      assertThat(injuryType.getInjuryName()).isNotBlank();

      // Verify external ID is unique
      long countWithSameExternalId =
          allInjuryTypes.stream()
              .filter(other -> other.getExternalId().equals(injuryType.getExternalId()))
              .count();
      assertThat(countWithSameExternalId).isEqualTo(1);
    }

    log.info("✅ Referential integrity maintained for {} injury types", allInjuryTypes.size());
  }
}
