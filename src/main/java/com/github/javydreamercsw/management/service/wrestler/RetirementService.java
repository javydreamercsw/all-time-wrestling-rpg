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
package com.github.javydreamercsw.management.service.wrestler;

import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.WrestlerRetiredEvent;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RetirementService {

  private final WrestlerRepository wrestlerRepository;
  private final CampaignRepository campaignRepository;
  private final WrestlerService wrestlerService;
  private final ApplicationEventPublisher eventPublisher;
  private final Random random = new Random();

  /**
   * Checks if a wrestler should retire based on physical condition in a league.
   *
   * @param wrestler The wrestler to check
   * @param leagueId The league ID
   */
  @Transactional
  public void checkRetirement(Wrestler wrestler, Long leagueId) {
    if (!wrestler.getActive()) {
      return;
    }

    com.github.javydreamercsw.management.domain.wrestler.WrestlerState state =
        wrestlerService.getOrCreateState(wrestler.getId(), leagueId);
    int condition = state.getPhysicalCondition();

    // Retirement logic:
    // If condition < 10%, 50% chance of forced retirement.
    // If condition < 20%, 10% chance of forced retirement.

    boolean shouldRetire = false;
    String reason = "";

    if (condition < 10) {
      if (random.nextInt(100) < 50) {
        shouldRetire = true;
        reason = "Severe physical degradation forced an immediate retirement in league " + leagueId;
      }
    } else if (condition < 20) {
      if (random.nextInt(100) < 10) {
        shouldRetire = true;
        reason = "Accumulated wear and tear led to a career-ending injury in league " + leagueId;
      }
    }

    if (shouldRetire) {
      retireWrestler(wrestler, reason);
    }
  }

  @Transactional
  public void retireWrestler(Wrestler wrestler, String reason) {
    log.info("Retiring wrestler: {} - Reason: {}", wrestler.getName(), reason);

    wrestler.setActive(false);
    wrestlerRepository.save(wrestler);

    // If they have an active campaign, end it
    campaignRepository
        .findActiveByWrestler(wrestler)
        .ifPresent(
            campaign -> {
              CampaignState state = campaign.getState();
              state.setFeatureData(addRetirementFlag(state.getFeatureData()));
              campaignRepository.save(campaign);
            });

    eventPublisher.publishEvent(new WrestlerRetiredEvent(this, wrestler, reason));
  }

  private String addRetirementFlag(String featureData) {
    // Basic implementation - in a real scenario we'd use ObjectMapper
    return featureData == null
        ? "{\"retired\": true}"
        : featureData.replace("}", ", \"retired\": true}");
  }
}
