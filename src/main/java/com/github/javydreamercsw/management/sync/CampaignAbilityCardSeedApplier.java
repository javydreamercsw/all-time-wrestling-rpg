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

import com.github.javydreamercsw.management.domain.campaign.AbilityTiming;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCard;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCardRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to campaign ability card rows (seed-update review
 * feature). The entity is resolved through the decision row's {@code entity_key}, reminted exactly
 * the way detection minted it ({@code campaign_ability_card:NAME|ALIGN|LEVEL} via {@link
 * CampaignAbilityCardSync#entityKeyFor}) — card names are NOT unique across the alignment/level
 * grid, so the composite key is the only unambiguous handle (the same key-based resolution the card
 * domain uses for its set#number rows); the {@code entity_display_name} stays the seed name for the
 * review UI.
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class CampaignAbilityCardSeedApplier implements SeedDomainApplier {

  private final CampaignAbilityCardRepository campaignAbilityCardRepository;
  private final CampaignStateRepository campaignStateRepository;

  public CampaignAbilityCardSeedApplier(
      final CampaignAbilityCardRepository campaignAbilityCardRepository,
      final CampaignStateRepository campaignStateRepository) {
    this.campaignAbilityCardRepository = campaignAbilityCardRepository;
    this.campaignStateRepository = campaignStateRepository;
  }

  @Override
  public String domainKey() {
    return CampaignAbilityCardSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    findCard(decision)
        .ifPresentOrElse(
            card -> stamp(card, decision),
            () ->
                log.warn(
                    "Seed decision {}: campaign ability card '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    findCard(decision)
        .ifPresentOrElse(
            card -> {
              applyFieldValue(card, decision);
              stamp(card, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: campaign ability card '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    findCard(decision)
        .ifPresentOrElse(
            card -> {
              card.setSeedFingerprint(null);
              campaignAbilityCardRepository.save(card);
            },
            () ->
                log.warn(
                    "Seed decision {}: campaign ability card '{}' not found — nothing to mark"
                        + " custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (campaign states holding
    // the card as active). The fail-safe direction re-checks at delete time: a card in a
    // campaign's active deck is kept as custom data rather than blowing the FK mid-apply.
    findCard(decision)
        .ifPresentOrElse(
            card -> {
              if (hasStates(card)) {
                log.warn(
                    "Seed decision {}: campaign ability card '{}' is in campaign states — the row"
                        + " is kept as custom data",
                    decision.getId(),
                    decision.getEntityDisplayName());
              } else {
                campaignAbilityCardRepository.delete(card);
              }
            },
            () ->
                log.warn(
                    "Seed decision {}: campaign ability card '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous, so match the key exactly
    // the way detection minted it.
    return findCardByKey(entityKey).map(this::hasStates).orElse(false);
  }

  /**
   * Resolves the decision's card by reminting its entity key — the composite name|alignment|level
   * key is rename-proof where the bare name is ambiguous (see class doc).
   */
  private Optional<CampaignAbilityCard> findCard(SeedUpdateDecision decision) {
    return findCardByKey(decision.getEntityKey());
  }

  private Optional<CampaignAbilityCard> findCardByKey(String entityKey) {
    return campaignAbilityCardRepository.findAll().stream()
        .filter(c -> entityKey.equals(CampaignAbilityCardSync.entityKeyFor(seedKeyOf(c))))
        .findFirst();
  }

  /** The composite seed key of an ability card row (the manifest's lookup key). */
  private static String seedKeyOf(CampaignAbilityCard c) {
    return c.getName() + "|" + c.getAlignmentType() + "|" + c.getLevel();
  }

  /** Whether any campaign state holds this card in its active deck (the real persisted FK). */
  private boolean hasStates(CampaignAbilityCard card) {
    return campaignStateRepository.findAll().stream()
        .anyMatch(state -> state.getActiveCards().contains(card));
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(CampaignAbilityCard card, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "description" -> card.setDescription(value);
      case "oneTimeUse" -> card.setOneTimeUse(Boolean.parseBoolean(value));
      case "timing" -> card.setTiming(value == null ? null : AbilityTiming.valueOf(value));
      case "effectScript" -> card.setEffectScript(value);
      case "secondaryEffectScript" -> card.setSecondaryEffectScript(value);
      case "secondaryOneTimeUse" -> card.setSecondaryOneTimeUse(Boolean.parseBoolean(value));
      case "secondaryTiming" ->
          card.setSecondaryTiming(value == null ? null : AbilityTiming.valueOf(value));
      default ->
          throw new IllegalStateException(
              "Campaign ability card field '"
                  + decision.getFieldName()
                  + "' is not a seed-owned field");
    }
    campaignAbilityCardRepository.save(card);
  }

  private void stamp(CampaignAbilityCard card, SeedUpdateDecision decision) {
    card.setSeedFingerprint(decision.getNewFingerprint());
    campaignAbilityCardRepository.save(card);
  }
}
