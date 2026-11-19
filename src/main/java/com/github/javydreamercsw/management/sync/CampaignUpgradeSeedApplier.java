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

import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignUpgrade;
import com.github.javydreamercsw.management.domain.campaign.CampaignUpgradeRepository;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to campaign upgrade rows (seed-update review
 * feature). The entity is resolved through the decision row's {@code entity_key}, reminted exactly
 * the way detection minted it ({@code campaign_upgrade:NAME-slug} via {@link
 * CampaignUpgradeSync#entityKeyFor}) — the name is the domain's stable identity, and key-based
 * reminting keeps resolution rename-proof if seed naming ever moves; the {@code
 * entity_display_name} stays the exact seed name for the review UI and is never slug-reversed.
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class CampaignUpgradeSeedApplier implements SeedDomainApplier {

  private final CampaignUpgradeRepository upgradeRepository;
  private final CampaignStateRepository campaignStateRepository;

  public CampaignUpgradeSeedApplier(
      final CampaignUpgradeRepository upgradeRepository,
      final CampaignStateRepository campaignStateRepository) {
    this.upgradeRepository = upgradeRepository;
    this.campaignStateRepository = campaignStateRepository;
  }

  @Override
  public String domainKey() {
    return CampaignUpgradeSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    findUpgrade(decision)
        .ifPresentOrElse(
            upgrade -> stamp(upgrade, decision),
            () ->
                log.warn(
                    "Seed decision {}: campaign upgrade '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    findUpgrade(decision)
        .ifPresentOrElse(
            upgrade -> {
              applyFieldValue(upgrade, decision);
              stamp(upgrade, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: campaign upgrade '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    findUpgrade(decision)
        .ifPresentOrElse(
            upgrade -> {
              upgrade.setSeedFingerprint(null);
              upgradeRepository.save(upgrade);
            },
            () ->
                log.warn(
                    "Seed decision {}: campaign upgrade '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (campaign states holding
    // the upgrade as owned). The fail-safe direction re-checks at delete time: an owned upgrade is
    // kept as custom data rather than blowing the campaign_state_upgrades join FK mid-apply.
    findUpgrade(decision)
        .ifPresentOrElse(
            upgrade -> {
              if (hasDependents(upgrade)) {
                log.warn(
                    "Seed decision {}: campaign upgrade '{}' is still owned by campaign states —"
                        + " the row is kept as custom data",
                    decision.getId(),
                    decision.getEntityDisplayName());
              } else {
                upgradeRepository.delete(upgrade);
              }
            },
            () ->
                log.warn(
                    "Seed decision {}: campaign upgrade '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous, so match the key exactly
    // the way detection minted it.
    return findUpgradeByKey(entityKey).map(this::hasDependents).orElse(false);
  }

  /**
   * The upgrade's REAL persisted FK relationship (verified against the entity model): the {@code
   * campaign_state_upgrades} join table ({@code CampaignState.upgrades} M2M, EAGER).
   */
  private boolean hasDependents(CampaignUpgrade upgrade) {
    return campaignStateRepository.findAll().stream()
        .anyMatch(state -> state.getUpgrades().contains(upgrade));
  }

  /** Resolves the decision's campaign upgrade by reminting its entity key. */
  private Optional<CampaignUpgrade> findUpgrade(SeedUpdateDecision decision) {
    return findUpgradeByKey(decision.getEntityKey());
  }

  private Optional<CampaignUpgrade> findUpgradeByKey(String entityKey) {
    return upgradeRepository.findAll().stream()
        .filter(u -> entityKey.equals(CampaignUpgradeSync.entityKeyFor(u.getName())))
        .findFirst();
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(CampaignUpgrade upgrade, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "description" -> upgrade.setDescription(value);
      case "type" -> upgrade.setType(value);
      case "subType" -> upgrade.setSubType(value);
      default ->
          throw new IllegalStateException(
              "Campaign upgrade field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    upgradeRepository.save(upgrade);
  }

  private void stamp(CampaignUpgrade upgrade, SeedUpdateDecision decision) {
    upgrade.setSeedFingerprint(decision.getNewFingerprint());
    upgradeRepository.save(upgrade);
  }
}
