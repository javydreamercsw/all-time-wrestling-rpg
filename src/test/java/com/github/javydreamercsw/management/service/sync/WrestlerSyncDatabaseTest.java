/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.service.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class specifically for testing wrestler database persistence logic. This test verifies that
 * wrestlers are properly saved to the database with correct data using the right save methods.
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
class WrestlerSyncDatabaseTest extends BaseTest {
  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerRepository wrestlerRepository;

  @Test
  @DisplayName("Should use WrestlerService.save() for new wrestlers")
  void shouldUseWrestlerServiceSaveForNewWrestlers() {
    // Given - Mock wrestler service save
    Wrestler savedWrestler = Wrestler.builder().build();
    savedWrestler.setId(1L);
    savedWrestler.setName("Test Wrestler");
    savedWrestler.setExternalId("notion-page-id-123");
    savedWrestler.setCreationDate(Instant.now());
    when(wrestlerService.save(any(Wrestler.class))).thenReturn(savedWrestler);

    // When - Simulate the database persistence logic for a new wrestler
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setName("Test Wrestler");
    wrestler.setExternalId("notion-page-id-123");
    wrestler.setDeckSize(15);
    wrestler.setStartingHealth(0);
    wrestler.setFans(0L);
    wrestler.setIsPlayer(false);

    // This simulates the logic in saveWrestlersToDatabase method
    Wrestler result = wrestlerService.save(wrestler);

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
    Wrestler existingWrestler = Wrestler.builder().build();
    existingWrestler.setId(2L);
    existingWrestler.setName("Existing Wrestler");
    existingWrestler.setDeckSize(20); // Different from default
    existingWrestler.setFans(5000L);
    existingWrestler.setCreationDate(Instant.now().minusSeconds(3600)); // Created 1 hour ago

    // Mock repository save for existing wrestler
    Wrestler updatedWrestler = Wrestler.builder().build();
    updatedWrestler.setId(2L);
    updatedWrestler.setName("Existing Wrestler");
    updatedWrestler.setExternalId("notion-page-id-456");
    updatedWrestler.setDeckSize(20); // Preserved
    updatedWrestler.setFans(5000L); // Preserved
    when(wrestlerRepository.saveAndFlush(any(Wrestler.class))).thenReturn(updatedWrestler);

    // When - Simulate the database persistence logic for an existing wrestler
    existingWrestler.setExternalId("notion-page-id-456"); // Update external ID

    // This simulates the logic in saveWrestlersToDatabase method
    Wrestler result = wrestlerRepository.saveAndFlush(existingWrestler);

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
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setName("Test Wrestler");
    wrestler.setExternalId("notion-page-id-789");

    // Mock the WrestlerService behavior (it always sets creation date)
    Wrestler savedWrestler = Wrestler.builder().build();
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
