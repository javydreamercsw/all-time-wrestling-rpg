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
package com.github.javydreamercsw.management.service.ringside;

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.show.segment.RingsideAction;
import com.github.javydreamercsw.management.domain.show.segment.RingsideActionRepository;
import com.github.javydreamercsw.management.domain.show.segment.RingsideActionType;
import com.github.javydreamercsw.management.domain.show.segment.RingsideActionTypeRepository;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RingsideActionDataService {

  private final RingsideActionRepository ringsideActionRepository;
  private final RingsideActionTypeRepository ringsideActionTypeRepository;
  private final ExpansionService expansionService;
  private final UniverseContextService universeContextService;
  private final UniverseSettingsService universeSettingsService;

  private Set<String> enabledExpansionCodes() {
    return universeContextService
        .getCurrentUniverse()
        .map(universeSettingsService::getEnabledExpansionCodesForUniverse)
        .orElseGet(() -> new HashSet<>(expansionService.getEnabledExpansionCodes()));
  }

  public List<RingsideActionType> findAllTypes() {
    return ringsideActionTypeRepository.findAll();
  }

  public long countTypes() {
    return ringsideActionTypeRepository.count();
  }

  public List<RingsideAction> findAllActions() {
    Set<String> enabled = enabledExpansionCodes();
    return ringsideActionRepository.findAll().stream()
        .filter(a -> a.getExpansionCode() == null || enabled.contains(a.getExpansionCode()))
        .collect(Collectors.toList());
  }

  public List<RingsideAction> findAllActionsUnfiltered() {
    return ringsideActionRepository.findAll();
  }

  public long countActions() {
    return ringsideActionRepository.count();
  }

  @Transactional
  public RingsideActionType createOrUpdateType(
      @NonNull final String name,
      final boolean increasesAwareness,
      final boolean canCauseDq,
      final double baseRiskMultiplier) {
    Optional<RingsideActionType> existingOpt = ringsideActionTypeRepository.findByName(name);

    RingsideActionType type;
    if (existingOpt.isPresent()) {
      type = existingOpt.get();
      log.debug("Updating existing ringside action type: {}", name);
    } else {
      type = new RingsideActionType();
      log.debug("Creating new ringside action type: {}", name);
    }

    type.setName(name);
    type.setIncreasesAwareness(increasesAwareness);
    type.setCanCauseDq(canCauseDq);
    type.setBaseRiskMultiplier(baseRiskMultiplier);

    return ringsideActionTypeRepository.save(type);
  }

  @Transactional
  public RingsideAction createOrUpdateAction(
      @NonNull final String name,
      @NonNull final String typeName,
      final String description,
      final int impact,
      final int risk,
      final AlignmentType alignment) {
    return createOrUpdateAction(name, typeName, description, impact, risk, alignment, "BASE_GAME");
  }

  @Transactional
  public RingsideAction createOrUpdateAction(
      @NonNull final String name,
      @NonNull final String typeName,
      final String description,
      final int impact,
      final int risk,
      final AlignmentType alignment,
      final String expansionCode) {

    RingsideActionType type =
        ringsideActionTypeRepository
            .findByName(typeName)
            .orElseThrow(
                () -> new IllegalArgumentException("RingsideActionType not found: " + typeName));

    Optional<RingsideAction> existingOpt = ringsideActionRepository.findByName(name);

    RingsideAction action;
    if (existingOpt.isPresent()) {
      action = existingOpt.get();
      log.debug("Updating existing ringside action: {}", name);
    } else {
      action = new RingsideAction();
      log.debug("Creating new ringside action: {}", name);
    }

    action.setName(name);
    action.setType(type);
    action.setDescription(description);
    action.setImpact(impact);
    action.setRisk(risk);
    action.setAlignment(alignment != null ? alignment : AlignmentType.NEUTRAL);
    action.setExpansionCode(expansionCode != null ? expansionCode : "BASE_GAME");

    return ringsideActionRepository.save(action);
  }
}
