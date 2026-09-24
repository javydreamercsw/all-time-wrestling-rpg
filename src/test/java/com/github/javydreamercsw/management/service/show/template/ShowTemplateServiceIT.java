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
package com.github.javydreamercsw.management.service.show.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeam;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateSegmentAssignment;
import com.github.javydreamercsw.management.domain.show.type.ShowCategory;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.service.show.ShowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for the template segment-assignment persistence (ATW-0331). These exercise the
 * real fetch-join + transactional sync against a live database — the failure mode they guard
 * (LazyInitializationException on the lazy collection from detached UI rows) cannot happen with
 * mocked repositories, which have no sessions to violate.
 */
class ShowTemplateServiceIT extends ManagementIntegrationTest {

  @Autowired private ShowTemplateService showTemplateService;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private SegmentRuleRepository segmentRuleRepository;
  @Autowired private TournamentRepository tournamentRepository;
  @Autowired private CommentaryTeamRepository commentaryTeamRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowService showService;
  @Autowired private Clock clock;

  private CommentaryTeam newTeam(String name) {
    CommentaryTeam team = new CommentaryTeam();
    team.setName(name);
    return commentaryTeamRepository.saveAndFlush(team);
  }

  private ShowType createPleType() {
    ShowType type =
        showTypeRepository.findAll().stream()
            .filter(t -> t.getCategory() == ShowCategory.PLE)
            .findFirst()
            .orElseGet(
                () -> {
                  ShowType fresh = new ShowType();
                  fresh.setName("PLE IT " + System.nanoTime());
                  fresh.setCategory(ShowCategory.PLE);
                  return showTypeRepository.save(fresh);
                });
    return type;
  }

  private ShowTemplate createTemplate(String name) {
    ShowTemplate template = new ShowTemplate();
    template.setName(name);
    template.setShowType(createPleType());
    return showTemplateService.save(template);
  }

  @Test
  @DisplayName("Template commentary-team change propagates to future empty shows (ATW-ekev)")
  void templateCommentaryChange_propagatesToFutureEmptyShells() {
    // Policy under test (ATW-ekev): snapshot fields re-sync to future non-adjudicated instances
    // at template save; shows with segments and past shows are never touched.
    CommentaryTeam newTeam =
        commentaryTeamRepository.save(newTeam("New Team " + System.nanoTime()));
    ShowType type = showTypeRepository.findAll().stream().findFirst().orElseThrow();

    ShowTemplate template = createTemplate("Prop Template " + System.nanoTime());
    Show futureEmpty =
        showService.createShow(
            "Future shell " + System.nanoTime(),
            null,
            type.getId(),
            LocalDate.now(clock).plusDays(10),
            null,
            template.getId(),
            null,
            null,
            null,
            null);
    Show pastShell =
        showService.createShow(
            "Past shell " + System.nanoTime(),
            null,
            type.getId(),
            LocalDate.now(clock).minusDays(5),
            null,
            template.getId(),
            null,
            null,
            null,
            null);

    // Change the template's commentary team and save — propagation fires on save.
    template.setCommentaryTeam(newTeam);
    showTemplateService.save(template);

    Show reloadedFuture = showService.getShowById(futureEmpty.getId()).orElseThrow();
    Show reloadedPast = showService.getShowById(pastShell.getId()).orElseThrow();
    assertThat(reloadedFuture.getCommentaryTeam())
        .as("Future empty shell picks up the new commentary team")
        .isNotNull()
        .extracting(CommentaryTeam::getId)
        .isEqualTo(newTeam.getId());
    assertThat(reloadedPast.getCommentaryTeam()).as("Past shows are never touched").isNull();
  }

  @Test
  @DisplayName("Shows with segments are exempt from snapshot propagation (ATW-ekev)")
  void propagation_skipsShowsWithSegments() {
    CommentaryTeam newTeam =
        commentaryTeamRepository.save(newTeam("Seg Team " + System.nanoTime()));
    ShowType type = showTypeRepository.findAll().stream().findFirst().orElseThrow();

    ShowTemplate template = createTemplate("Seg Template " + System.nanoTime());
    Show worked =
        showService.createShow(
            "Worked shell " + System.nanoTime(),
            null,
            type.getId(),
            LocalDate.now(clock).plusDays(10),
            null,
            template.getId(),
            null,
            null,
            null,
            null);
    Segment existing = new Segment();
    existing.setShow(worked);
    existing.setSegmentOrder(1);
    SegmentType anyType =
        segmentTypeRepository.findAll().stream()
            .findFirst()
            .orElseGet(
                () -> {
                  SegmentType fresh = new SegmentType();
                  fresh.setName("Prop IT Type " + System.nanoTime());
                  return segmentTypeRepository.saveAndFlush(fresh);
                });
    existing.setSegmentType(anyType);
    segmentRepository.save(existing);

    template.setCommentaryTeam(newTeam);
    showTemplateService.save(template);

    Show reloaded = showService.getShowById(worked.getId()).orElseThrow();
    assertThat(reloaded.getCommentaryTeam())
        .as("A shell with booker card work is left alone")
        .isNull();
  }

  @Test
  @DisplayName("getTemplateWithAssignments returns an initialized collection outside a session")
  void getTemplateWithAssignments_initializesLazyCollection() {
    ShowTemplate template = createTemplate("IT Template Read " + System.nanoTime());

    ShowTemplate fetched =
        showTemplateService.getTemplateWithAssignments(template.getId()).orElseThrow();

    // If the fetch-join did not initialize the (lazy) bag, reading it here — after the
    // transactional read method returned — would throw LazyInitializationException.
    assertThat(fetched.getSegmentAssignments()).isNotNull().isEmpty();
  }

