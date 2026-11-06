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
import com.github.javydreamercsw.management.domain.show.segment.RingsideAction;
import com.github.javydreamercsw.management.dto.RingsideActionDTO;
import com.github.javydreamercsw.management.dto.RingsideActionTypeDTO;
import com.github.javydreamercsw.management.service.ringside.RingsideActionDataService;
import java.io.IOException;
import java.util.ArrayList;
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
@Order(240)
public class RingsideActionSync implements DataSyncContributor {

  private final SeedSyncSupport syncSupport;
  private final RingsideActionDataService ringsideActionDataService;
  private final ObjectMapper objectMapper;

  @Autowired
  public RingsideActionSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final RingsideActionDataService ringsideActionDataService,
      final ObjectMapper objectMapper) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.ringsideActionDataService = ringsideActionDataService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    // Types always flow through their own skip gate inside syncTypes() (FULL re-syncs them).
    syncTypes();
    if (syncSupport.mode() != SeedSyncMode.FULL && ringsideActionDataService.countActions() > 0) {
      topUpMissingActions();
      if (syncSupport.isVerify()) {
        reconcileVerifiedActions();
      }
      return;
    }
    syncActions();
  }

  private void syncTypes() {
    if (syncSupport.mode() != SeedSyncMode.FULL && ringsideActionDataService.countTypes() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("ringside_action_types.json");
    if (resource.exists()) {
      log.debug("Loading ringside action types from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<RingsideActionTypeDTO> dtos =
            objectMapper.readValue(is, new TypeReference<List<RingsideActionTypeDTO>>() {});
        for (RingsideActionTypeDTO dto : dtos) {
          ringsideActionDataService.createOrUpdateType(
              dto.getName(),
              dto.isIncreasesAwareness(),
              dto.isCanCauseDq(),
              dto.getBaseRiskMultiplier());
          log.debug("Loaded ringside action type: {}", dto.getName());
        }
        log.debug("Ringside action type loading completed - {} types loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading ringside action types from file", e);
      }
    }
  }

  /**
   * Skip-gate branch: actions exist and the flag protects them. Still insert seed actions that
   * don't exist yet. Existing rows are never re-saved.
   */
  private void topUpMissingActions() {
    ClassPathResource resource = new ClassPathResource("ringside_actions.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<RingsideActionDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, RingsideAction> existingByName =
          ringsideActionDataService.findAllActionsUnfiltered().stream()
              .collect(Collectors.toMap(RingsideAction::getName, a -> a, (a, b) -> a));
      int inserted = 0;
      for (RingsideActionDTO dto : dtos) {
        if (existingByName.containsKey(dto.getName())) {
          continue;
        }
        RingsideAction created =
            ringsideActionDataService.createOrUpdateAction(
                dto.getName(),
                dto.getType(),
                dto.getDescription(),
                dto.getImpact(),
                dto.getRisk(),
                dto.getAlignment(),
                dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME");
        created.setSeedFingerprint(actionFingerprint(created));
        ringsideActionDataService.saveAction(created);
        inserted++;
      }
      if (inserted > 0) {
        log.info("Ringside action top-up: inserting {} missing seed action(s).", inserted);
      }
    } catch (IOException e) {
      log.error("Error loading ringside actions from file", e);
    }
  }

  private void syncActions() {
    if (syncSupport.mode() != SeedSyncMode.FULL && ringsideActionDataService.countActions() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("ringside_actions.json");
    if (resource.exists()) {
      log.debug("Loading ringside actions from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<RingsideActionDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        for (RingsideActionDTO dto : dtos) {
          RingsideAction action =
              ringsideActionDataService.createOrUpdateAction(
                  dto.getName(),
                  dto.getType(),
                  dto.getDescription(),
                  dto.getImpact(),
                  dto.getRisk(),
                  dto.getAlignment(),
                  dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME");
          action.setSeedFingerprint(actionFingerprint(action));
          ringsideActionDataService.saveAction(action);
          log.debug("Loaded ringside action: {}", dto.getName());
        }
        log.debug("Ringside action loading completed - {} actions loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading ringside actions from file", e);
      }
    }
  }

  /**
   * Canonical seed-owned field values for a ringside action row, in fingerprint order: type name,
   * description, impact, risk, alignment, expansionCode. Mirrors the write set of {@code
   * createOrUpdateAction} minus the name key. Public for the seed-provenance manifest generator,
   * which re-hashes fresh seed rows through the owning sync's own method.
   */
  public String actionFingerprint(RingsideAction action) {
    return syncSupport.fingerprint(
        action.getType() == null ? null : action.getType().getName(),
        action.getDescription(),
        action.getImpact(),
        action.getRisk(),
        action.getAlignment() == null ? null : action.getAlignment().name(),
        action.getExpansionCode());
  }

  /**
   * Verify mode: reconcile seed-owned fields of existing ringside actions. The stored fingerprint
   * is the hash of the row's seed-owned fields as the last seed write left them; re-hashing the
   * row's current fields detects hand edits. A row that still hashes to its stamp is clean and
   * adopts seed changes; a row whose fields drifted is user-edited — warned and preserved. Actions
   * with no seed entry are never considered.
   */
  private void reconcileVerifiedActions() {
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("ringside_actions.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<RingsideActionDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, RingsideAction> existingByName =
          ringsideActionDataService.findAllActionsUnfiltered().stream()
              .collect(Collectors.toMap(RingsideAction::getName, a -> a, (a, b) -> a));
      int reconciled = 0;
      int preserved = 0;
      List<RingsideAction> toSave = new ArrayList<>();
      for (RingsideActionDTO dto : dtos) {
        RingsideAction action = existingByName.get(dto.getName());
        if (action == null) {
          continue; // user-created rows have no seed match and are never touched
        }
        String current = actionFingerprint(action);
        boolean matchesSeed =
            Objects.equals(
                    action.getType() == null ? null : action.getType().getName(), dto.getType())
                && Objects.equals(action.getDescription(), dto.getDescription())
                && action.getImpact() == dto.getImpact()
                && action.getRisk() == dto.getRisk()
                && Objects.equals(
                    action.getAlignment() == null ? null : action.getAlignment().name(),
                    dto.getAlignment() == null ? "NEUTRAL" : dto.getAlignment().name())
                && Objects.equals(
                    action.getExpansionCode(),
                    dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME");
        if (SeedSyncSupport.isBlankFingerprint(action.getSeedFingerprint())) {
          // Legacy row: adopt only if its seed-owned fields already equal the seed.
          if (matchesSeed) {
            action.setSeedFingerprint(current);
            toSave.add(action);
            reconciled++;
          } else {
            log.warn(
                "Seed verify: ringside action '{}' has hand-edited seed-owned fields (legacy "
                    + "row, no fingerprint); preserving user values.",
                action.getName());
            preserved++;
          }
        } else if (current.equals(action.getSeedFingerprint())) {
          if (matchesSeed) {
            continue; // already in sync
          }
          RingsideAction refreshed =
              ringsideActionDataService.createOrUpdateAction(
                  dto.getName(),
                  dto.getType(),
                  dto.getDescription(),
                  dto.getImpact(),
                  dto.getRisk(),
                  dto.getAlignment(),
                  dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME");
          refreshed.setSeedFingerprint(actionFingerprint(refreshed));
          ringsideActionDataService.saveAction(refreshed);
          reconciled++;
        } else {
          log.warn(
              "Seed verify: ringside action '{}' was modified after seeding (fingerprint "
                  + "mismatch); preserving user values.",
              action.getName());
          preserved++;
        }
      }
      for (RingsideAction action : toSave) {
        ringsideActionDataService.saveAction(action);
      }
      log.info(
          "{} ({} ms)",
          syncSupport.reconcileLog("ringside action", reconciled, preserved),
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during ringside action verify reconcile", e);
    }
  }
}
