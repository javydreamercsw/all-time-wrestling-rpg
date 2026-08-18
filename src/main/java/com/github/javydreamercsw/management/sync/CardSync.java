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
package com.github.javydreamercsw.management.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.dto.CardDTO;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(80)
public class CardSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final CardService cardService;
  private final CardSetService cardSetService;
  private final ResourcePatternResolver resourcePatternResolver;
  private final ObjectMapper objectMapper;

  @Autowired
  public CardSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final CardService cardService,
      final CardSetService cardSetService,
      final ResourcePatternResolver resourcePatternResolver,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.cardService = cardService;
    this.cardSetService = cardSetService;
    this.resourcePatternResolver = resourcePatternResolver;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (skipIfNotEmpty && cardService.count() > 0) {
      return;
    }
    try {
      Resource[] resources = resourcePatternResolver.getResources("classpath*:cards/*.json");
      Map<String, CardSet> setCache = new HashMap<>();
      Map<String, Card> existing =
          cardService.findAll().stream()
              .collect(
                  Collectors.toMap(
                      c -> c.getSet().getCode() + "#" + c.getNumber(), c -> c, (a, b) -> a));

      for (Resource resource : resources) {
        if (!resource.exists()) {
          continue;
        }
        log.debug("Loading cards from file: {}", resource.getFilename());
        try (var is = resource.getInputStream()) {
          List<CardDTO> cardsFromFile = objectMapper.readValue(is, new TypeReference<>() {});
          List<Card> toSave = new ArrayList<>();
          for (CardDTO dto : cardsFromFile) {
            CardSet set = setCache.get(dto.getSet());
            if (set == null) {
              Optional<CardSet> setOpt = cardSetService.findBySetCode(dto.getSet());
              if (setOpt.isEmpty()) {
                log.warn(
                    "CardSet with code {} not found for card {}. Skipping card.",
                    dto.getSet(),
                    dto.getName());
                continue;
              }
              set = setOpt.get();
              setCache.put(dto.getSet(), set);
            }

            final String key = dto.getSet() + "#" + dto.getNumber();
            Card card = existing.getOrDefault(key, new Card());
            card.setName(dto.getName());
            card.setDamage(dto.getDamage());
            card.setFinisher(dto.isFinisher());
            card.setSignature(dto.isSignature());
            card.setStamina(dto.getStamina());
            card.setMomentum(dto.getMomentum());
            card.setTarget(dto.getTarget());
            card.setNumber(dto.getNumber());
            card.setSet(set);
            card.setType(dto.getType());
            card.setTaunt(dto.isTaunt());
            card.setPin(dto.isPin());
            card.setRecover(dto.isRecover());
            toSave.add(card);
          }
          cardService.saveAll(toSave);
          log.debug("Saved/Updated {} cards from {}", toSave.size(), resource.getFilename());
        } catch (IOException e) {
          log.error("Error loading cards from file: {}", resource.getFilename(), e);
        }
      }
    } catch (IOException e) {
      log.error("Error resolving card resources", e);
    }
  }
}
