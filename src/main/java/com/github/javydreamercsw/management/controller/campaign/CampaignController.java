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
package com.github.javydreamercsw.management.controller.campaign;

import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignUpgrade;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.campaign.CampaignUpgradeService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/campaign")
@RequiredArgsConstructor
public class CampaignController {

  private final CampaignService campaignService;
  private final CampaignRepository campaignRepository;
  private final WrestlerRepository wrestlerRepository;
  private final CampaignUpgradeService upgradeService;

  @GetMapping("/{wrestlerId}/state")
  public ResponseEntity<CampaignState> getCampaignState(@PathVariable Long wrestlerId) {
    Optional<Wrestler> wrestler = wrestlerRepository.findById(wrestlerId);
    if (wrestler.isEmpty()) return ResponseEntity.notFound().build();

    Optional<Campaign> campaign = campaignRepository.findActiveByWrestler(wrestler.get());
    return campaign
        .map(c -> ResponseEntity.ok(c.getState()))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/upgrades")
  public ResponseEntity<List<CampaignUpgrade>> getAllUpgrades() {
    return ResponseEntity.ok(upgradeService.getAllUpgrades());
  }

  @PostMapping("/{wrestlerId}/upgrades/purchase")
  public ResponseEntity<Void> purchaseUpgrade(
      @PathVariable Long wrestlerId, @RequestParam Long upgradeId) {
    Optional<Wrestler> wrestler = wrestlerRepository.findById(wrestlerId);
    if (wrestler.isEmpty()) return ResponseEntity.notFound().build();

    Optional<Campaign> campaign = campaignRepository.findActiveByWrestler(wrestler.get());
    if (campaign.isEmpty()) return ResponseEntity.notFound().build();

    upgradeService.purchaseUpgrade(campaign.get(), upgradeId);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{wrestlerId}/test/process-match")
  public ResponseEntity<Void> processMatchResult(
      @PathVariable Long wrestlerId, @RequestParam boolean won) {
    Optional<Wrestler> wrestler = wrestlerRepository.findById(wrestlerId);
    if (wrestler.isEmpty()) return ResponseEntity.notFound().build();

    Optional<Campaign> campaign = campaignRepository.findActiveByWrestler(wrestler.get());
    if (campaign.isEmpty()) return ResponseEntity.notFound().build();

    campaignService.processMatchResult(campaign.get(), won);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{wrestlerId}/test/advance-chapter")
  public ResponseEntity<Void> advanceChapter(@PathVariable Long wrestlerId) {
    Optional<Wrestler> wrestler = wrestlerRepository.findById(wrestlerId);
    if (wrestler.isEmpty()) return ResponseEntity.notFound().build();

    Optional<Campaign> campaign = campaignRepository.findActiveByWrestler(wrestler.get());
    if (campaign.isEmpty()) return ResponseEntity.notFound().build();

    campaignService.advanceChapter(campaign.get());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{wrestlerId}/test/skip-to-show")
  public ResponseEntity<Void> skipToShow(@PathVariable Long wrestlerId) {
    Optional<Wrestler> wrestler = wrestlerRepository.findById(wrestlerId);
    if (wrestler.isEmpty()) return ResponseEntity.notFound().build();

    Optional<Campaign> campaign = campaignRepository.findActiveByWrestler(wrestler.get());
    if (campaign.isEmpty()) return ResponseEntity.notFound().build();

    campaignService.completePostMatch(campaign.get());
    return ResponseEntity.ok().build();
  }
}
