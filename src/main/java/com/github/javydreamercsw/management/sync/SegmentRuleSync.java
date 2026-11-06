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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.dto.SegmentRuleDTO;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
@Order(30)
public class SegmentRuleSync implements DataSyncContributor {

  private final SeedSyncSupport syncSupport;
  private final SegmentRuleService segmentRuleService;
  private final ObjectMapper objectMapper;

  @Autowired
  public SegmentRuleSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final SegmentRuleService segmentRuleService,
      final ObjectMapper objectMapper) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.segmentRuleService = segmentRuleService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    ClassPathResource resource = new ClassPathResource("segment_rules.json");
    if (resource.exists()) {
      log.debug("Loading segment rules from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<SegmentRuleDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        if (syncSupport.isVerify()) {
          // The one entity where VERIFY tightens an existing unconditional reconcile: only
          // fingerprint-clean rows go through the service; edited rows are warned and preserved.
          reconcileVerified(dtos);
          return;
        }
        // FULL path unchanged: unconditional reconcile, now with fingerprint stamping.
        for (SegmentRuleDTO dto : dtos) {
          SegmentRule saved =
              segmentRuleService.createOrUpdateRule(
                  dto.getName(),
                  dto.getDescription(),
                  dto.isRequiresHighHeat(),
                  dto.isNoDq(),
                  dto.getBumpAddition(),
                  dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME",
                  dto.getGuide(),
                  dto.isAllowsRefereeStopage());
          String before = saved.getSeedFingerprint();
          stampFingerprint(saved, dto);
          if (!Objects.equals(before, saved.getSeedFingerprint())) {
            segmentRuleService.saveRule(saved);
          }
        }
        log.debug("Segment rule loading completed - {} rules loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading segment rules from file", e);
      }
    } else {
      log.warn("Segment rules file not found: {}", resource.getPath());
    }
  }

  /**
   * Canonical seed-owned field values for a segment rule row, in fingerprint order — the {@code
   * createOrUpdateRule} write set. {@code active} is admin-managed and excluded. Public for the
   * seed-provenance manifest generator, which re-hashes fresh seed rows through the owning sync's
   * own method.
   */
  public String ruleFingerprint(SegmentRule sr) {
    return syncSupport.fingerprint(
        sr.getDescription(),
        sr.getRequiresHighHeat(),
        sr.getNoDq(),
        sr.isAllowsRefereeStopage(),
        sr.getBumpAddition() == null ? null : sr.getBumpAddition().name(),
        sr.getExpansionCode(),
        sr.getGuideHash());
  }

  /** Re-stamps a saved rule from the DTO's guide hash (the service leaves the stamp unset). */
  private void stampFingerprint(SegmentRule sr, SegmentRuleDTO dto) {
    sr.setSeedFingerprint(
        syncSupport.fingerprint(
            sr.getDescription(),
            sr.getRequiresHighHeat(),
            sr.getNoDq(),
            sr.isAllowsRefereeStopage(),
            sr.getBumpAddition() == null ? null : sr.getBumpAddition().name(),
            sr.getExpansionCode(),
            guideHashFor(dto)));
  }

  /** Guide hash the seed write would compute for a DTO (mirrors the service's private method). */
  private String guideHashFor(SegmentRuleDTO dto) {
    if (dto.getGuide() == null) {
      return null;
    }
    try {
      String json = objectMapper.writeValueAsString(dto.getGuide());
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (JsonProcessingException | NoSuchAlgorithmException e) {
      log.warn("Failed to compute seed guide hash for segment rule", e);
      return null;
    }
  }

  /**
   * Verify mode: reconcile seed-owned fields of existing segment rules keyed by name. The stored
   * fingerprint is the hash of the row's seed-owned fields as the last seed write left them;
   * re-hashing the row's current fields detects hand edits. A row that still hashes to its stamp is
   * clean and adopts seed changes through the same service path; a row whose fields drifted is
   * user-edited — warned and preserved. Rules with no seed entry (user-created) are never
   * considered.
   */
  private void reconcileVerified(List<SegmentRuleDTO> dtos) {
    long start = System.currentTimeMillis();
    Map<String, SegmentRule> existingByName =
        segmentRuleService.findAllForAdmin().stream()
            .collect(Collectors.toMap(SegmentRule::getName, sr -> sr, (a, b) -> a));
    int reconciled = 0;
    int preserved = 0;
    for (SegmentRuleDTO dto : dtos) {
      SegmentRule sr = existingByName.get(dto.getName());
      if (sr == null) {
        continue; // user-created rows have no seed match and are never touched
      }
      String current = ruleFingerprint(sr);
      String seedHash = guideHashFor(dto);
      boolean matchesSeed =
          Objects.equals(sr.getDescription(), dto.getDescription())
              && sr.getRequiresHighHeat() == dto.isRequiresHighHeat()
              && sr.getNoDq() == dto.isNoDq()
              && sr.isAllowsRefereeStopage() == dto.isAllowsRefereeStopage()
              && sr.getBumpAddition() == dto.getBumpAddition()
              && Objects.equals(
                  sr.getExpansionCode(),
                  dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME")
              && Objects.equals(sr.getGuideHash(), seedHash);
      if (SeedSyncSupport.isBlankFingerprint(sr.getSeedFingerprint())) {
        // Legacy row: adopt only if its seed-owned fields already equal the seed.
        if (matchesSeed) {
          SegmentRule saved =
              segmentRuleService.createOrUpdateRule(
                  dto.getName(),
                  dto.getDescription(),
                  dto.isRequiresHighHeat(),
                  dto.isNoDq(),
                  dto.getBumpAddition(),
                  dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME",
                  dto.getGuide(),
                  dto.isAllowsRefereeStopage());
          String before = saved.getSeedFingerprint();
          stampFingerprint(saved, dto);
          if (!Objects.equals(before, saved.getSeedFingerprint())) {
            segmentRuleService.saveRule(saved);
          }
          reconciled++;
        } else {
          log.warn(
              "Seed verify: segment rule '{}' has hand-edited seed-owned fields (legacy row, "
                  + "no fingerprint); preserving user values.",
              sr.getName());
          preserved++;
        }
      } else if (current.equals(sr.getSeedFingerprint())) {
        if (matchesSeed) {
          continue; // already in sync
        }
        SegmentRule saved =
            segmentRuleService.createOrUpdateRule(
                dto.getName(),
                dto.getDescription(),
                dto.isRequiresHighHeat(),
                dto.isNoDq(),
                dto.getBumpAddition(),
                dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME",
                dto.getGuide(),
                dto.isAllowsRefereeStopage());
        String before = saved.getSeedFingerprint();
        stampFingerprint(saved, dto);
        if (!Objects.equals(before, saved.getSeedFingerprint())) {
          segmentRuleService.saveRule(saved);
        }
        reconciled++;
      } else {
        log.warn(
            "Seed verify: segment rule '{}' was modified after seeding (fingerprint mismatch); "
                + "preserving user values.",
            sr.getName());
        preserved++;
      }
    }
    log.info(
        "{} ({} ms)",
        syncSupport.reconcileLog("segment rule", reconciled, preserved),
        System.currentTimeMillis() - start);
  }
}
