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
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCard;
import com.github.javydreamercsw.management.dto.CampaignAbilityCardDTO;
import com.github.javydreamercsw.management.service.campaign.CampaignAbilityCardService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(180)
public class CampaignAbilityCardSync implements DataSyncContributor {

  private final SeedSyncSupport syncSupport;
  private final CampaignAbilityCardService campaignAbilityCardService;
  private final ObjectMapper objectMapper;

  @Autowired
  public CampaignAbilityCardSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final CampaignAbilityCardService campaignAbilityCardService,
      final ObjectMapper objectMapper) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.campaignAbilityCardService = campaignAbilityCardService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && campaignAbilityCardService.count() > 0) {
      topUpMissingCards();
      if (syncSupport.isVerify()) {
        reconcileVerifiedCards();
      }
      return;
    }
    loadCards();
  }

  /**
   * Skip-gate branch: cards exist and the flag protects them. Still insert seed cards that don't
   * exist yet. Existing rows are never re-saved.
   */
  private void topUpMissingCards() {
    ClassPathResource resource = new ClassPathResource("campaign_ability_cards.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<CampaignAbilityCardDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, CampaignAbilityCard> existingByName =
          campaignAbilityCardService.findAll().stream()
              .collect(
                  Collectors.toMap(
                      c -> c.getName() + "|" + c.getAlignmentType() + "|" + c.getLevel(),
                      c -> c,
                      (a, b) -> a));
      int inserted = 0;
      for (CampaignAbilityCardDTO dto : dtos) {
        String key = dto.getName() + "|" + dto.getAlignmentType() + "|" + dto.getLevel();
        if (existingByName.containsKey(key)) {
          continue;
        }
        CampaignAbilityCard created = applyDto(dto);
        created.setSeedFingerprint(cardFingerprint(created));
        campaignAbilityCardService.save(created);
        inserted++;
      }
      if (inserted > 0) {
        log.info("Campaign ability card top-up: inserting {} missing seed card(s).", inserted);
      }
    } catch (IOException e) {
      log.error("Error loading campaign ability cards from file", e);
    }
  }

  private void loadCards() {
    ClassPathResource resource = new ClassPathResource("campaign_ability_cards.json");
    if (resource.exists()) {
      log.debug("Loading campaign ability cards from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<CampaignAbilityCardDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        for (CampaignAbilityCardDTO dto : dtos) {
          CampaignAbilityCard card = applyDto(dto);
          card.setSeedFingerprint(cardFingerprint(card));
          campaignAbilityCardService.save(card);
          log.debug("Loaded campaign ability card: {}", dto.getName());
        }
        log.debug("Campaign ability card loading completed - {} cards loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading campaign ability cards from file", e);
      }
    } else {
      log.warn("Campaign ability cards file not found: {}", resource.getPath());
    }
  }

  /** Creates or updates through the service (which saves) and returns the row. */
  private CampaignAbilityCard applyDto(CampaignAbilityCardDTO dto) {
    return campaignAbilityCardService.createOrUpdateCard(
        dto.getName(),
        dto.getDescription(),
        dto.getAlignmentType(),
        dto.getLevel(),
        dto.isOneTimeUse(),
        dto.getTiming(),
        dto.getEffectScript(),
        dto.getSecondaryEffectScript(),
        dto.isSecondaryOneTimeUse(),
        dto.getSecondaryTiming());
  }

  /**
   * Canonical seed-owned field values for an ability card row, in fingerprint order. The lookup key
   * (name, alignmentType, level) is excluded; the rest mirrors {@code createOrUpdateCard}'s write
   * set.
   */
  private String cardFingerprint(CampaignAbilityCard card) {
    return syncSupport.fingerprint(
        card.getDescription(),
        card.isOneTimeUse(),
        card.getTiming() == null ? null : card.getTiming().name(),
        card.getEffectScript(),
        card.getSecondaryEffectScript(),
        card.isSecondaryOneTimeUse(),
        card.getSecondaryTiming() == null ? null : card.getSecondaryTiming().name());
  }

  /**
   * Verify mode: reconcile seed-owned fields of existing ability cards. The stored fingerprint is
   * the hash of the row's seed-owned fields as the last seed write left them; re-hashing the row's
   * current fields detects hand edits. A row that still hashes to its stamp is clean and adopts
   * seed changes; a row whose fields drifted is user-edited — warned and preserved. Cards with no
   * seed entry (keyed by name|alignment|level) are never considered.
   */
  private void reconcileVerifiedCards() {
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("campaign_ability_cards.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<CampaignAbilityCardDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, CampaignAbilityCard> existingByName =
          campaignAbilityCardService.findAll().stream()
              .collect(
                  Collectors.toMap(
                      c -> c.getName() + "|" + c.getAlignmentType() + "|" + c.getLevel(),
                      c -> c,
                      (a, b) -> a));
      int reconciled = 0;
      int preserved = 0;
      for (CampaignAbilityCardDTO dto : dtos) {
        String key = dto.getName() + "|" + dto.getAlignmentType() + "|" + dto.getLevel();
        CampaignAbilityCard card = existingByName.get(key);
        if (card == null) {
          continue; // user-created rows have no seed match and are never touched
        }
        String current = cardFingerprint(card);
        boolean matchesSeed =
            Objects.equals(card.getDescription(), dto.getDescription())
                && card.isOneTimeUse() == dto.isOneTimeUse()
                && Objects.equals(
                    card.getTiming() == null ? null : card.getTiming().name(),
                    dto.getTiming() == null ? null : dto.getTiming().name())
                && Objects.equals(card.getEffectScript(), dto.getEffectScript())
                && Objects.equals(card.getSecondaryEffectScript(), dto.getSecondaryEffectScript())
                && card.isSecondaryOneTimeUse() == dto.isSecondaryOneTimeUse()
                && Objects.equals(
                    card.getSecondaryTiming() == null ? null : card.getSecondaryTiming().name(),
                    dto.getSecondaryTiming() == null ? null : dto.getSecondaryTiming().name());
        if (SeedSyncSupport.isBlankFingerprint(card.getSeedFingerprint())) {
          // Legacy row: adopt only if its seed-owned fields already equal the seed.
          if (matchesSeed) {
            card.setSeedFingerprint(current);
            campaignAbilityCardService.save(card);
            reconciled++;
          } else {
            log.warn(
                "Seed verify: campaign ability card '{}' has hand-edited seed-owned fields "
                    + "(legacy row, no fingerprint); preserving user values.",
                card.getName());
            preserved++;
          }
        } else if (current.equals(card.getSeedFingerprint())) {
          if (matchesSeed) {
            continue; // already in sync
          }
          CampaignAbilityCard refreshed = applyDto(dto);
          refreshed.setSeedFingerprint(cardFingerprint(refreshed));
          campaignAbilityCardService.save(refreshed);
          reconciled++;
        } else {
          log.warn(
              "Seed verify: campaign ability card '{}' was modified after seeding (fingerprint "
                  + "mismatch); preserving user values.",
              card.getName());
          preserved++;
        }
      }
      log.info(
          "{} ({} ms)",
          syncSupport.reconcileLog("campaign ability card", reconciled, preserved),
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during campaign ability card verify reconcile", e);
    }
  }
}
