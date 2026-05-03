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
import com.github.javydreamercsw.management.domain.campaign.StatusCardRepository;
import jakarta.persistence.EntityNotFoundException;
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
public class StatusCardService {

  private final StatusCardRepository statusCardRepository;

  public StatusCard createOrUpdateCard(
      String key,
      String level1Name,
      String level2Name,
      String description,
      boolean positive,
      String level1Effect,
      String level2Effect,
      String flipUpCondition,
      String flipDownCondition,
      String discardCondition) {

    StatusCard probe = new StatusCard();
    probe.setKey(key);

    Optional<StatusCard> existingOpt = statusCardRepository.findOne(Example.of(probe));

    StatusCard card;
    if (existingOpt.isPresent()) {
      card = existingOpt.get();
      log.debug("Updating existing status card: {}", key);
    } else {
      card = new StatusCard();
      card.setKey(key);
      log.debug("Creating new status card: {}", key);
    }

    card.setLevel1Name(level1Name);
    card.setLevel2Name(level2Name);
    card.setDescription(description);
    card.setPositive(positive);
    card.setLevel1Effect(level1Effect);
    card.setLevel2Effect(level2Effect);
    card.setFlipUpCondition(flipUpCondition);
    card.setFlipDownCondition(flipDownCondition);
    card.setDiscardCondition(discardCondition);

    return statusCardRepository.save(card);
  }

  public StatusCard findByKey(String key) {
    return statusCardRepository
        .findByKey(key)
        .orElseThrow(
            () -> new EntityNotFoundException("StatusCard with key " + key + " not found"));
  }

  public java.util.List<StatusCard> findAll() {
    return statusCardRepository.findAll();
  }
}
