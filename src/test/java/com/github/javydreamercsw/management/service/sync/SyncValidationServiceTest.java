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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.config.NotionSyncProperties;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for SyncValidationService to verify prerequisite validation logic. Note: Full entity
 * validation tests are covered by integration tests due to the complexity of creating Notion page
 * objects.
 */
@ExtendWith(MockitoExtension.class)
class SyncValidationServiceTest {

  @Mock private NotionSyncProperties syncProperties;

  private SyncValidationService validationService;

  private MockedStatic<EnvironmentVariableUtil> mockedEnvironmentVariableUtil;

  @BeforeEach
  void setUp() {
    mockedEnvironmentVariableUtil = mockStatic(EnvironmentVariableUtil.class);
    when(EnvironmentVariableUtil.isNotionTokenAvailable()).thenReturn(true);
    validationService = new SyncValidationService(syncProperties);
  }

  @AfterEach
  void tearDown() {
    mockedEnvironmentVariableUtil.close();
  }

  @Test
  void shouldValidateSyncPrerequisitesSuccessfully() {
    // Given
    when(syncProperties.isEnabled()).thenReturn(true);

    NotionSyncProperties.Scheduler scheduler = new NotionSyncProperties.Scheduler();
    scheduler.setEnabled(true);
    scheduler.setInterval(300_000); // 5 minutes
    when(syncProperties.getScheduler()).thenReturn(scheduler);

    // When
    SyncValidationService.ValidationResult result = validationService.validateSyncPrerequisites();

    // Then
    assertThat(result.isValid()).isTrue();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getSummary()).contains("Validation passed");
  }

  @Test
  void shouldFailPrerequisitesWhenSyncDisabled() {
    // Given
    when(syncProperties.isEnabled()).thenReturn(false);

    // Mock scheduler to avoid null pointer
    NotionSyncProperties.Scheduler scheduler = new NotionSyncProperties.Scheduler();
    scheduler.setEnabled(false);
    scheduler.setInterval(300_000);
    when(syncProperties.getScheduler()).thenReturn(scheduler);

    // When
    SyncValidationService.ValidationResult result = validationService.validateSyncPrerequisites();

    // Then
    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).contains("Sync is disabled in configuration");
    assertThat(result.getSummary()).contains("Validation failed");
  }

  @Test
  void shouldWarnAboutShortSyncInterval() {
    // Given
    when(syncProperties.isEnabled()).thenReturn(true);

    NotionSyncProperties.Scheduler scheduler = new NotionSyncProperties.Scheduler();
    scheduler.setEnabled(true);
    scheduler.setInterval(30_000); // 30 seconds - very short
    when(syncProperties.getScheduler()).thenReturn(scheduler);

    // When
    SyncValidationService.ValidationResult result = validationService.validateSyncPrerequisites();

    // Then
    assertThat(result.hasWarnings()).isTrue();
    assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("very short"));
    assertThat(result.isValid()).isTrue();
  }

  @Test
  void shouldHandleEmptyShowsList() {
    // When
    SyncValidationService.ValidationResult result =
        validationService.validateShows(Collections.emptyList());

    // Then
    assertThat(result.isValid()).isTrue();
    assertThat(result.hasWarnings()).isTrue();
    assertThat(result.getWarnings()).contains("No shows to validate");
  }

  @Test
  void shouldHandleNullShowsList() {
    // When
    SyncValidationService.ValidationResult result = validationService.validateShows(null);

    // Then
    assertThat(result.isValid()).isTrue();
    assertThat(result.hasWarnings()).isTrue();
    assertThat(result.getWarnings()).contains("No shows to validate");
  }

  @Test
  void shouldHandleEmptyWrestlersList() {
    // When
    SyncValidationService.ValidationResult result =
        validationService.validateWrestlers(Collections.emptyList());

    // Then
    assertThat(result.isValid()).isTrue();
    assertThat(result.hasWarnings()).isTrue();
    assertThat(result.getWarnings()).contains("No wrestlers to validate");
  }

  @Test
  void shouldHandleEmptyFactionsList() {
    // When
    SyncValidationService.ValidationResult result =
        validationService.validateFactions(Collections.emptyList());

    // Then
    assertThat(result.isValid()).isTrue();
    assertThat(result.hasWarnings()).isTrue();
    assertThat(result.getWarnings()).contains("No factions to validate");
  }

  @Test
  void shouldHandleEmptyTeamsList() {
    // When
    SyncValidationService.ValidationResult result =
        validationService.validateTeams(Collections.emptyList());

    // Then
    assertThat(result.isValid()).isTrue();
    assertThat(result.hasWarnings()).isTrue();
    assertThat(result.getWarnings()).contains("No teams to validate");
  }

  @Test
  void shouldCreateValidationResultWithCorrectProperties() {
    // Given
    SyncValidationService.ValidationResult result =
        new SyncValidationService.ValidationResult(
            false, Arrays.asList("Error 1", "Error 2"), Arrays.asList("Warning 1"));

    // Then
    assertThat(result.isValid()).isFalse();
    assertThat(result.hasErrors()).isTrue();
    assertThat(result.hasWarnings()).isTrue();
    assertThat(result.getErrors()).hasSize(2);
    assertThat(result.getWarnings()).hasSize(1);
    // ValidationResult doesn't have getCheckTime() method
    assertThat(result.getSummary()).contains("Validation failed");
    assertThat(result.getSummary()).contains("2 errors");
    assertThat(result.getSummary()).contains("1 warnings");
  }

  @Test
  void shouldCreateSuccessfulValidationResult() {
    // Given
    SyncValidationService.ValidationResult result =
        new SyncValidationService.ValidationResult(
            true, Collections.emptyList(), Collections.emptyList());

    // Then
    assertThat(result.isValid()).isTrue();
    assertThat(result.hasErrors()).isFalse();
    assertThat(result.hasWarnings()).isFalse();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getWarnings()).isEmpty();
    assertThat(result.getSummary()).isEqualTo("Validation passed");
  }
}
