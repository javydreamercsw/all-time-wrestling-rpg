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
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.show.template.RecurrenceType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateSegmentAssignment;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.dto.ShowTemplateDTO;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(60)
public class ShowTemplateSync implements DataSyncContributor {

  private final ShowTemplateService showTemplateService;
  private final SegmentTypeService segmentTypeService;
  private final SegmentRuleService segmentRuleService;
  private final TournamentRepository tournamentRepository;
  private final ObjectMapper objectMapper;

  @Autowired
  public ShowTemplateSync(
      final ShowTemplateService showTemplateService,
      final SegmentTypeService segmentTypeService,
      final SegmentRuleService segmentRuleService,
      final TournamentRepository tournamentRepository,
      final ObjectMapper objectMapper) {
    this.showTemplateService = showTemplateService;
    this.segmentTypeService = segmentTypeService;
    this.segmentRuleService = segmentRuleService;
    this.tournamentRepository = tournamentRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    long existingCount = showTemplateService.count();
    if (existingCount > 0) {
      log.debug(
          "Show templates table already contains {} templates - skipping file import",
          existingCount);
      return;
    }

    try {
      List<ShowTemplateDTO> dtos = loadCatalog();
      for (ShowTemplateDTO dto : dtos) {
        ShowTemplate template =
            showTemplateService.createOrUpdateTemplate(
                dto.getName(),
                dto.getDescription(),
                dto.getShowTypeName(),
                null,
                dto.getCommentaryTeamName(),
                dto.getExpectedMatches(),
                dto.getExpectedPromos(),
                dto.getDurationDays(),
                dto.getRecurrenceType() != null
                    ? RecurrenceType.valueOf(dto.getRecurrenceType())
                    : null,
                dto.getDayOfWeek() != null ? DayOfWeek.valueOf(dto.getDayOfWeek()) : null,
                dto.getDayOfMonth(),
                dto.getWeekOfMonth(),
                dto.getMonth() != null ? Month.valueOf(dto.getMonth()) : null,
                dto.getGenderConstraint() != null
                    ? Gender.valueOf(dto.getGenderConstraint())
                    : null,
                dto.getRequiredExpansions());
        if (template != null) {
          seedAssignments(template, dto);
          log.debug(
              "Loaded show template: {} (Type: {})", template.getName(), dto.getShowTypeName());
        } else {
          log.warn(
              "Failed to load show template: {} - show type not found: {}",
              dto.getName(),
              dto.getShowTypeName());
        }
      }
    } catch (IOException e) {
      log.error("Error loading show templates from file", e);
    }
  }

  /** Parse the seed catalog. Protected so tests can substitute their own definitions. */
  protected List<ShowTemplateDTO> loadCatalog() throws IOException {
    ClassPathResource resource = new ClassPathResource("show_templates.json");
    if (resource.exists()) {
      log.debug("Loading show templates from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        return objectMapper.readValue(is, new TypeReference<>() {});
      }
    }
    log.warn("Show templates file not found: {}", resource.getPath());
    return List.of();
  }

  /**
   * Seed the template's assignment rows from the DTO (ATW-cpuu). Row targets resolve by NAME —
   * SegmentRuleSync (@Order 30) and SegmentTypeSync (@Order 40) ran earlier, so names resolve;
   * unknown names are skipped with a warning rather than failing the whole template. Deliberately
   * only touches templates with no rows yet: on re-seed the booker's own edits win.
   */
  private void seedAssignments(ShowTemplate template, ShowTemplateDTO dto) {
    if (dto.getAssignments() == null || dto.getAssignments().isEmpty()) {
      return;
    }
    List<ShowTemplateSegmentAssignment> rows = new ArrayList<>();
    for (ShowTemplateDTO.AssignmentDTO assignmentDto : dto.getAssignments()) {
      ShowTemplateSegmentAssignment row = new ShowTemplateSegmentAssignment();
      if (assignmentDto.getSegmentTypeName() != null) {
        segmentTypeService
            .findByName(assignmentDto.getSegmentTypeName())
            .ifPresentOrElse(
                row::setSegmentType,
                () ->
                    log.warn(
                        "Segment type '{}' not found — skipping assignment row on template '{}'",
                        assignmentDto.getSegmentTypeName(),
                        dto.getName()));
      }
      if (assignmentDto.getSegmentRuleName() != null) {
        segmentRuleService
            .findByName(assignmentDto.getSegmentRuleName())
            .ifPresentOrElse(
                row::setSegmentRule,
                () ->
                    log.warn(
                        "Segment rule '{}' not found — skipping assignment row on template '{}'",
                        assignmentDto.getSegmentRuleName(),
                        dto.getName()));
      }
      if (assignmentDto.getTournamentCode() != null) {
        // TournamentSync (@Order 55) ran before this sync, so catalog codes resolve.
        tournamentRepository
            .findByCode(assignmentDto.getTournamentCode())
            .ifPresentOrElse(
                row::setTournament,
                () ->
                    log.warn(
                        "Tournament code '{}' not found — skipping assignment row on template"
                            + " '{}'",
                        assignmentDto.getTournamentCode(),
                        dto.getName()));
      }
      if (assignmentDto.getSpecName() != null) {
        // Full spec row (ATW-etws): the booking path creates the tournament on first use. The
        // final rule and allowed pool resolve by name like type/rule targets.
        if (assignmentDto.getTournamentCode() != null) {
          log.warn(
              "Assignment row on template '{}' sets both tournamentCode and a spec — keeping the"
                  + " tournament reference, ignoring the spec",
              dto.getName());
        } else {
          row.setSpecName(assignmentDto.getSpecName());
          row.setSpecFormatId(assignmentDto.getSpecFormatId());
          row.setSpecEntrantCount(assignmentDto.getSpecEntrantCount());
          if (assignmentDto.getSpecFinalRuleName() != null) {
            segmentRuleService
                .findByName(assignmentDto.getSpecFinalRuleName())
                .ifPresentOrElse(
                    row::setSpecFinalRule,
                    () ->
                        log.warn(
                            "Final rule '{}' not found — spec final rule skipped on template"
                                + " '{}'",
                            assignmentDto.getSpecFinalRuleName(),
                            dto.getName()));
          }
          for (String ruleName : assignmentDto.getAllowedRuleNames()) {
            segmentRuleService
                .findByName(ruleName)
                .ifPresentOrElse(
                    row.getSpecAllowedRules()::add,
                    () ->
                        log.warn(
                            "Allowed rule '{}' not found — skipped from spec pool on template"
                                + " '{}'",
                            ruleName,
                            dto.getName()));
          }
        }
      }
      row.setMode(
          assignmentDto.getMode() != null
              ? ShowTemplateSegmentAssignment.AssignmentMode.valueOf(assignmentDto.getMode())
              : ShowTemplateSegmentAssignment.AssignmentMode.ENCOURAGED);
      if (!row.isValid()) {
        log.warn(
            "Assignment row on template '{}' targets nothing recognizable (type: '{}', rule:"
                + " '{}') — skipping",
            dto.getName(),
            assignmentDto.getSegmentTypeName(),
            assignmentDto.getSegmentRuleName());
        continue;
      }
      rows.add(row);
    }
    if (!rows.isEmpty()) {
      showTemplateService.syncSegmentAssignments(template.getId(), rows);
      log.debug("Seeded {} assignment row(s) on template '{}'", rows.size(), template.getName());
    }
  }
}
