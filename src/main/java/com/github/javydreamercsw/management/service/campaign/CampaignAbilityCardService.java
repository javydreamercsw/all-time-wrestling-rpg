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

import com.github.javydreamercsw.management.domain.campaign.AbilityTiming;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCard;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCardRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CampaignAbilityCardService {

  private final CampaignAbilityCardRepository campaignAbilityCardRepository;

  public Optional<CampaignAbilityCard> findByNameAndAlignmentAndLevel(
      String name, AlignmentType alignmentType, int level) {
    CampaignAbilityCard probe = new CampaignAbilityCard();
    probe.setName(name);
    probe.setAlignmentType(alignmentType);
    probe.setLevel(level);
    return campaignAbilityCardRepository.findOne(Example.of(probe));
  }

  public CampaignAbilityCard createOrUpdateCard(
      String name,
      String description,
      AlignmentType alignmentType,
      int level,
      boolean oneTimeUse,
      AbilityTiming timing,
      int trackRequirement,
      String effectScript,
      String secondaryEffectScript,
      boolean secondaryOneTimeUse,
      AbilityTiming secondaryTiming) {

    Optional<CampaignAbilityCard> existingOpt =
        findByNameAndAlignmentAndLevel(name, alignmentType, level);

    CampaignAbilityCard card;
    if (existingOpt.isPresent()) {
      card = existingOpt.get();
      log.debug("Updating existing ability card: {}", name);
    } else {
      card = new CampaignAbilityCard();
      card.setName(name);
      card.setAlignmentType(alignmentType);
      card.setLevel(level);
      log.debug("Creating new ability card: {}", name);
    }

    card.setDescription(description);
    card.setOneTimeUse(oneTimeUse);
    card.setTiming(timing);
    card.setTrackRequirement(trackRequirement);
    card.setEffectScript(effectScript);
    card.setSecondaryEffectScript(secondaryEffectScript);
    card.setSecondaryOneTimeUse(secondaryOneTimeUse);
    card.setSecondaryTiming(secondaryTiming);

    return campaignAbilityCardRepository.save(card);
  }

  public List<CampaignAbilityCard> findAll() {
    return campaignAbilityCardRepository.findAll();
  }
}
