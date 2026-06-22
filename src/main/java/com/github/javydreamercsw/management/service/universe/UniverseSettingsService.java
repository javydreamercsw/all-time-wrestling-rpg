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
package com.github.javydreamercsw.management.service.universe;

import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseExpansionSetting;
import com.github.javydreamercsw.management.domain.universe.UniverseExpansionSettingRepository;
import com.github.javydreamercsw.management.domain.universe.UniverseWrestlerExclusion;
import com.github.javydreamercsw.management.domain.universe.UniverseWrestlerExclusionRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.expansion.Expansion;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UniverseSettingsService {

  private final UniverseExpansionSettingRepository expansionSettingRepository;
  private final UniverseWrestlerExclusionRepository wrestlerExclusionRepository;
  private final ExpansionService expansionService;
  private final com.github.javydreamercsw.management.service.GameSettingService gameSettingService;
  private final ApplicationEventPublisher eventPublisher;

  /** Returns the set of expansion codes enabled for a universe (falling back to global). */
  @PreAuthorize("isAuthenticated()")
  public Set<String> getEnabledExpansionCodesForUniverse(@NonNull final Universe universe) {
    return expansionService.getExpansions().stream()
        .filter(e -> isExpansionEnabledForUniverse(universe, e.getCode()))
        .map(Expansion::getCode)
        .collect(Collectors.toSet());
  }

  /** Returns all expansions with their per-universe enabled state (falling back to global). */
  @PreAuthorize("isAuthenticated()")
  public List<Expansion> getExpansionsForUniverse(@NonNull final Universe universe) {
    List<Expansion> expansions = expansionService.getExpansions();
    expansions.forEach(e -> e.setEnabled(isExpansionEnabledForUniverse(universe, e.getCode())));
    return expansions;
  }

  /** Checks per-universe override first, then falls back to the global expansion setting. */
  @PreAuthorize("isAuthenticated()")
  public boolean isExpansionEnabledForUniverse(
      @NonNull final Universe universe, @NonNull final String expansionCode) {
    return expansionSettingRepository
        .findByUniverseAndExpansionCode(universe, expansionCode)
        .map(UniverseExpansionSetting::isEnabled)
        .orElseGet(
            () -> {
              String key =
                  com.github.javydreamercsw.management.service.expansion.ExpansionService
                          .SET_ENABLED_PREFIX
                      + expansionCode;
              return gameSettingService
                  .findByKeyForUniverse(key, universe.getId())
                  .map(s -> Boolean.parseBoolean(s.getValue()))
                  .orElse(true);
            });
  }

  /**
   * Sets a per-universe expansion override. Deletes the override row if the value matches the
   * global default, keeping the table lean.
   */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or @universeAuthz.hasRole(#universe, 'BOOKER')")
  @Transactional
  public void setExpansionEnabled(
      @NonNull final Universe universe,
      @NonNull final String expansionCode,
      final boolean enabled) {
    UniverseExpansionSetting setting =
        expansionSettingRepository
            .findByUniverseAndExpansionCode(universe, expansionCode)
            .orElseGet(
                () -> {
                  UniverseExpansionSetting s = new UniverseExpansionSetting();
                  s.setUniverse(universe);
                  s.setExpansionCode(expansionCode);
                  return s;
                });
    setting.setEnabled(enabled);
    expansionSettingRepository.save(setting);
    eventPublisher.publishEvent(
        new UniverseExpansionToggledEvent(this, universe.getId(), expansionCode, enabled));
  }

  /** Removes the per-universe override, reverting to the global setting. */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or @universeAuthz.hasRole(#universe, 'BOOKER')")
  @Transactional
  public void resetExpansionToGlobal(
      @NonNull final Universe universe, @NonNull final String expansionCode) {
    expansionSettingRepository.deleteByUniverseAndExpansionCode(universe, expansionCode);
  }

  /** Returns the set of wrestlers excluded from a universe. */
  @PreAuthorize("isAuthenticated()")
  public Set<Wrestler> getExcludedWrestlers(@NonNull final Universe universe) {
    return wrestlerExclusionRepository.findByUniverse(universe).stream()
        .map(UniverseWrestlerExclusion::getWrestler)
        .collect(Collectors.toSet());
  }

  /** Excludes a wrestler from a universe. No-op if already excluded. */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or @universeAuthz.hasRole(#universe, 'BOOKER')")
  @Transactional
  public void excludeWrestler(@NonNull final Universe universe, @NonNull final Wrestler wrestler) {
    if (!wrestlerExclusionRepository.existsByUniverseAndWrestler(universe, wrestler)) {
      UniverseWrestlerExclusion exclusion = new UniverseWrestlerExclusion();
      exclusion.setUniverse(universe);
      exclusion.setWrestler(wrestler);
      wrestlerExclusionRepository.save(exclusion);
    }
  }

  /** Removes a wrestler exclusion, making them available again in this universe. */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or @universeAuthz.hasRole(#universe, 'BOOKER')")
  @Transactional
  public void includeWrestler(@NonNull final Universe universe, @NonNull final Wrestler wrestler) {
    wrestlerExclusionRepository.deleteByUniverseAndWrestler(universe, wrestler);
  }
}
