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
      topUpMissingCards();
      return;
    }
    loadCards(true);
  }

  /**
   * Skip-gate branch: the table is non-empty and the {@code skip-if-not-empty} flag protects
   * existing rows from seed-driven updates. Still insert seed entries that don't exist yet, so card
   * sets added in later releases reach flag-protected installs. Existing rows are never re-saved.
   */
  private void topUpMissingCards() {
    List<Card> missing = loadCards(false);
    if (!missing.isEmpty()) {
      log.info("Card top-up: inserting {} missing seed card(s).", missing.size());
      cardService.saveAll(missing);
    }
  }

  /**
   * Loads cards from {@code classpath*:cards/*.json}.
   *
   * @param updateExisting when true, updates existing rows to match the seed file (full sync); when
   *     false, only entries missing from the database are returned and existing rows are left
   *     untouched.
   * @return Cards that were inserted or need saving.
   */
  private List<Card> loadCards(boolean updateExisting) {
    try {
      Resource[] resources = resourcePatternResolver.getResources("classpath*:cards/*.json");
      Map<String, CardSet> setCache = new HashMap<>();
      Map<String, Card> existing =
          cardService.findAll().stream()
              .collect(
                  Collectors.toMap(
                      c -> c.getSet().getCode() + "#" + c.getNumber(), c -> c, (a, b) -> a));

      List<Card> toSave = new ArrayList<>();
      for (Resource resource : resources) {
        if (!resource.exists()) {
          continue;
        }
        log.debug("Loading cards from file: {}", resource.getFilename());
        try (var is = resource.getInputStream()) {
          List<CardDTO> cardsFromFile = objectMapper.readValue(is, new TypeReference<>() {});
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
            Card card = existing.get(key);
            if (card == null) {
              toSave.add(fromDto(dto, set));
            } else if (updateExisting) {
              applyDto(card, dto, set);
              toSave.add(card);
            }
          }
          if (updateExisting) {
            cardService.saveAll(toSave);
            log.debug("Saved/Updated {} cards from {}", toSave.size(), resource.getFilename());
          }
        } catch (IOException e) {
          log.error("Error loading cards from file: {}", resource.getFilename(), e);
        }
      }
      return toSave;
    } catch (IOException e) {
      log.error("Error resolving card resources", e);
      return List.of();
    }
  }

  private static Card fromDto(CardDTO dto, CardSet set) {
    Card card = new Card();
    applyDto(card, dto, set);
    return card;
  }

  private static void applyDto(Card card, CardDTO dto, CardSet set) {
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
  }
}
