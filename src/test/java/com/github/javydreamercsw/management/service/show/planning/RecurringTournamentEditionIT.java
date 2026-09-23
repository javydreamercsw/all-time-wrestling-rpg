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
package com.github.javydreamercsw.management.service.show.planning;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentRecurrence;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Cross-edition cycle IT for recurring tournaments (ATW-o4ad, ATW-o4ad.5): a full annual cycle on
 * the real service graph — the ANNUAL edition's payoff books at a PLE approval → the next edition
 * auto-creates in SCHEDULED → the template pairing re-points to it → a second PLE cycle feeds the
 * new edition's bracket. Also guards the legacy semantics: one-shot (recurrence NONE) tournaments
 * consume their pairing and never re-fire.
 */
class RecurringTournamentEditionIT extends AbstractTournamentFedPleIT {

  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowTemplateRepository showTemplateRepository;

  @BeforeEach
  void setUpFixture() {
    buildTournamentFedPleFixture();
    // Recurring edition: the fixture tournament is edition 1 of an ANNUAL chain.
    tournament.setRecurrence(TournamentRecurrence.ANNUAL);
    tournament.setEditionOrdinal(1);
    tournamentRepository.saveAndFlush(tournament);
  }

  @Test
  @DisplayName("Annual cycle: payoff books → successor auto-creates → pairing re-points")
  void annualCycle_payoffBooks_nextEditionRePlacesPairing() {
    // Approve a card containing the paired event-only type: the tournament auto-starts and its
    // round-1 match books. With a 2-entrant bracket this match IS the final (payoff).
    seedTournamentEntries(tournament, 2);
    ProposedSegment paired = new ProposedSegment();
    paired.setType(eventType.getName());

    approveCard(show, List.of(paired));

    // The edition that just played is COMPLETE and carries the chain link forward.
    Tournament firstEdition =
        tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    assertThat(firstEdition.getStatus()).isEqualTo(TournamentStatus.COMPLETE);

    // Successor: SCHEDULED, ordinal 2, named "II", chained to the completed edition.
    Optional<Tournament> successor = findSuccessor(firstEdition.getId());
    assertThat(successor).as("Next edition auto-creates at payoff booking").isPresent();
    Tournament second = successor.orElseThrow();
    assertThat(second.getStatus()).isEqualTo(TournamentStatus.SCHEDULED);
    assertThat(second.getEditionOrdinal()).isEqualTo(2);
    assertThat(second.getName()).endsWith(" II");
    assertThat(second.getRecurrence()).isEqualTo(TournamentRecurrence.ANNUAL);
    assertThat(second.getParent().getId()).isEqualTo(firstEdition.getId());

    // The pairing re-pointed: the template row now references the SECOND edition.
    Tournament pairingTarget = pairingTarget();
    assertThat(pairingTarget.getId())
        .as("Template pairing must re-point to the next edition")
        .isEqualTo(second.getId());
  }

  @Test
  @DisplayName("Re-approving the same PLE re-points to the existing successor — no duplicate")
  void annualCycle_reApproval_isIdempotent() {
    seedTournamentEntries(tournament, 2);
    ProposedSegment paired = new ProposedSegment();
    paired.setType(eventType.getName());
    approveCard(show, List.of(paired));

    Tournament firstEdition = tournamentService.findById(tournament.getId()).orElseThrow();
    Optional<Tournament> firstSuccessor = findSuccessor(firstEdition.getId());
    assertThat(firstSuccessor).isPresent();

    // A second approval of the same show (retry path) must not mint a third edition.
    Show sameShow = showService.getShowById(show.getId()).orElseThrow();
    ProposedSegment pairedAgain = new ProposedSegment();
    pairedAgain.setType(eventType.getName());
    approveCard(sameShow, List.of(pairedAgain));

    long editionCount =
        tournamentService.findAll().stream()
            .filter(t -> t.getEditionOrdinal() != null)
            .filter(t -> t.getName().startsWith(editionBaseName(tournament.getName())))
            .count();
    assertThat(editionCount)
        .as("Re-approval must not mint duplicate editions")
        .isEqualTo(2); // the completed edition + its single successor

    // And the pairing still points at the SAME successor.
    assertThat(pairingTarget().getId()).isEqualTo(firstSuccessor.orElseThrow().getId());
  }

  @Test
  @DisplayName("One-shot tournaments consume the pairing and never re-fire")
  void oneShot_pairingConsumed_neverRefires() {
    // Recurrence NONE (default) — the legacy ATW-z963 consumption semantics.
    tournament.setRecurrence(TournamentRecurrence.NONE);
    tournament.setEditionOrdinal(null);
    tournamentRepository.saveAndFlush(tournament);
    seedTournamentEntries(tournament, 2);

    ProposedSegment paired = new ProposedSegment();
    paired.setType(eventType.getName());
    approveCard(show, List.of(paired));

    assertThat(tournamentService.findById(tournament.getId()).orElseThrow().getStatus())
        .isEqualTo(TournamentStatus.COMPLETE);
    assertThat(pairingTarget())
        .as("One-shot payoff consumes the pairing — no tournament behind the row")
        .isNull();

    // A second approval falls back to AI participants; no new edition exists.
    Show secondShow = cloneShow();
    ProposedSegment pairedAgain = new ProposedSegment();
    pairedAgain.setType(eventType.getName());
    approveCard(secondShow, List.of(pairedAgain));

    assertThat(findSuccessor(tournament.getId())).isEmpty();
  }

  @Test
  @DisplayName("Second PLE cycle feeds the new edition's bracket")
  void annualCycle_secondPleFeedsNewEdition() {
    seedTournamentEntries(tournament, 2);
    ProposedSegment first = new ProposedSegment();
    first.setType(eventType.getName());
    approveCard(show, List.of(first));

    // The pairing now points at the successor — the next PLE runs on it.
    Show secondShow = cloneShow();
    ProposedSegment second = new ProposedSegment();
    second.setType(eventType.getName());
    approveCard(secondShow, List.of(second));

    // The new edition's bracket fed the second PLE — it is underway, not stranded.
    Tournament secondEdition = findSuccessor(tournament.getId()).orElseThrow();
    Tournament secondAfter =
        tournamentService.findByIdWithDetails(secondEdition.getId()).orElseThrow();
    assertThat(secondAfter.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);
    List<Segment> saved = segmentRepository.findByShowOrderBySegmentOrderAsc(secondShow);
    assertThat(saved)
        .as("The re-pointed pairing feeds the new edition on the next PLE")
        .isNotEmpty();
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private Optional<Tournament> findSuccessor(Long parentId) {
    return tournamentService.findAll().stream()
        .filter(t -> t.getParent() != null && parentId.equals(t.getParent().getId()))
        .findFirst();
  }

  private String editionBaseName(String name) {
    return name.replaceFirst(" (?:[IVXLCDM]+)$", "");
  }

  /** The tournament the template's pairing row currently references (re-read via the DB). */
  private Tournament pairingTarget() {
    return showTemplateRepository
        .findByIdWithAssignments(show.getTemplate().getId())
        .orElseThrow()
        .getTournamentAssignments()
        .stream()
        .map(a -> a.getTournament())
        .filter(java.util.Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private Show cloneShow() {
    return showService.createShow(
        "Recurring PLE II " + System.nanoTime(),
        "Second annual PLE fixture",
        show.getType().getId(),
        LocalDate.now().plusDays(14),
        null,
        show.getTemplate().getId(),
        universe.getId(),
        null,
        null,
        null);
  }
}
