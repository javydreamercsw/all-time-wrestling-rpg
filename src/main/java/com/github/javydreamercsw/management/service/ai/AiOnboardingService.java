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

import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.universe.UniverseAuthorizationService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Gate/save/status logic for the AI onboarding wizard (ATW-pq0s). The gate decides who may
 * configure the provider for the current universe: an admin by account role, or the universe's
 * OWNER (the tutorial-universe creator is one by construction). The save writes the same
 * game_setting rows Admin → AI Settings manages — one source of truth, no new storage. The status
 * check is deliberately cheap (enabled + key present, re-read from the service): a bad key surfaces
 * on first use through the existing AI error path, not a wizard-side probe.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiOnboardingService {

  /** Why the dialog may or may not offer key configuration right now. */
  public enum GateState {
    CAN_CONFIGURE,
    ALREADY_CONFIGURED,
    NOT_UNIVERSE_OWNER,
    NO_UNIVERSE_SELECTED
  }

  private final AiSettingsService aiSettingsService;
  private final GameSettingService gameSettingService;
  private final SecurityUtils securityUtils;
  private final UniverseContextService universeContextService;
  private final UniverseAuthorizationService universeAuthorizationService;
  private final SegmentNarrationServiceFactory aiFactory;

  /** The dialog's rendering decision, re-evaluated on every open. */
  public GateState evaluateGate() {
    return evaluateGateInternal();
  }

  /**
   * Persist the Gemini key for the current universe and report whether the cheap check passes
   * afterwards (re-read through {@link AiSettingsService} — the same resolution hierarchy readers
   * use, so the status line cannot lie about what was written).
   *
   * @param key the pasted API key (trimmed here)
   * @return true when the post-save re-read says configured
   * @throws IllegalArgumentException when the key is blank
   */
  public boolean saveGeminiKey(@NonNull String key) {
    String trimmed = key.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("API key is required");
    }
    if (evaluateGateInternal() != GateState.CAN_CONFIGURE) {
      log.warn("AI onboarding save attempted while gate is not CAN_CONFIGURE — refusing.");
      return false;
    }
    if (securityUtils.isAdmin()) {
      writeSettings(trimmed);
    } else {
      GeneralSecurityUtils.runAsAdmin(() -> writeSettings(trimmed));
    }
    return isConfigured();
  }

  /** The cheap configured check alone (no gate) — the dialog's status line re-read. */
  public boolean isConfigured() {
    return aiSettingsService.isGeminiEnabled() && !aiSettingsService.getGeminiApiKey().isBlank();
  }

  private void writeSettings(String trimmedKey) {
    // Enabled first so the cheap check passes atomically-ish once the key lands.
    gameSettingService.save("AI_GEMINI_ENABLED", "true");
    gameSettingService.save("AI_GEMINI_API_KEY", trimmedKey);
  }

  private GateState evaluateGateInternal() {
    if (!aiFactory.getAvailableServicesInPriorityOrder().isEmpty()) {
      return GateState.ALREADY_CONFIGURED;
    }
    return universeContextService
        .getCurrentUniverse()
        .map(
            current ->
                securityUtils.isAdmin() || universeAuthorizationService.isOwner(current)
                    ? GateState.CAN_CONFIGURE
                    : GateState.NOT_UNIVERSE_OWNER)
        .orElse(GateState.NO_UNIVERSE_SELECTED);
  }
}