  @Test
  @DisplayName("syncSegmentAssignments replaces rows transactionally; read-back is session-safe")
  void syncSegmentAssignments_roundTrip() {
    SegmentType eventOnlyType = new SegmentType();
    eventOnlyType.setName("IT Event Type " + System.nanoTime());
    eventOnlyType.setDescription("IT fixture");
    eventOnlyType.setEventOnly(true);
    eventOnlyType = segmentTypeRepository.save(eventOnlyType);

    ShowTemplate template = createTemplate("IT Template Sync " + System.nanoTime());

    ShowTemplateSegmentAssignment row = new ShowTemplateSegmentAssignment();
    row.setSegmentType(eventOnlyType);
    row.setMode(ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH);

    showTemplateService.syncSegmentAssignments(template.getId(), List.of(row));

    ShowTemplate fetched =
        showTemplateService.getTemplateWithAssignments(template.getId()).orElseThrow();
    assertThat(fetched.getSegmentAssignments()).hasSize(1);
    ShowTemplateSegmentAssignment saved = fetched.getSegmentAssignments().get(0);
    assertThat(saved.getSegmentType().getName()).isEqualTo(eventOnlyType.getName());
    assertThat(saved.getMode()).isEqualTo(ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH);

    // Second sync replaces (orphanRemoval) instead of appending.
    showTemplateService.syncSegmentAssignments(template.getId(), List.of());
    ShowTemplate reFetched =
        showTemplateService.getTemplateWithAssignments(template.getId()).orElseThrow();
    assertThat(reFetched.getSegmentAssignments()).isEmpty();
  }

  @Test
  @DisplayName("syncSegmentAssignments accepts a type+rule pair row (AUTO_ATTACH pairing)")
  void syncSegmentAssignments_typePairedRow() {
    SegmentType eventOnlyType = new SegmentType();
    eventOnlyType.setName("IT Pair Type " + System.nanoTime());
    eventOnlyType.setEventOnly(true);
    eventOnlyType = segmentTypeRepository.save(eventOnlyType);

    SegmentRule rule = new SegmentRule();
    rule.setName("IT Pair Rule " + System.nanoTime());
    rule.setDescription("IT fixture");
    rule = segmentRuleRepository.save(rule);

    ShowTemplate template = createTemplate("IT Template Pair " + System.nanoTime());

    ShowTemplateSegmentAssignment pair = new ShowTemplateSegmentAssignment();
    pair.setSegmentType(eventOnlyType);
    pair.setSegmentRule(rule);
    pair.setMode(ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH);

    showTemplateService.syncSegmentAssignments(template.getId(), List.of(pair));

    ShowTemplate fetched =
        showTemplateService.getTemplateWithAssignments(template.getId()).orElseThrow();
    assertThat(fetched.getSegmentAssignments()).hasSize(1);
    ShowTemplateSegmentAssignment saved = fetched.getSegmentAssignments().get(0);
    assertThat(saved.getSegmentType().getName()).isEqualTo(eventOnlyType.getName());
    assertThat(saved.getSegmentRule().getName()).isEqualTo(rule.getName());
    assertThat(fetched.getTypePairedAutoAttachAssignments()).hasSize(1);
  }

  @Test
  @DisplayName("syncSegmentAssignments rejects a row targeting neither type nor rule")
  void syncSegmentAssignments_invalidRow_throws() {
    ShowTemplate template = createTemplate("IT Template Invalid " + System.nanoTime());

    assertThatThrownBy(
            () ->
                showTemplateService.syncSegmentAssignments(
                    template.getId(), List.of(new ShowTemplateSegmentAssignment())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("segment type, a segment rule, or a tournament");
  }

  @Test
  @DisplayName(
      "a type+tournament row persists with tournament_id and is fetched eagerly by the"
          + " fetch-join (ATW-oahn)")
  void syncSegmentAssignments_tournamentRow_roundTrip() {
    // Tournament fixture: the template pairing references it via tournament_id (ATW-oahn).
    Tournament tournament = new Tournament();
    tournament.setName("IT Crown Cup " + System.nanoTime());
    tournament.setFormatId("SINGLE_ELIMINATION");
    tournament.setStatus(TournamentStatus.SCHEDULED);
    tournament = tournamentRepository.save(tournament);

    SegmentType eventOnlyType = new SegmentType();
    eventOnlyType.setName("IT Tournament Type " + System.nanoTime());
    eventOnlyType.setEventOnly(true);
    eventOnlyType = segmentTypeRepository.save(eventOnlyType);

    ShowTemplate template = createTemplate("IT Template Tournament " + System.nanoTime());

    ShowTemplateSegmentAssignment row = new ShowTemplateSegmentAssignment();
    row.setSegmentType(eventOnlyType);
    row.setTournament(tournament);
    row.setMode(ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH);

    showTemplateService.syncSegmentAssignments(template.getId(), List.of(row));

    ShowTemplate fetched =
        showTemplateService.getTemplateWithAssignments(template.getId()).orElseThrow();
    assertThat(fetched.getSegmentAssignments()).hasSize(1);
    ShowTemplateSegmentAssignment saved = fetched.getSegmentAssignments().get(0);
    // Reading the tournament reference after the transactional read returned is exactly the
    // detached-access path that motivates the LEFT JOIN FETCH a.tournament — a lazy proxy
    // without the fetch-join would throw LazyInitializationException here.
    assertThat(saved.getTournament().getName()).isEqualTo(tournament.getName());
    assertThat(fetched.findTournamentForSegmentType(saved.getSegmentType())).isPresent();
    assertThat(fetched.getTournamentAssignments()).hasSize(1);
  }
}
