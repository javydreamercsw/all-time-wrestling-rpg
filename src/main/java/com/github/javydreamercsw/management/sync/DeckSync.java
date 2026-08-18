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
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.DeckCardDTO;
import com.github.javydreamercsw.management.dto.DeckDTO;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(150)
public class DeckSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final DeckService deckService;
  private final WrestlerRepository wrestlerRepository;
  private final CardSetRepository cardSetRepository;
  private final CardService cardService;
  private final ObjectMapper objectMapper;

  @Autowired
  public DeckSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final DeckService deckService,
      final WrestlerRepository wrestlerRepository,
      final CardSetRepository cardSetRepository,
      final CardService cardService,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.deckService = deckService;
    this.wrestlerRepository = wrestlerRepository;
    this.cardSetRepository = cardSetRepository;
    this.cardService = cardService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (skipIfNotEmpty && deckService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("decks.json");
    if (!resource.exists()) {
      return;
    }
    log.debug("Loading decks from file: {}", resource.getPath());
    try (var is = resource.getInputStream()) {
      List<DeckDTO> decksFromFile = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, Wrestler> wrestlers =
          wrestlerRepository.findAll().stream()
              .collect(Collectors.toMap(Wrestler::getName, w -> w, (a, b) -> a));
      Map<Long, CardSet> setCache = new HashMap<>();
      cardSetRepository.findAll().forEach(cs -> setCache.put(cs.getId(), cs));

      List<Deck> decksToSave = new ArrayList<>();
      for (DeckDTO deckDTO : decksFromFile) {
        Wrestler wrestler = wrestlers.get(deckDTO.getWrestler());
        if (wrestler == null) {
          continue;
        }

        List<Deck> byWrestler = deckService.findByWrestlerWithCards(wrestler);
        Deck deck = byWrestler.isEmpty() ? deckService.createDeck(wrestler) : byWrestler.getFirst();

        deck.getCards()
            .forEach(
                dc -> {
                  if (dc.getSet() != null && dc.getSet().getId() != null) {
                    dc.setSet(setCache.getOrDefault(dc.getSet().getId(), dc.getSet()));
                  }
                  if (dc.getCard() != null
                      && dc.getCard().getSet() != null
                      && dc.getCard().getSet().getId() != null) {
                    dc.getCard()
                        .setSet(
                            setCache.getOrDefault(
                                dc.getCard().getSet().getId(), dc.getCard().getSet()));
                  }
                });

        Set<DeckCard> cardsToRemove = new HashSet<>(deck.getCards());
        Map<String, Integer> cardKeyToAmount = new HashMap<>();
        Map<String, Card> cardKeyToCard = new HashMap<>();

        for (DeckCardDTO cardDTO : deckDTO.getCards()) {
          Card card =
              cardService.findByNumberAndSet(cardDTO.getNumber(), cardDTO.getSet()).orElse(null);
          if (card == null) {
            log.warn(
                "Card not found: {} in set {} from deck {}",
                cardDTO.getNumber(),
                cardDTO.getSet(),
                wrestler.getName());
            continue;
          }

          CardSet canonicalSet =
              setCache.computeIfAbsent(card.getSet().getId(), id -> card.getSet());
          card.setSet(canonicalSet);

          String key = card.getSet().getName() + "-" + card.getId();
          cardKeyToAmount.merge(key, cardDTO.getAmount(), Integer::sum);
          cardKeyToCard.putIfAbsent(key, card);
        }

        boolean changed = false;
        for (var entry : cardKeyToAmount.entrySet()) {
          Card card = cardKeyToCard.get(entry.getKey());
          int amount = entry.getValue();

          Optional<DeckCard> existingDeckCardOpt =
              deck.getCards().stream()
                  .filter(dc -> dc.getCard().equals(card) && dc.getSet().equals(card.getSet()))
                  .findFirst();

          if (existingDeckCardOpt.isPresent()) {
            DeckCard existingDeckCard = existingDeckCardOpt.get();
            if (existingDeckCard.getAmount() != amount) {
              existingDeckCard.setAmount(amount);
              changed = true;
            }
            cardsToRemove.remove(existingDeckCard);
          } else {
            DeckCard newDeckCard = new DeckCard();
            newDeckCard.setCard(card);
            newDeckCard.setSet(card.getSet());
            newDeckCard.setAmount(amount);
            newDeckCard.setDeck(deck);
            deck.getCards().add(newDeckCard);
            changed = true;
          }
        }

        if (!cardsToRemove.isEmpty()) {
          deck.getCards().removeAll(cardsToRemove);
          changed = true;
        }

        if (changed) {
          decksToSave.add(deck);
        }
      }
      deckService.saveAll(decksToSave);
      log.debug(
          "Deck loading completed - {} decks processed, {} updated",
          decksFromFile.size(),
          decksToSave.size());
    } catch (IOException e) {
      log.error("Error loading decks from file", e);
    }
  }
}
