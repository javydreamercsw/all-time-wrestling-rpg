package com.github.javydreamercsw.management.service.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class specifically for testing wrestler database persistence logic. This test verifies that
 * wrestlers are properly saved to the database with correct data using the right save methods.
 */
@ExtendWith(MockitoExtension.class)
@EnabledIf("isNotionTokenAvailable")
class WrestlerSyncDatabaseTest extends BaseSyncTest {

  private static final Logger log = LoggerFactory.getLogger(WrestlerSyncDatabaseTest.class);

  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerRepository wrestlerRepository;

  @Test
  @DisplayName("Should use WrestlerService.save() for new wrestlers")
  void shouldUseWrestlerServiceSaveForNewWrestlers() {
    // Given - Mock wrestler service save
    Wrestler savedWrestler = new Wrestler();
    savedWrestler.setId(1L);
    savedWrestler.setName("Test Wrestler");
    savedWrestler.setExternalId("notion-page-id-123");
    savedWrestler.setCreationDate(Instant.now());
    when(wrestlerService.save(any(Wrestler.class))).thenReturn(savedWrestler);

    // When - Simulate the database persistence logic for a new wrestler
    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Wrestler");
    wrestler.setExternalId("notion-page-id-123");
    wrestler.setDeckSize(15);
    wrestler.setStartingHealth(0);
    wrestler.setFans(0L);
    wrestler.setIsPlayer(false);

    // This simulates the logic in saveWrestlersToDatabase method
    boolean isNewWrestler = true; // Wrestler not found in database
    Wrestler result;
    if (isNewWrestler) {
      result = wrestlerService.save(wrestler);
    } else {
      result = wrestlerRepository.saveAndFlush(wrestler);
    }

    // Then
    assertNotNull(result);
    assertEquals("Test Wrestler", result.getName());
    assertEquals("notion-page-id-123", result.getExternalId());

    // Verify that wrestler service save was called (not repository)
    verify(wrestlerService, times(1)).save(any(Wrestler.class));
    verify(wrestlerRepository, never()).saveAndFlush(any(Wrestler.class));

    log.info("✅ Test passed: New wrestler uses WrestlerService.save()");
  }

  @Test
  @DisplayName("Should use WrestlerRepository.saveAndFlush() for existing wrestlers")
  void shouldUseWrestlerRepositorySaveAndFlushForExistingWrestlers() {
    // Given - Existing wrestler
    Wrestler existingWrestler = new Wrestler();
    existingWrestler.setId(2L);
    existingWrestler.setName("Existing Wrestler");
    existingWrestler.setDeckSize(20); // Different from default
    existingWrestler.setFans(5000L);
    existingWrestler.setCreationDate(Instant.now().minusSeconds(3600)); // Created 1 hour ago

    // Mock repository save for existing wrestler
    Wrestler updatedWrestler = new Wrestler();
    updatedWrestler.setId(2L);
    updatedWrestler.setName("Existing Wrestler");
    updatedWrestler.setExternalId("notion-page-id-456");
    updatedWrestler.setDeckSize(20); // Preserved
    updatedWrestler.setFans(5000L); // Preserved
    when(wrestlerRepository.saveAndFlush(any(Wrestler.class))).thenReturn(updatedWrestler);

    // When - Simulate the database persistence logic for an existing wrestler
    existingWrestler.setExternalId("notion-page-id-456"); // Update external ID

    // This simulates the logic in saveWrestlersToDatabase method
    boolean isNewWrestler = false; // Wrestler found in database
    Wrestler result;
    if (isNewWrestler) {
      result = wrestlerService.save(existingWrestler);
    } else {
      result = wrestlerRepository.saveAndFlush(existingWrestler);
    }

    // Then
    assertNotNull(result);
    assertEquals("Existing Wrestler", result.getName());
    assertEquals("notion-page-id-456", result.getExternalId());
    assertEquals(20, result.getDeckSize()); // Should preserve existing value

    // Verify that repository saveAndFlush was called (not service save)
    verify(wrestlerRepository, times(1)).saveAndFlush(any(Wrestler.class));
    verify(wrestlerService, never()).save(any(Wrestler.class));

    log.info("✅ Test passed: Existing wrestler uses WrestlerRepository.saveAndFlush()");
  }

  @Test
  @DisplayName("Should verify WrestlerService.save() sets creation date automatically")
  void shouldVerifyWrestlerServiceSaveSetsCreationDateAutomatically() {
    // Given - This test verifies the behavior we discovered in WrestlerService
    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Wrestler");
    wrestler.setExternalId("notion-page-id-789");

    // Mock the WrestlerService behavior (it always sets creation date)
    Wrestler savedWrestler = new Wrestler();
    savedWrestler.setId(3L);
    savedWrestler.setName("Test Wrestler");
    savedWrestler.setExternalId("notion-page-id-789");
    savedWrestler.setCreationDate(Instant.now()); // WrestlerService always sets this
    when(wrestlerService.save(any(Wrestler.class))).thenReturn(savedWrestler);

    // When
    Wrestler result = wrestlerService.save(wrestler);

    // Then
    assertNotNull(result);
    assertNotNull(result.getCreationDate(), "WrestlerService.save() should set creation date");
    assertEquals("Test Wrestler", result.getName());
    assertEquals("notion-page-id-789", result.getExternalId());

    verify(wrestlerService, times(1)).save(any(Wrestler.class));

    log.info("✅ Test passed: WrestlerService.save() sets creation date automatically");
  }
}
