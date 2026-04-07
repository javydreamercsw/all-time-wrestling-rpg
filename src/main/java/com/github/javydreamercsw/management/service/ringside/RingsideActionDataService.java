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
import java.util.List;
import java.util.Optional;
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

  public List<RingsideActionType> findAllTypes() {
    return ringsideActionTypeRepository.findAll();
  }

  public List<RingsideAction> findAllActions() {
    return ringsideActionRepository.findAll();
  }

  @Transactional
  public RingsideActionType createOrUpdateType(
      @NonNull String name,
      boolean increasesAwareness,
      boolean canCauseDq,
      double baseRiskMultiplier) {
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
      @NonNull String name,
      @NonNull String typeName,
      String description,
      int impact,
      int risk,
      AlignmentType alignment) {

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

    return ringsideActionRepository.save(action);
  }
}
