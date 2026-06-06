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
package com.github.javydreamercsw.management.service.export;

import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationshipRepository;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UniverseExportService {

  private final WrestlerStateRepository wrestlerStateRepository;
  private final InjuryRepository injuryRepository;
  private final RivalryRepository rivalryRepository;
  private final TitleReignRepository titleReignRepository;
  private final WrestlerAlignmentRepository wrestlerAlignmentRepository;
  private final WrestlerRelationshipRepository wrestlerRelationshipRepository;

  public ExportPayload collect(
      Universe universe, Set<ExportCategory> categories, WrestlerFilter filter) {
    if (categories.isEmpty()) {
      return new ExportPayload(Map.of());
    }

    List<WrestlerState> filtered =
        applyFilter(wrestlerStateRepository.findByUniverseId(universe.getId()), filter);

    Map<ExportCategory, List<Map<String, Object>>> data = new LinkedHashMap<>();
    for (ExportCategory category : categories) {
      data.put(category, collectCategory(category, universe, filtered));
    }
    return new ExportPayload(data);
  }

  private List<WrestlerState> applyFilter(List<WrestlerState> states, WrestlerFilter filter) {
    return switch (filter.scope()) {
      case ALL -> states;
      case ACTIVE_ONLY ->
          states.stream().filter(s -> Boolean.TRUE.equals(s.getWrestler().getActive())).toList();
      case MANUAL -> {
        Set<Long> ids = filter.wrestlerIds();
        yield states.stream().filter(s -> ids.contains(s.getWrestler().getId())).toList();
      }
    };
  }

  private List<Map<String, Object>> collectCategory(
      ExportCategory category, Universe universe, List<WrestlerState> states) {
    return switch (category) {
      case UNIVERSE_STATE -> collectUniverseState(states);
      case INJURIES -> collectInjuries(universe, states);
      case RIVALRIES -> collectRivalries(universe, states);
      case TITLE_REIGNS -> collectTitleReigns(states);
      case ALIGNMENTS -> collectAlignments(universe, states);
      case RELATIONSHIPS -> collectRelationships(states);
    };
  }

  private List<Map<String, Object>> collectUniverseState(List<WrestlerState> states) {
    return states.stream()
        .map(
            s -> {
              Map<String, Object> row = new LinkedHashMap<>();
              row.put("wrestler", s.getWrestler().getName());
              row.put("tier", s.getTier() != null ? s.getTier().name() : "");
              row.put("fans", s.getFans());
              row.put("bumps", s.getBumps());
              row.put("morale", s.getMorale());
              row.put("currentHealth", s.getCurrentHealth());
              row.put("managementStamina", s.getManagementStamina());
              row.put("physicalCondition", s.getPhysicalCondition());
              row.put("faction", s.getFaction() != null ? s.getFaction().getName() : "");
              return row;
            })
        .toList();
  }

  private List<Map<String, Object>> collectInjuries(Universe universe, List<WrestlerState> states) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (WrestlerState state : states) {
      for (var injury : injuryRepository.findByWrestlerAndUniverse(state.getWrestler(), universe)) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("wrestler", state.getWrestler().getName());
        row.put("name", injury.getName());
        row.put("severity", injury.getSeverity().name());
        row.put("healthPenalty", injury.getHealthPenalty());
        row.put("staminaPenalty", injury.getStaminaPenalty());
        row.put("handSizePenalty", injury.getHandSizePenalty());
        row.put("isActive", injury.getIsActive());
        row.put("injuryDate", injury.getInjuryDate());
        row.put("healedDate", injury.getHealedDate() != null ? injury.getHealedDate() : "");
        rows.add(row);
      }
    }
    return rows;
  }

  private List<Map<String, Object>> collectRivalries(
      Universe universe, List<WrestlerState> states) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (var r : rivalryRepository.findByUniverseWithWrestlers(universe)) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("wrestler1", r.getWrestler1().getName());
      row.put("wrestler2", r.getWrestler2().getName());
      row.put("heat", r.getHeat());
      row.put("heatEvents", r.getHeatEvents().size());
      row.put("isActive", r.getIsActive());
      row.put("startedDate", r.getStartedDate());
      row.put("endedDate", r.getEndedDate() != null ? r.getEndedDate() : "");
      row.put("storylineNotes", r.getStorylineNotes() != null ? r.getStorylineNotes() : "");
      rows.add(row);
    }
    return rows;
  }

  private List<Map<String, Object>> collectTitleReigns(List<WrestlerState> states) {
    Set<Long> seenReignIds = new HashSet<>();
    List<Map<String, Object>> rows = new ArrayList<>();
    for (WrestlerState state : states) {
      for (var reign : titleReignRepository.findByChampionsContaining(state.getWrestler())) {
        if (seenReignIds.add(reign.getId())) {
          String champions =
              reign.getChampions().stream().map(w -> w.getName()).collect(Collectors.joining(", "));
          Map<String, Object> row = new LinkedHashMap<>();
          row.put("title", reign.getTitle().getName());
          row.put("champions", champions);
          row.put("reignNumber", reign.getReignNumber());
          row.put("startDate", reign.getStartDate());
          row.put("endDate", reign.getEndDate() != null ? reign.getEndDate() : "");
          rows.add(row);
        }
      }
    }
    return rows;
  }

  private List<Map<String, Object>> collectAlignments(
      Universe universe, List<WrestlerState> states) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (WrestlerState state : states) {
      wrestlerAlignmentRepository
          .findByWrestlerAndUniverseId(state.getWrestler(), universe.getId())
          .ifPresent(
              a -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("wrestler", state.getWrestler().getName());
                row.put("alignmentType", a.getAlignmentType().name());
                row.put("level", a.getLevel());
                rows.add(row);
              });
    }
    return rows;
  }

  private List<Map<String, Object>> collectRelationships(List<WrestlerState> states) {
    Set<Long> seenRelIds = new HashSet<>();
    List<Map<String, Object>> rows = new ArrayList<>();
    for (WrestlerState state : states) {
      for (var rel : wrestlerRelationshipRepository.findAllByWrestler(state.getWrestler())) {
        if (seenRelIds.add(rel.getId())) {
          Map<String, Object> row = new LinkedHashMap<>();
          row.put("wrestler1", rel.getWrestler1().getName());
          row.put("wrestler2", rel.getWrestler2().getName());
          row.put("type", rel.getType().name());
          row.put("level", rel.getLevel());
          row.put("isStoryline", rel.getIsStoryline());
          row.put("startedDate", rel.getStartedDate());
          rows.add(row);
        }
      }
    }
    return rows;
  }
}
