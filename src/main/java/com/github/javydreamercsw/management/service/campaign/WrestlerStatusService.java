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
package com.github.javydreamercsw.management.service.campaign;

import com.github.javydreamercsw.management.domain.campaign.StatusCard;
import com.github.javydreamercsw.management.domain.campaign.WrestlerStatus;
import com.github.javydreamercsw.management.domain.campaign.WrestlerStatusAction;
import com.github.javydreamercsw.management.domain.campaign.WrestlerStatusHistory;
import com.github.javydreamercsw.management.domain.campaign.WrestlerStatusHistoryRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerStatusRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class WrestlerStatusService {

  public static final String STATUS_CARDS_EXPANSION_CODE = "ATW_VS_WOW";

  private final WrestlerStatusRepository wrestlerStatusRepository;
  private final WrestlerStatusHistoryRepository wrestlerStatusHistoryRepository;
  private final StatusCardService statusCardService;
  private final WrestlerService wrestlerService;
  private final CampaignScriptService campaignScriptService;
  private final GameSettingService gameSettingService;
  private final ExpansionService expansionService;

  public boolean isStatusMechanicEnabled() {
    return gameSettingService.isStatusCardsEnabled()
        && expansionService.isExpansionEnabled(STATUS_CARDS_EXPANSION_CODE);
  }

  public java.util.List<StatusCard> getAllStatusCards() {
    return statusCardService.findAll();
  }

  public void assignStatus(Long wrestlerId, String statusKey) {
    if (!isStatusMechanicEnabled()) {
      log.debug("Status Cards mechanic is disabled. Ignoring assignment: {}", statusKey);
      return;
    }

    Wrestler wrestler =
        wrestlerService
            .findById(wrestlerId)
            .orElseThrow(
                () -> new EntityNotFoundException("Wrestler with id " + wrestlerId + " not found"));
    StatusCard card = statusCardService.findByKey(statusKey);

    Optional<WrestlerStatus> existingStatusOpt =
        wrestlerStatusRepository.findByWrestlerAndStatusCard(wrestler, card);

    if (existingStatusOpt.isPresent()) {
      WrestlerStatus status = existingStatusOpt.get();
      if (status.getLevel() == 1) {
        log.info("Flipping status {} for wrestler {} to Level II", statusKey, wrestler.getName());
        status.setLevel(2);
        wrestlerStatusRepository.save(status);
        logHistory(wrestler, card, WrestlerStatusAction.FLIP, 1, 2);
      } else {
        log.debug(
            "Wrestler {} already has status {} at Level II. Ignoring assignment.",
            wrestler.getName(),
            statusKey);
      }
    } else {
      log.info("Assigning status {} to wrestler {} at Level I", statusKey, wrestler.getName());
      WrestlerStatus newStatus =
          WrestlerStatus.builder().wrestler(wrestler).statusCard(card).level(1).build();
      wrestlerStatusRepository.save(newStatus);
      logHistory(wrestler, card, WrestlerStatusAction.GAIN, null, 1);
    }
  }

  public void removeStatus(Long wrestlerId, String statusKey) {
    if (!isStatusMechanicEnabled()) {
      log.debug("Status Cards mechanic is disabled. Ignoring removal: {}", statusKey);
      return;
    }

    Wrestler wrestler =
        wrestlerService
            .findById(wrestlerId)
            .orElseThrow(
                () -> new EntityNotFoundException("Wrestler with id " + wrestlerId + " not found"));
    StatusCard card = statusCardService.findByKey(statusKey);

    wrestlerStatusRepository
        .findByWrestlerAndStatusCard(wrestler, card)
        .ifPresent(
            status -> {
              log.info("Removing status {} from wrestler {}", statusKey, wrestler.getName());
              wrestlerStatusRepository.delete(status);
              logHistory(wrestler, card, WrestlerStatusAction.LOSS, status.getLevel(), null);
            });
  }

  /**
   * Evaluates the trigger conditions for a wrestler's active status.
   *
   * @param status The active status to evaluate.
   * @param finalMomentum The momentum at the end of the match.
   * @param isLoss Whether the wrestler lost the match.
   */
  public void evaluateTriggerConditions(WrestlerStatus status, int finalMomentum, boolean isLoss) {
    if (!isStatusMechanicEnabled()) {
      log.debug("Status Cards mechanic is disabled. Skipping evaluation.");
      return;
    }

    StatusCard card = status.getStatusCard();
    Wrestler wrestler = status.getWrestler();

    Map<String, Object> variables = new HashMap<>();
    variables.put("momentum", finalMomentum);
    variables.put("loss", isLoss);

    // 1. Check Discard Condition
    if (evaluateCondition(card.getDiscardCondition(), variables)) {
      log.info("Status {} discarded for wrestler {}", card.getKey(), wrestler.getName());
      wrestlerStatusRepository.delete(status);
      logHistory(wrestler, card, WrestlerStatusAction.LOSS, status.getLevel(), null);
      return;
    }

    // 2. Check Flip Up (Level I -> II)
    if (status.getLevel() == 1 && evaluateCondition(card.getFlipUpCondition(), variables)) {
      log.info("Status {} flipping UP for wrestler {}", card.getKey(), wrestler.getName());
      status.setLevel(2);
      wrestlerStatusRepository.save(status);
      logHistory(wrestler, card, WrestlerStatusAction.FLIP, 1, 2);
    }
    // 3. Check Flip Down (Level II -> I)
    else if (status.getLevel() == 2 && evaluateCondition(card.getFlipDownCondition(), variables)) {
      log.info("Status {} flipping DOWN for wrestler {}", card.getKey(), wrestler.getName());
      status.setLevel(1);
      wrestlerStatusRepository.save(status);
      logHistory(wrestler, card, WrestlerStatusAction.FLIP, 2, 1);
    }
  }

  private boolean evaluateCondition(String condition, Map<String, Object> variables) {
    if (condition == null || condition.isBlank()) {
      return false;
    }
    try {
      Object result = campaignScriptService.evaluateSnippet(condition, variables);
      return result instanceof Boolean && (Boolean) result;
    } catch (Exception e) {
      log.warn("Failed to evaluate status condition: {}", condition, e);
      return false;
    }
  }

  private void logHistory(
      Wrestler wrestler,
      StatusCard card,
      WrestlerStatusAction action,
      Integer oldLevel,
      Integer newLevel) {
    WrestlerStatusHistory history =
        WrestlerStatusHistory.builder()
            .wrestler(wrestler)
            .statusCard(card)
            .action(action)
            .oldLevel(oldLevel)
            .newLevel(newLevel)
            .build();
    wrestlerStatusHistoryRepository.save(history);
  }
}
