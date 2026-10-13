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
package com.github.javydreamercsw.management.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for the ATW-8p72 one-time repair: convert the placeholder "Time Vault" title chain
 * into briefcase-deciding tournaments. The sandbox run (2026-10-08) shaped two of these: the
 * flush-before-reference-check ordering and the adjudication gate on the backfill.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TimeVaultBriefcaseMigrationTest {

  @Mock private TournamentRepository tournamentRepository;
  @Mock private TitleRepository titleRepository;
  @Mock private TitleOpportunityRepository titleOpportunityRepository;
  @Mock private SegmentRepository segmentRepository;
  @Mock private TitleOpportunityService titleOpportunityService;

  @InjectMocks private TimeVaultBriefcaseMigration migration;

  @Test
  @DisplayName("No Time Vault tournaments — nothing happens")
  void migrate_noVaults_nothingHappens() {
    when(tournamentRepository.findByNameStartingWithIgnoreCase("Time Vault")).thenReturn(List.of());

    migration.migrate();

    verify(tournamentRepository, never()).save(any());
    verify(titleRepository, never()).delete(any(Title.class));
    verify(titleOpportunityService, never()).grantFromTournament(any(), any());
  }

  @Test
  @DisplayName("Vaults get flagged briefcase-deciding; placeholder detached from segments")
  void migrate_flagsVaultsAndDetachesPlaceholderFromSegments() {
    // Production hazard: the pending final still carried the placeholder title — adjudicating it
    // as-is would have awarded a fake championship.
    Title placeholder = title(2L, "Time Vault");
    Tournament vault = vault(1L, "Time Vault", TournamentStatus.SCHEDULED);
    vault.setLinkedTitle(placeholder);
    Segment pendingFinal = new Segment();
    pendingFinal.setId(1230L);
    pendingFinal.getTitles().add(placeholder);
    when(tournamentRepository.findByNameStartingWithIgnoreCase("Time Vault"))
        .thenReturn(List.of(vault));
    when(segmentRepository.findByTitle(placeholder)).thenReturn(List.of(pendingFinal));
    when(titleRepository.findByName("Time Vault")).thenReturn(Optional.empty());

    migration.migrate();

    assertThat(vault.isBriefcaseDeciding()).isTrue();
    assertThat(vault.getLinkedTitle()).isNull();
    assertThat(pendingFinal.getTitles()).as("fake-award hazard defused").isEmpty();
    verify(tournamentRepository).save(vault);
    verify(titleOpportunityService, never()).grantFromTournament(any(), any());
  }

  @Test
  @DisplayName("Unused placeholder title is deleted when no references remain")
  void migrate_deletesUnusedPlaceholderTitle() {
    Tournament vault = vault(1L, "Time Vault", TournamentStatus.COMPLETE);
    vault.setBriefcaseDeciding(true); // already flagged, no detach needed; no entries → no backfill
    Title placeholder = title(2L, "Time Vault");
    placeholder.setIsActive(false);
    when(tournamentRepository.findByNameStartingWithIgnoreCase("Time Vault"))
        .thenReturn(List.of(vault));
    when(titleRepository.findByName("Time Vault")).thenReturn(Optional.of(placeholder));
    when(titleRepository.findById(2L)).thenReturn(Optional.of(placeholder));
    when(segmentRepository.findByTitle(placeholder)).thenReturn(List.of());

    migration.migrate();

    verify(titleRepository).delete(placeholder);
  }

  @Test
  @DisplayName("Placeholder with remaining references (still active) is kept, not deleted")
  void migrate_placeholderWithBlockers_isKeptInactive() {
    Tournament vault = vault(1L, "Time Vault", TournamentStatus.SCHEDULED);
    vault.setBriefcaseDeciding(true);
    Title placeholder = title(2L, "Time Vault");
    placeholder.setIsActive(true); // blocker: an active title must never be deleted
    when(tournamentRepository.findByNameStartingWithIgnoreCase("Time Vault"))
        .thenReturn(List.of(vault));
    when(titleRepository.findByName("Time Vault")).thenReturn(Optional.of(placeholder));
    when(titleRepository.findById(2L)).thenReturn(Optional.of(placeholder));
    when(segmentRepository.findByTitle(placeholder)).thenReturn(List.of());

    migration.migrate();

    verify(titleRepository, never()).delete(any(Title.class));
  }

  @Test
  @DisplayName("COMPLETE vault with an adjudicated final backfills the winner's briefcase")
  void migrate_backfillsWinner_whenFinalAdjudicated() {
    Wrestler winner = wrestler(10L, "Mukundi Shumba");
    Tournament vault = completeVaultWithWinner(winner);
    Segment adjudicatedFinal = new Segment();
    adjudicatedFinal.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);
    attachMatch(vault, adjudicatedFinal);
    when(tournamentRepository.findByNameStartingWithIgnoreCase("Time Vault"))
        .thenReturn(List.of(vault));
    when(titleRepository.findByName("Time Vault")).thenReturn(Optional.empty());
    when(titleOpportunityService.grantFromTournament(vault, winner))
        .thenReturn(Optional.of(new TitleOpportunity()));

    migration.migrate();

    verify(titleOpportunityService).grantFromTournament(vault, winner);
  }

  @Test
  @DisplayName("Booked-but-unadjudicated final defers the backfill (grants at adjudication)")
  void migrate_defersBackfill_whileFinalIsUnadjudicated() {
    // The provisional bracket winner must not mint a briefcase before adjudication confirms it.
    Wrestler winner = wrestler(10L, "Mukundi Shumba");
    Tournament vault = completeVaultWithWinner(winner);
    attachMatch(vault, new Segment()); // default adjudication status: PENDING
    when(tournamentRepository.findByNameStartingWithIgnoreCase("Time Vault"))
        .thenReturn(List.of(vault));
    when(titleRepository.findByName("Time Vault")).thenReturn(Optional.empty());

    migration.migrate();

    verify(titleOpportunityService, never()).grantFromTournament(any(), any());
  }

  @Test
  @DisplayName("Hand-recorded bracket (no segments ever booked) backfills immediately")
  void migrate_backfillsWinner_forHandRecordedBracketWithoutSegments() {
    Wrestler winner = wrestler(10L, "Mukundi Shumba");
    Tournament vault = completeVaultWithWinner(winner); // no rounds/matches attached
    when(tournamentRepository.findByNameStartingWithIgnoreCase("Time Vault"))
        .thenReturn(List.of(vault));
    when(titleRepository.findByName("Time Vault")).thenReturn(Optional.empty());
    when(titleOpportunityService.grantFromTournament(vault, winner))
        .thenReturn(Optional.of(new TitleOpportunity()));

    migration.migrate();

    verify(titleOpportunityService).grantFromTournament(vault, winner);
  }

  @Test
  @DisplayName("Non-COMPLETE editions (successor still scheduled) are never backfilled")
  void migrate_skipsBackfill_forIncompleteEditions() {
    Tournament scheduled = vault(3L, "Time Vault II", TournamentStatus.SCHEDULED);
    scheduled.setBriefcaseDeciding(true);
    when(tournamentRepository.findByNameStartingWithIgnoreCase("Time Vault"))
        .thenReturn(List.of(scheduled));
    when(titleRepository.findByName("Time Vault")).thenReturn(Optional.empty());

    migration.migrate();

    verify(titleOpportunityService, never()).grantFromTournament(any(), any());
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private Title title(Long id, String name) {
    Title title = new Title();
    title.setId(id);
    title.setName(name);
    return title;
  }

  private Tournament vault(Long id, String name, TournamentStatus status) {
    Tournament tournament = new Tournament();
    tournament.setId(id);
    tournament.setName(name);
    tournament.setStatus(status);
    return tournament;
  }

  private Wrestler wrestler(Long id, String name) {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(id);
    wrestler.setName(name);
    return wrestler;
  }

  private Tournament completeVaultWithWinner(Wrestler winner) {
    Tournament vault = vault(1L, "Time Vault", TournamentStatus.COMPLETE);
    vault.setBriefcaseDeciding(true);
    vault
        .getEntries()
        .add(
            TournamentEntry.builder()
                .wrestler(winner)
                .status(TournamentEntryStatus.WINNER)
                .build());
    return vault;
  }

  private void attachMatch(Tournament vault, Segment segment) {
    TournamentMatch match = new TournamentMatch();
    match.setSegment(segment);
    TournamentRound round = new TournamentRound();
    round.setTournament(vault);
    round.getMatches().add(match);
    match.setRound(round);
    vault.getRounds().add(round);
  }
}
