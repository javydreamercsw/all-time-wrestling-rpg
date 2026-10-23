/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.universe.UniverseAuthorizationService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiOnboardingServiceTest {

  @Mock private AiSettingsService aiSettingsService;
  @Mock private GameSettingService gameSettingService;
  @Mock private SecurityUtils securityUtils;
  @Mock private UniverseContextService universeContextService;
  @Mock private UniverseAuthorizationService universeAuthorizationService;
  @Mock private SegmentNarrationServiceFactory aiFactory;
  @Mock private SegmentNarrationService narrationService;

  private AiOnboardingService service;
  private Universe universe;

  @BeforeEach
  void setUp() {
    service =
        new AiOnboardingService(
            aiSettingsService,
            gameSettingService,
            securityUtils,
            universeContextService,
            universeAuthorizationService,
            aiFactory);
    universe = new Universe();
    universe.setId(1L);
    when(universeContextService.getCurrentUniverse()).thenReturn(Optional.of(universe));
  }

  @Test
  @DisplayName("Gate: already-configured wins over everything — even with no universe")
  void gate_alreadyConfigured_winsOverEverything() {
    when(aiFactory.getAvailableServicesInPriorityOrder()).thenReturn(List.of(narrationService));
    when(universeContextService.getCurrentUniverse()).thenReturn(Optional.empty());

    assertEquals(AiOnboardingService.GateState.ALREADY_CONFIGURED, service.evaluateGate());
  }

  @Test
  @DisplayName("Gate: no universe selected in session")
  void gate_noUniverseSelected() {
    when(aiFactory.getAvailableServicesInPriorityOrder()).thenReturn(List.of());
    when(universeContextService.getCurrentUniverse()).thenReturn(Optional.empty());

    assertEquals(AiOnboardingService.GateState.NO_UNIVERSE_SELECTED, service.evaluateGate());
  }

  @Test
  @DisplayName("Gate: admin passes without universe membership (isOwner not consulted)")
  void gate_adminPasses_withoutUniverseMembership() {
    when(aiFactory.getAvailableServicesInPriorityOrder()).thenReturn(List.of());
    when(securityUtils.isAdmin()).thenReturn(true);

    assertEquals(AiOnboardingService.GateState.CAN_CONFIGURE, service.evaluateGate());
    verify(universeAuthorizationService, never()).isOwner(any());
  }

  @Test
  @DisplayName("Gate: universe owner passes")
  void gate_universeOwnerPasses() {
    when(aiFactory.getAvailableServicesInPriorityOrder()).thenReturn(List.of());
    when(securityUtils.isAdmin()).thenReturn(false);
    when(universeAuthorizationService.isOwner(universe)).thenReturn(true);

    assertEquals(AiOnboardingService.GateState.CAN_CONFIGURE, service.evaluateGate());
  }

  @Test
  @DisplayName("Gate: non-owner member denied")
  void gate_nonOwnerDenied() {
    when(aiFactory.getAvailableServicesInPriorityOrder()).thenReturn(List.of());
    when(securityUtils.isAdmin()).thenReturn(false);
    when(universeAuthorizationService.isOwner(universe)).thenReturn(false);

    assertEquals(AiOnboardingService.GateState.NOT_UNIVERSE_OWNER, service.evaluateGate());
  }

  @Test
  @DisplayName("Save: blank key throws")
  void save_blankKey_throws() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> service.saveGeminiKey("   "));
    assertTrue(ex.getMessage().contains("API key is required"));
  }

  @Test
  @DisplayName("Save: gate is re-evaluated — non-owner saves nothing")
  void save_reEvaluatesGate_onlySavesWhenCanConfigure() {
    when(aiFactory.getAvailableServicesInPriorityOrder()).thenReturn(List.of());
    when(securityUtils.isAdmin()).thenReturn(false);
    when(universeAuthorizationService.isOwner(universe)).thenReturn(false);

    assertFalse(service.saveGeminiKey("AIza-key"));
    verify(gameSettingService, never()).save(any(), any());
  }

  @Test
  @DisplayName("Save: owner path elevates through runAsAdmin and writes both keys")
  void save_owner_usesRunAsAdmin() {
    when(aiFactory.getAvailableServicesInPriorityOrder()).thenReturn(List.of());
    when(securityUtils.isAdmin()).thenReturn(false);
    when(universeAuthorizationService.isOwner(universe)).thenReturn(true);
    when(aiSettingsService.isGeminiEnabled()).thenReturn(true);
    when(aiSettingsService.getGeminiApiKey()).thenReturn("AIza-key");

    try (MockedStatic<GeneralSecurityUtils> mocked =
        Mockito.mockStatic(GeneralSecurityUtils.class)) {
      // Production passes a void lambda, so the Runnable overload is the one invoked —
      // the stub must match it (the Supplier overload is never called).
      mocked
          .when(() -> GeneralSecurityUtils.runAsAdmin((Runnable) Mockito.any()))
          .thenAnswer(
              inv -> {
                ((Runnable) inv.getArgument(0)).run();
                return null;
              });

      assertTrue(service.saveGeminiKey("  AIza-key  "));

      verify(gameSettingService).save("AI_GEMINI_ENABLED", "true");
      verify(gameSettingService).save("AI_GEMINI_API_KEY", "AIza-key");
    }
  }

  @Test
  @DisplayName("Save: admin path saves directly without elevation")
  void save_admin_savesDirectly() {
    when(aiFactory.getAvailableServicesInPriorityOrder()).thenReturn(List.of());
    when(securityUtils.isAdmin()).thenReturn(true);
    when(aiSettingsService.isGeminiEnabled()).thenReturn(true);
    when(aiSettingsService.getGeminiApiKey()).thenReturn("AIza-key");

    assertTrue(service.saveGeminiKey("AIza-key"));

    verify(gameSettingService).save("AI_GEMINI_ENABLED", "true");
    verify(gameSettingService).save("AI_GEMINI_API_KEY", "AIza-key");
  }

  @Test
  @DisplayName("Save: returns the post-save re-read, not the field value")
  void save_returnsReReadResult() {
    when(aiFactory.getAvailableServicesInPriorityOrder()).thenReturn(List.of());
    when(securityUtils.isAdmin()).thenReturn(true);
    // The write "succeeds" but the re-read (same resolution hierarchy readers use) says no.
    when(aiSettingsService.isGeminiEnabled()).thenReturn(false);
    when(aiSettingsService.getGeminiApiKey()).thenReturn("");

    assertFalse(service.saveGeminiKey("AIza-key"));
  }

  @Test
  @DisplayName("Save: enabled is written before the key")
  void save_writesEnabledBeforeKey() {
    when(aiFactory.getAvailableServicesInPriorityOrder()).thenReturn(List.of());
    when(securityUtils.isAdmin()).thenReturn(true);
    when(aiSettingsService.isGeminiEnabled()).thenReturn(true);
    when(aiSettingsService.getGeminiApiKey()).thenReturn("AIza-key");

    service.saveGeminiKey("AIza-key");

    ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> values = ArgumentCaptor.forClass(String.class);
    verify(gameSettingService, Mockito.times(2)).save(keys.capture(), values.capture());
    assertEquals("AI_GEMINI_ENABLED", keys.getAllValues().get(0));
    assertEquals("AI_GEMINI_API_KEY", keys.getAllValues().get(1));
  }

  @Test
  @DisplayName("isConfigured: true only when enabled and key non-blank")
  void isConfigured_isTheCheapCheck() {
    when(aiSettingsService.isGeminiEnabled()).thenReturn(true, true, false);
    when(aiSettingsService.getGeminiApiKey()).thenReturn("AIza-key", "", "AIza-key");

    assertTrue(service.isConfigured());
    assertFalse(service.isConfigured());
    assertFalse(service.isConfigured());
  }
}
