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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CampaignAbilityCardService {

  private final CampaignAbilityCardRepository campaignAbilityCardRepository;

  public CampaignAbilityCard save(CampaignAbilityCard card) {
    return campaignAbilityCardRepository.save(card);
  }

  public List<CampaignAbilityCard> saveAll(final List<CampaignAbilityCard> cards) {
    return campaignAbilityCardRepository.saveAll(cards);
  }

  public void delete(final Long id) {
    campaignAbilityCardRepository.deleteById(id);
  }

  public Page<CampaignAbilityCard> list(final Pageable pageable) {
    return campaignAbilityCardRepository.findAll(pageable);
  }

  public Optional<CampaignAbilityCard> findByNameAndAlignmentAndLevel(
      final String name, final AlignmentType alignmentType, final int level) {
    return campaignAbilityCardRepository.findByNameAndAlignmentTypeAndLevel(
        name, alignmentType, level);
  }

  public CampaignAbilityCard createOrUpdateCard(
      final String name,
      final String description,
      final AlignmentType alignmentType,
      final int level,
      final boolean oneTimeUse,
      final AbilityTiming timing,
      final String effectScript,
      final String secondaryEffectScript,
      final boolean secondaryOneTimeUse,
      final AbilityTiming secondaryTiming) {

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
    card.setEffectScript(effectScript);
    card.setSecondaryEffectScript(secondaryEffectScript);
    card.setSecondaryOneTimeUse(secondaryOneTimeUse);
    card.setSecondaryTiming(secondaryTiming);

    return campaignAbilityCardRepository.save(card);
  }

  public List<CampaignAbilityCard> findAll() {
    return campaignAbilityCardRepository.findAll();
  }

  public long count() {
    return campaignAbilityCardRepository.count();
  }
}
