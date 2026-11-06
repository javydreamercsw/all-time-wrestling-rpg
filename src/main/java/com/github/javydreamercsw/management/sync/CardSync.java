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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
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

  private final SeedSyncSupport syncSupport;
  private final CardService cardService;
  private final CardSetService cardSetService;
  private final ResourcePatternResolver resourcePatternResolver;
  private final ObjectMapper objectMapper;

  @Autowired
  public CardSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final CardService cardService,
      final CardSetService cardSetService,
      final ResourcePatternResolver resourcePatternResolver,
      final ObjectMapper objectMapper) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.cardService = cardService;
    this.cardSetService = cardSetService;
    this.resourcePatternResolver = resourcePatternResolver;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && cardService.count() > 0) {
      topUpMissingCards();
      if (syncSupport.isVerify()) {
        reconcileVerifiedCards();
      }
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

      List<Card> allMissing = new ArrayList<>();
      for (Resource resource : resources) {
        if (!resource.exists()) {
          continue;
        }
        log.debug("Loading cards from file: {}", resource.getFilename());
        try (var is = resource.getInputStream()) {
          List<CardDTO> cardsFromFile = objectMapper.readValue(is, new TypeReference<>() {});
          List<Card> fileToSave = new ArrayList<>();
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
              Card newCard = fromDto(dto, set);
              newCard.setSeedFingerprint(cardFingerprint(dto));
              fileToSave.add(newCard);
              allMissing.add(newCard);
            } else if (updateExisting) {
              applyDto(card, dto, set);
              card.setSeedFingerprint(cardFingerprint(card));
              fileToSave.add(card);
            }
          }
          if (updateExisting && !fileToSave.isEmpty()) {
            cardService.saveAll(fileToSave);
          }
          log.debug(
              "Processed {} cards from {} ({} new)",
              cardsFromFile.size(),
              resource.getFilename(),
              fileToSave.size());
        } catch (IOException e) {
          log.error("Error loading cards from file: {}", resource.getFilename(), e);
        }
      }
      return allMissing;
    } catch (IOException e) {
      log.error("Error resolving card resources", e);
      return List.of();
    }
  }

  private static Card fromDto(@NonNull CardDTO dto, @NonNull CardSet set) {
    Card card = new Card();
    applyDto(card, dto, set);
    return card;
  }

  /** Fingerprint over a card's seed-owned fields in canonical order. */
  private String cardFingerprint(CardDTO dto) {
    return syncSupport.fingerprint(
        dto.getName(),
        dto.getDamage(),
        dto.getStamina(),
        dto.getTarget(),
        dto.getMomentum(),
        dto.getType(),
        dto.isSignature(),
        dto.isFinisher(),
        dto.isTaunt(),
        dto.isRecover(),
        dto.isPin());
  }

  /**
   * Fingerprint over a card row's current seed-owned fields, in the same canonical order. Public
   * for the seed-provenance manifest generator, which re-hashes fresh seed rows through the owning
   * sync's own method.
   */
  public String cardFingerprint(Card card) {
    return syncSupport.fingerprint(
        card.getName(),
        card.getDamage(),
        card.getStamina(),
        card.getTarget(),
        card.getMomentum(),
        card.getType(),
        card.getSignature(),
        card.getFinisher(),
        card.getTaunt(),
        card.getRecover(),
        card.getPin());
  }

  private static void applyDto(@NonNull Card card, @NonNull CardDTO dto, @NonNull CardSet set) {
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

  /**
   * Verify mode: reconcile seed-owned fields of existing cards keyed by set#number. The stored
   * fingerprint is the hash of the row's seed-owned fields as the last seed write left them;
   * re-hashing the row's current fields detects hand edits. A row that still hashes to its stored
   * fingerprint is clean and adopts seed changes; a row whose fields drifted is user-edited —
   * warned and preserved. Cards in user-created sets have no seed match and are never touched.
   */
  private void reconcileVerifiedCards() {
    long start = System.currentTimeMillis();
    try {
      Resource[] resources = resourcePatternResolver.getResources("classpath*:cards/*.json");
      Map<String, Card> existing =
          cardService.findAll().stream()
              .collect(
                  Collectors.toMap(
                      c -> c.getSet().getCode() + "#" + c.getNumber(), c -> c, (a, b) -> a));
      Map<String, CardSet> setCache = new HashMap<>();
      int reconciled = 0;
      int preserved = 0;
      List<Card> toSave = new ArrayList<>();
      for (Resource resource : resources) {
        if (!resource.exists()) {
          continue;
        }
        try (var is = resource.getInputStream()) {
          List<CardDTO> cardsFromFile = objectMapper.readValue(is, new TypeReference<>() {});
          for (CardDTO dto : cardsFromFile) {
            CardSet set = setCache.get(dto.getSet());
            if (set == null) {
              Optional<CardSet> setOpt = cardSetService.findBySetCode(dto.getSet());
              if (setOpt.isEmpty()) {
                continue;
              }
              set = setOpt.get();
              setCache.put(dto.getSet(), set);
            }
            String key = dto.getSet() + "#" + dto.getNumber();
            Card card = existing.get(key);
            if (card == null) {
              continue; // user-created rows have no seed match and are never touched
            }
            String current = cardFingerprint(card);
            if (SeedSyncSupport.isBlankFingerprint(card.getSeedFingerprint())) {
              if (matchesSeed(card, dto, set)) {
                card.setSeedFingerprint(current);
                toSave.add(card);
                reconciled++;
              } else {
                log.warn(
                    "Seed verify: card {}#{} has hand-edited seed-owned fields "
                        + "(legacy row, no fingerprint); preserving user values.",
                    dto.getSet(),
                    dto.getNumber());
                preserved++;
              }
            } else if (current.equals(card.getSeedFingerprint())) {
              if (matchesSeed(card, dto, set)) {
                continue; // already in sync
              }
              applyDto(card, dto, set);
              card.setSeedFingerprint(cardFingerprint(card));
              toSave.add(card);
              reconciled++;
            } else {
              log.warn(
                  "Seed verify: card {}#{} was modified after seeding (fingerprint mismatch); "
                      + "preserving user values.",
                  dto.getSet(),
                  dto.getNumber());
              preserved++;
            }
          }
        } catch (IOException e) {
          log.error("Error loading cards from file: {}", resource.getFilename(), e);
        }
      }
      if (!toSave.isEmpty()) {
        cardService.saveAll(toSave);
      }
      log.info(
          "{} ({} ms)",
          syncSupport.reconcileLog("card", reconciled, preserved),
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error resolving card resources for verify reconcile", e);
    }
  }

  private boolean matchesSeed(Card card, CardDTO dto, CardSet set) {
    return Objects.equals(card.getName(), dto.getName())
        && Objects.equals(card.getDamage(), dto.getDamage())
        && Objects.equals(card.getStamina(), dto.getStamina())
        && Objects.equals(card.getTarget(), dto.getTarget())
        && Objects.equals(card.getMomentum(), dto.getMomentum())
        && Objects.equals(card.getType(), dto.getType())
        && card.getSignature() == dto.isSignature()
        && card.getFinisher() == dto.isFinisher()
        && card.getTaunt() == dto.isTaunt()
        && card.getRecover() == dto.isRecover()
        && card.getPin() == dto.isPin()
        && card.getSet().getId().equals(set.getId());
  }
}
