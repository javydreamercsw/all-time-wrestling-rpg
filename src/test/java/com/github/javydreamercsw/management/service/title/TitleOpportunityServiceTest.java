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
package com.github.javydreamercsw.management.service.title;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentParticipant;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityRepository;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityStatus;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.BriefcaseCashedInEvent;
import com.github.javydreamercsw.management.event.BriefcaseGrantedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService;
import com.github.javydreamercsw.management.service.segment.SegmentTeam;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

/** Unit tests for the briefcase service (ATW-8p72): grant idempotency and cash-in validation. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TitleOpportunityServiceTest {

  private static final LocalDate GAME_DATE = LocalDate.of(2026, 10, 8);

  @Mock private TitleOpportunityRepository opportunityRepository;
  @Mock private TournamentRepository tournamentRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private TitleRepository titleRepository;
  @Mock private TitleReignRepository titleReignRepository;
  @Mock private ShowRepository showRepository;
  @Mock private SegmentRepository segmentRepository;
  @Mock private SegmentTypeRepository segmentTypeRepository;
  @Mock private SegmentRuleRepository segmentRuleRepository;
  @Mock private NPCSegmentResolutionService segmentResolutionService;
  @Mock private GameSettingService gameSettingService;
  @Mock private ApplicationEventPublisher eventPublisher;

  private TitleOpportunityService service;
  private Tournament tournament;
  private Wrestler winner;
  private Title title;
  private Universe universe;

  private final Clock fixedClock =
      Clock.fixed(
          GAME_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

  @BeforeEach
  void setUp() {
    service =
        new TitleOpportunityService(
            opportunityRepository,
            tournamentRepository,
            wrestlerRepository,
            titleRepository,
            titleReignRepository,
            showRepository,
            segmentTypeRepository,
            segmentRepository,
            segmentRuleRepository,
            segmentResolutionService,
            gameSettingService,
            fixedClock,
            eventPublisher);

    winner = new Wrestler();
    winner.setId(8L);
    winner.setName("Mukundi Shumba");
    winner.setActive(true);
    winner.setGender(Gender.MALE);

    universe = new Universe();
    universe.setId(1L);

    tournament = new Tournament();
    tournament.setId(1L);
    tournament.setName("Time Vault");
    tournament.setUniverse(universe);
    tournament.setEndDate(GAME_DATE);

    title = new Title();
    title.setId(2L);
    title.setName("ATW World");
    title.setIsActive(true);
    title.setUniverse(universe);

    lenient().when(gameSettingService.getCurrentGameDate()).thenReturn(GAME_DATE);
    lenient().when(gameSettingService.getBriefcaseExpiryDays()).thenReturn(365);
    lenient()
        .when(opportunityRepository.save(any(TitleOpportunity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
  }

  // ── Grant ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Grant creates a HELD briefcase with expiry earnedAt + 365 days")
  void grantCreatesHeldOpportunity() {
    when(opportunityRepository.existsByEarnedFromTournamentId(1L)).thenReturn(false);
    when(opportunityRepository.findFirstByWrestlerIdAndStatus(8L, TitleOpportunityStatus.HELD))
        .thenReturn(Optional.empty());

    Optional<TitleOpportunity> result = service.grantFromTournament(tournament, winner);

    assertTrue(result.isPresent());
    assertEquals(TitleOpportunityStatus.HELD, result.get().getStatus());
    assertEquals("Time Vault briefcase", result.get().getName());
    assertEquals(GAME_DATE, result.get().getEarnedAt());
    assertEquals(GAME_DATE.plusDays(365), result.get().getExpiryDate());
    assertEquals(tournament, result.get().getEarnedFromTournament());
    verify(eventPublisher).publishEvent(any(BriefcaseGrantedEvent.class));
  }

  @Test
  @DisplayName("Grant is idempotent: a second grant for the same tournament is skipped")
  void grantIsIdempotent() {
    when(opportunityRepository.existsByEarnedFromTournamentId(1L)).thenReturn(true);

    Optional<TitleOpportunity> result = service.grantFromTournament(tournament, winner);

    assertTrue(result.isEmpty());
    verify(opportunityRepository, never()).save(any(TitleOpportunity.class));
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Grant is skipped when the winner already holds a briefcase")
  void grantSkippedWhenHolderAlreadyHoldsOne() {
    when(opportunityRepository.existsByEarnedFromTournamentId(1L)).thenReturn(false);
    when(opportunityRepository.findFirstByWrestlerIdAndStatus(8L, TitleOpportunityStatus.HELD))
        .thenReturn(Optional.of(new TitleOpportunity()));

    Optional<TitleOpportunity> result = service.grantFromTournament(tournament, winner);

    assertTrue(result.isEmpty());
    verify(opportunityRepository, never()).save(any(TitleOpportunity.class));
  }

  @Test
  @DisplayName("Grant copies the tournament's gender division onto the briefcase")
  void grantCopiesTournamentGender() {
    tournament.setGender(Gender.MALE);
    when(opportunityRepository.existsByEarnedFromTournamentId(1L)).thenReturn(false);
    when(opportunityRepository.findFirstByWrestlerIdAndStatus(8L, TitleOpportunityStatus.HELD))
        .thenReturn(Optional.empty());

    Optional<TitleOpportunity> result = service.grantFromTournament(tournament, winner);

    assertTrue(result.isPresent());
    assertEquals(Gender.MALE, result.get().getGender());
  }

  // ── Cash in ──────────────────────────────────────────────────────────────

  private TitleOpportunity heldOpportunity() {
    TitleOpportunity opportunity = new TitleOpportunity();
    opportunity.setId(10L);
    opportunity.setName("Time Vault briefcase");
    opportunity.setStatus(TitleOpportunityStatus.HELD);
    opportunity.setWrestler(winner);
    opportunity.setUniverse(universe);
    opportunity.setEarnedAt(GAME_DATE.minusDays(30));
    opportunity.setExpiryDate(GAME_DATE.plusDays(335));
    return opportunity;
  }

  @Test
  @DisplayName("Cash-in books a title segment against the reigning champion and spends the case")
  void cashInBooksTitleSegment() {
    TitleOpportunity opportunity = heldOpportunity();
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(opportunity));
    when(titleRepository.findById(2L)).thenReturn(Optional.of(title));
    when(titleReignRepository.findByTitleIdAndEndDateIsNull(2L))
        .thenReturn(List.of(reignWith(winner.getName()))); // champion exists
    when(showRepository.findById(5L)).thenReturn(Optional.of(new Show()));

    SegmentType oneOnOne = new SegmentType();
    when(segmentTypeRepository.findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode()))
        .thenReturn(Optional.of(oneOnOne));
    Segment booked = new Segment();
    when(segmentResolutionService.resolveTeamSegment(
            any(SegmentTeam.class),
            any(SegmentTeam.class),
            any(SegmentType.class),
            any(Show.class),
            anyString()))
        .thenReturn(booked);

    Segment result = service.cashIn(10L, 2L, 5L);

    assertEquals(booked, result);
    assertTrue(booked.getIsTitleSegment());
    assertEquals(1, booked.getTitles().size());
    assertEquals(TitleOpportunityStatus.CASHED_IN, opportunity.getStatus());
    assertEquals(title, opportunity.getCashedAgainstTitle());
    assertEquals(booked, opportunity.getCashedAtSegment());
    assertNotNull(opportunity.getCashedAt());
    verify(eventPublisher).publishEvent(any(BriefcaseCashedInEvent.class));
  }

  @Test
  @DisplayName("Cash-in rejects a non-HELD briefcase")
  void cashInRejectsNonHeld() {
    TitleOpportunity opportunity = heldOpportunity();
    opportunity.setStatus(TitleOpportunityStatus.CASHED_IN);
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(opportunity));

    assertThrows(IllegalStateException.class, () -> service.cashIn(10L, 2L, 5L));
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Cash-in rejects an expired briefcase and flips it to EXPIRED")
  void cashInRejectsExpired() {
    TitleOpportunity opportunity = heldOpportunity();
    opportunity.setExpiryDate(GAME_DATE.minusDays(1));
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(opportunity));

    assertThrows(IllegalStateException.class, () -> service.cashIn(10L, 2L, 5L));
    assertEquals(TitleOpportunityStatus.EXPIRED, opportunity.getStatus());
    verify(opportunityRepository).save(opportunity);
  }

  @Test
  @DisplayName("Cash-in rejects an inactive holder")
  void cashInRejectsInactiveHolder() {
    TitleOpportunity opportunity = heldOpportunity();
    winner.setActive(false);
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(opportunity));

    assertThrows(IllegalStateException.class, () -> service.cashIn(10L, 2L, 5L));
  }

  @Test
  @DisplayName("Cash-in rejects a retired title")
  void cashInRejectsRetiredTitle() {
    TitleOpportunity opportunity = heldOpportunity();
    title.setIsActive(false);
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(opportunity));
    when(titleRepository.findById(2L)).thenReturn(Optional.of(title));

    assertThrows(IllegalStateException.class, () -> service.cashIn(10L, 2L, 5L));
  }

  @Test
  @DisplayName("Cash-in rejects a title from a different universe")
  void cashInRejectsUniverseMismatch() {
    TitleOpportunity opportunity = heldOpportunity();
    Universe otherUniverse = new Universe();
    otherUniverse.setId(2L);
    title.setUniverse(otherUniverse);
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(opportunity));
    when(titleRepository.findById(2L)).thenReturn(Optional.of(title));

    assertThrows(IllegalStateException.class, () -> service.cashIn(10L, 2L, 5L));
  }

  @Test
  @DisplayName("Cash-in rejects a gender-mismatched championship")
  void cashInRejectsGenderMismatch() {
    TitleOpportunity opportunity = heldOpportunity();
    title.setGender(Gender.FEMALE);
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(opportunity));
    when(titleRepository.findById(2L)).thenReturn(Optional.of(title));

    assertThrows(IllegalStateException.class, () -> service.cashIn(10L, 2L, 5L));
  }

  @Test
  @DisplayName("Cash-in rejects a vacant title")
  void cashInRejectsVacantTitle() {
    TitleOpportunity opportunity = heldOpportunity();
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(opportunity));
    when(titleRepository.findById(2L)).thenReturn(Optional.of(title));
    when(titleReignRepository.findByTitleIdAndEndDateIsNull(2L)).thenReturn(List.of());

    assertThrows(IllegalStateException.class, () -> service.cashIn(10L, 2L, 5L));
  }

  @Test
  @DisplayName("Cash-in rejects a division mismatch between gendered briefcase and title")
  void cashInRejectsDivisionMismatch() {
    // A men's briefcase cannot challenge a women's championship even though the holder
    // (a male wrestler) would pass the holder-gender check (ATW-hq8d).
    TitleOpportunity opportunity = heldOpportunity();
    opportunity.setGender(Gender.MALE);
    title.setGender(Gender.FEMALE);
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(opportunity));
    when(titleRepository.findById(2L)).thenReturn(Optional.of(title));

    assertThrows(IllegalStateException.class, () -> service.cashIn(10L, 2L, 5L));
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Cash-in allows a gendered briefcase against an ungendered title")
  void cashInAllowsGenderedBriefcaseOnUngenderedTitle() {
    TitleOpportunity opportunity = heldOpportunity();
    opportunity.setGender(Gender.MALE);
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(opportunity));
    when(titleRepository.findById(2L)).thenReturn(Optional.of(title));
    when(titleReignRepository.findByTitleIdAndEndDateIsNull(2L))
        .thenReturn(List.of(reignWith("Champ")));
    when(showRepository.findById(5L)).thenReturn(Optional.of(new Show()));
    when(segmentTypeRepository.findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode()))
        .thenReturn(Optional.of(new SegmentType()));
    when(segmentResolutionService.resolveTeamSegment(
            any(SegmentTeam.class),
            any(SegmentTeam.class),
            any(SegmentType.class),
            any(Show.class),
            anyString()))
        .thenReturn(new Segment());

    Segment result = service.cashIn(10L, 2L, 5L);

    assertNotNull(result);
    assertEquals(TitleOpportunityStatus.CASHED_IN, opportunity.getStatus());
  }

  // ── Post-match ambush (ATW-p8ij) ─────────────────────────────────────────

  private Segment championSegmentWithHealth(Integer finalHealth) {
    Segment championSegment = new Segment();
    championSegment.setId(20L);
    championSegment.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);
    SegmentParticipant participant = new SegmentParticipant();
    participant.setWrestler(winner);
    participant.setFinalHealth(finalHealth);
    championSegment.getParticipants().add(participant);
    return championSegment;
  }

  private void stubAmbushHappyPath(Segment championSegment, int startingHealth) {
    Show show = new Show();
    show.setId(5L);
    championSegment.setShow(show);
    when(showRepository.findById(5L)).thenReturn(Optional.of(show));
    when(segmentRepository.findById(20L)).thenReturn(Optional.of(championSegment));
    when(titleRepository.findById(2L)).thenReturn(Optional.of(title));
    // The reigning champion IS the wrestler whose wear we read (same id as the participant).
    when(titleReignRepository.findByTitleIdAndEndDateIsNull(2L))
        .thenReturn(List.of(reignWith(winner)));
    when(segmentTypeRepository.findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode()))
        .thenReturn(Optional.of(new SegmentType()));
    when(segmentResolutionService.resolveTeamSegment(
            any(SegmentTeam.class),
            any(SegmentTeam.class),
            any(SegmentType.class),
            any(Show.class),
            anyString()))
        .thenReturn(new Segment());
  }

  @Test
  @DisplayName("Ambush books with the champion's wear carried into the team penalty")
  void ambushAppliesWearPenalty() {
    TitleOpportunity opportunity = heldOpportunity();
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(opportunity));
    winner.setStartingHealth(20);
    Segment championSegment = championSegmentWithHealth(6); // 70% lost → 7 - 2 breather = 5
    stubAmbushHappyPath(championSegment, 20);

    Segment result = service.cashInAmbush(10L, 2L, 5L, 20L);

    assertNotNull(result);
    assertEquals(TitleOpportunityStatus.CASHED_IN, opportunity.getStatus());
    ArgumentCaptor<SegmentTeam> teams = ArgumentCaptor.forClass(SegmentTeam.class);
    verify(segmentResolutionService)
        .resolveTeamSegment(teams.capture(), any(SegmentTeam.class), any(), any(), anyString());
    assertEquals(5, teams.getValue().getExtraWearPenalty());
  }

  @Test
  @DisplayName("Ambush rejects a champion who has not wrestled on the show")
  void ambushRejectsUnwornChampion() {
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(heldOpportunity()));
    Segment championSegment = new Segment();
    championSegment.setId(20L);
    championSegment.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);
    Show show = new Show();
    show.setId(5L);
    championSegment.setShow(show);
    when(showRepository.findById(5L)).thenReturn(Optional.of(show));
    when(segmentRepository.findById(20L)).thenReturn(Optional.of(championSegment));
    when(titleRepository.findById(2L)).thenReturn(Optional.of(title));
    when(titleReignRepository.findByTitleIdAndEndDateIsNull(2L))
        .thenReturn(List.of(reignWith("Champ")));

    assertThrows(IllegalStateException.class, () -> service.cashInAmbush(10L, 2L, 5L, 20L));
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Ambush rejects a fresh champion (no exploitable wear)")
  void ambushRejectsFreshChampion() {
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(heldOpportunity()));
    winner.setStartingHealth(20);
    stubAmbushHappyPath(championSegmentWithHealth(20), 20); // no health lost

    assertThrows(IllegalStateException.class, () -> service.cashInAmbush(10L, 2L, 5L, 20L));
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Ambush rejects a promo (no wear to exploit)")
  void ambushRejectsPromo() {
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(heldOpportunity()));
    Show show = new Show();
    show.setId(5L);
    Segment promo = new Segment();
    promo.setId(20L);
    promo.setShow(show);
    SegmentType promoType = new SegmentType();
    promoType.setCode(WellKnownSegmentType.PROMO.getCode());
    promo.setSegmentType(promoType);
    promo.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);
    when(showRepository.findById(5L)).thenReturn(Optional.of(show));
    when(segmentRepository.findById(20L)).thenReturn(Optional.of(promo));

    assertThrows(IllegalStateException.class, () -> service.cashInAmbush(10L, 2L, 5L, 20L));
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Ambush rejects an unadjudicated champion match (no final wear)")
  void ambushRejectsUnadjudicatedChampionMatch() {
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(heldOpportunity()));
    Show show = new Show();
    show.setId(5L);
    Segment pending = championSegmentWithHealth(6);
    pending.setAdjudicationStatus(AdjudicationStatus.PENDING);
    pending.setShow(show);
    when(showRepository.findById(5L)).thenReturn(Optional.of(show));
    when(segmentRepository.findById(20L)).thenReturn(Optional.of(pending));

    assertThrows(IllegalStateException.class, () -> service.cashInAmbush(10L, 2L, 5L, 20L));
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Ambush rejects a champion segment from a different show")
  void ambushRejectsCrossShowTarget() {
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(heldOpportunity()));
    Show thisShow = new Show();
    thisShow.setId(5L);
    Show otherShow = new Show();
    otherShow.setId(9L);
    Segment elsewhere = championSegmentWithHealth(6);
    elsewhere.setShow(otherShow);
    when(showRepository.findById(5L)).thenReturn(Optional.of(thisShow));
    when(segmentRepository.findById(20L)).thenReturn(Optional.of(elsewhere));

    assertThrows(IllegalArgumentException.class, () -> service.cashInAmbush(10L, 2L, 5L, 20L));
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Ambush rejects a champion who has not wrestled on the show")
  void ambushRejectsChampionNotOnCard() {
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(heldOpportunity()));
    Show show = new Show();
    show.setId(5L);
    // The champion (winner) is the reigning champion but never appears in the segment's
    // participants or wrestler list — nothing to exploit.
    Segment elsewhere = new Segment();
    elsewhere.setId(20L);
    elsewhere.setShow(show);
    elsewhere.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);
    when(showRepository.findById(5L)).thenReturn(Optional.of(show));
    when(segmentRepository.findById(20L)).thenReturn(Optional.of(elsewhere));
    when(titleRepository.findById(2L)).thenReturn(Optional.of(title));
    when(titleReignRepository.findByTitleIdAndEndDateIsNull(2L))
        .thenReturn(List.of(reignWith(winner)));

    assertThrows(IllegalStateException.class, () -> service.cashInAmbush(10L, 2L, 5L, 20L));
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Ambush rejects a vacant title")
  void ambushRejectsVacantTitle() {
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(heldOpportunity()));
    stubAmbushHappyPath(championSegmentWithHealth(6), 20);
    when(titleReignRepository.findByTitleIdAndEndDateIsNull(2L)).thenReturn(List.of());

    assertThrows(IllegalStateException.class, () -> service.cashInAmbush(10L, 2L, 5L, 20L));
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Ambush rejects a missing champion segment")
  void ambushRejectsMissingSegment() {
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(heldOpportunity()));
    when(showRepository.findById(5L)).thenReturn(Optional.of(new Show()));
    when(segmentRepository.findById(20L)).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> service.cashInAmbush(10L, 2L, 5L, 20L));
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Ambush by a gendered briefcase against the other division's title rejects")
  void ambushRejectsDivisionMismatch() {
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(heldOpportunity()));
    heldOpportunity().setGender(Gender.FEMALE);
    title.setGender(Gender.MALE);
    stubAmbushHappyPath(championSegmentWithHealth(6), 20);

    assertThrows(IllegalStateException.class, () -> service.cashInAmbush(10L, 2L, 5L, 20L));
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Cash-in rejects an unknown briefcase id")
  void cashInRejectsUnknownBriefcase() {
    when(opportunityRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> service.cashIn(999L, 2L, 5L));
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Cash-in rejects when the One on One segment type is missing")
  void cashInRejectsMissingSegmentType() {
    when(opportunityRepository.findById(10L)).thenReturn(Optional.of(heldOpportunity()));
    when(titleRepository.findById(2L)).thenReturn(Optional.of(title));
    when(titleReignRepository.findByTitleIdAndEndDateIsNull(2L))
        .thenReturn(List.of(reignWith(winner.getName())));
    when(showRepository.findById(5L)).thenReturn(Optional.of(new Show()));
    when(segmentTypeRepository.findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode()))
        .thenReturn(Optional.empty());

    assertThrows(IllegalStateException.class, () -> service.cashIn(10L, 2L, 5L));
    verify(eventPublisher, never()).publishEvent(any());
  }

  // ── Narration context (ATW-brrz) ─────────────────────────────────────────

  @Test
  @DisplayName("heldBriefcaseContextOf describes the case with its expiry")
  void heldBriefcaseContext_describesCase() {
    TitleOpportunity held = heldOpportunity();
    held.setExpiryDate(LocalDate.of(2027, 10, 8));
    when(opportunityRepository.findFirstByWrestlerIdAndStatus(8L, TitleOpportunityStatus.HELD))
        .thenReturn(Optional.of(held));

    String context = service.heldBriefcaseContextOf(8L);

    assertNotNull(context);
    assertTrue(context.contains("Time Vault briefcase"));
    assertTrue(context.contains("cashable against any reigning champion"));
    assertTrue(context.contains("until 2027-10-08"));
  }

  @Test
  @DisplayName("heldBriefcaseContextOf returns null without a held case")
  void heldBriefcaseContext_none_returnsNull() {
    when(opportunityRepository.findFirstByWrestlerIdAndStatus(8L, TitleOpportunityStatus.HELD))
        .thenReturn(Optional.empty());

    assertNull(service.heldBriefcaseContextOf(8L));
  }

  // ── Expiry sweep ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("expireOverdue flips held past expiry to EXPIRED")
  void expireOverdueFlipsStatus() {
    TitleOpportunity overdue = heldOpportunity();
    when(opportunityRepository.findByStatusAndExpiryDateBefore(
            TitleOpportunityStatus.HELD, GAME_DATE))
        .thenReturn(List.of(overdue));

    int count = service.expireOverdue();

    assertEquals(1, count);
    assertEquals(TitleOpportunityStatus.EXPIRED, overdue.getStatus());
  }

  private TitleReign reignWith(String championName) {
    Wrestler champion = new Wrestler();
    champion.setId(99L);
    champion.setName(championName);
    TitleReign reign = new TitleReign();
    reign.getChampions().add(champion);
    return reign;
  }

  private TitleReign reignWith(Wrestler champion) {
    TitleReign reign = new TitleReign();
    reign.getChampions().add(champion);
    return reign;
  }

  // ── Admin CRUD (ATW-jpki) ────────────────────────────────────────────────

  private TitleOpportunity heldOpportunityForCrud() {
    TitleOpportunity held = new TitleOpportunity();
    held.setId(40L);
    held.setName("Manual briefcase");
    held.setStatus(TitleOpportunityStatus.HELD);
    held.setWrestler(winner);
    held.setUniverse(universe);
    held.setEarnedAt(GAME_DATE);
    return held;
  }

  @Test
  @DisplayName("create grants a manual HELD case with holder, universe and dates")
  void adminCreate_heldCase() {
    when(opportunityRepository.findFirstByWrestlerIdAndStatus(8L, TitleOpportunityStatus.HELD))
        .thenReturn(Optional.empty());

    TitleOpportunity created =
        service.adminCreate(
            "Golden case", winner, universe, Gender.MALE, GAME_DATE.minusDays(10), null, null);

    assertEquals(TitleOpportunityStatus.HELD, created.getStatus());
    assertEquals("Golden case", created.getName());
    assertEquals(winner, created.getWrestler());
    assertEquals(universe, created.getUniverse());
    assertEquals(GAME_DATE.minusDays(10), created.getEarnedAt());
    // Default expiry applies when none supplied.
    assertEquals(GAME_DATE.minusDays(10).plusDays(365), created.getExpiryDate());
  }

  @Test
  @DisplayName("create rejects a second HELD case for a wrestler who already holds one")
  void adminCreate_rejectsSecondHeldCase() {
    when(opportunityRepository.findFirstByWrestlerIdAndStatus(8L, TitleOpportunityStatus.HELD))
        .thenReturn(Optional.of(heldOpportunityForCrud()));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.adminCreate(
                "Golden case", winner, universe, Gender.MALE, GAME_DATE, null, null));
  }

  @Test
  @DisplayName("create rejects an inactive holder")
  void adminCreate_rejectsInactiveHolder() {
    winner.setActive(false);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.adminCreate(
                "Golden case", winner, universe, Gender.MALE, GAME_DATE, null, null));
  }

  @Test
  @DisplayName("create rejects a future earned-at date")
  void adminCreate_rejectsFutureEarnedAt() {
    // The anchor is the real clock, not the kayfabe game date: the Time Vault case in the
    // sandbox was earned 2026-10-08 while its universe's game date sat at 2026-07-07 — a
    // kayfabe-past, wall-clock-present date that must stay editable.
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.adminCreate(
                "Golden case",
                winner,
                universe,
                Gender.MALE,
                LocalDate.now().plusDays(1),
                null,
                null));
  }

  @Test
  @DisplayName("create honours an explicit expiry override")
  void adminCreate_expiryOverride() {
    LocalDate expiry = GAME_DATE.plusDays(30);
    when(opportunityRepository.findFirstByWrestlerIdAndStatus(8L, TitleOpportunityStatus.HELD))
        .thenReturn(Optional.empty());

    TitleOpportunity created =
        service.adminCreate("Golden case", winner, universe, null, GAME_DATE, expiry, null);

    assertEquals(expiry, created.getExpiryDate());
  }

  @Test
  @DisplayName("update edits name, expiry, image and earned-at of a HELD case")
  void adminUpdate_heldCase() {
    TitleOpportunity held = heldOpportunityForCrud();
    when(opportunityRepository.findByIdWithDetails(40L)).thenReturn(Optional.of(held));

    TitleOpportunity updated =
        service.adminUpdate(
            40L, "Renamed case", GAME_DATE.plusDays(90), "img://case.png", GAME_DATE.minusDays(5));

    assertEquals("Renamed case", updated.getName());
    assertEquals(GAME_DATE.plusDays(90), updated.getExpiryDate());
    assertEquals("img://case.png", updated.getImageUrl());
    assertEquals(GAME_DATE.minusDays(5), updated.getEarnedAt());
    verify(opportunityRepository).save(held);
  }

  @Test
  @DisplayName("update can reassign the holder of a HELD case")
  void adminUpdate_reassignsHolder() {
    TitleOpportunity held = heldOpportunityForCrud();
    when(opportunityRepository.findByIdWithDetails(40L)).thenReturn(Optional.of(held));
    Wrestler newHolder = new Wrestler();
    newHolder.setId(9L);
    newHolder.setName("New Holder");
    newHolder.setActive(true);
    when(opportunityRepository.findFirstByWrestlerIdAndStatus(9L, TitleOpportunityStatus.HELD))
        .thenReturn(Optional.empty());
    when(wrestlerRepository.findById(9L)).thenReturn(Optional.of(newHolder));

    TitleOpportunity updated = service.adminUpdateHolder(40L, 9L);

    assertEquals(newHolder, updated.getWrestler());
    verify(opportunityRepository).save(held);
  }

  @Test
  @DisplayName("holder reassignment keeps the one-HELD invariant and active-holder rule")
  void adminUpdateHolder_rejectsAlreadyHoldingAndInactive() {
    // The fixture's holder is wrestler 8; reassign to wrestler 12 to exercise the guards.
    TitleOpportunity held = heldOpportunityForCrud();
    when(opportunityRepository.findByIdWithDetails(40L)).thenReturn(Optional.of(held));
    when(opportunityRepository.findFirstByWrestlerIdAndStatus(12L, TitleOpportunityStatus.HELD))
        .thenReturn(Optional.of(new TitleOpportunity()));
    assertThrows(IllegalArgumentException.class, () -> service.adminUpdateHolder(40L, 12L));

    when(opportunityRepository.findFirstByWrestlerIdAndStatus(12L, TitleOpportunityStatus.HELD))
        .thenReturn(Optional.empty());
    Wrestler inactive = new Wrestler();
    inactive.setId(12L);
    inactive.setName("Inactive");
    inactive.setActive(false);
    when(wrestlerRepository.findById(12L)).thenReturn(Optional.of(inactive));
    assertThrows(IllegalArgumentException.class, () -> service.adminUpdateHolder(40L, 12L));
    verify(opportunityRepository, never()).save(any(TitleOpportunity.class));
  }

  @Test
  @DisplayName("update can change the division of a HELD case")
  void adminUpdate_changesDivision() {
    TitleOpportunity held = heldOpportunityForCrud();
    held.setGender(Gender.MALE);
    when(opportunityRepository.findByIdWithDetails(40L)).thenReturn(Optional.of(held));

    TitleOpportunity updated = service.adminUpdateDivision(40L, Gender.FEMALE);

    assertEquals(Gender.FEMALE, updated.getGender());
    verify(opportunityRepository).save(held);
  }

  @Test
  @DisplayName("update rejects CASHED_IN rows — history is immutable")
  void adminUpdate_rejectsCashedIn() {
    TitleOpportunity cashed = heldOpportunityForCrud();
    cashed.setStatus(TitleOpportunityStatus.CASHED_IN);
    when(opportunityRepository.findByIdWithDetails(40L)).thenReturn(Optional.of(cashed));

    assertThrows(
        IllegalArgumentException.class, () -> service.adminUpdate(40L, "x", null, null, null));
    verify(opportunityRepository, never()).save(any(TitleOpportunity.class));
  }

  @Test
  @DisplayName("update rejects EXPIRED rows too — only live cases are editable")
  void adminUpdate_rejectsExpired() {
    TitleOpportunity expired = heldOpportunityForCrud();
    expired.setStatus(TitleOpportunityStatus.EXPIRED);
    when(opportunityRepository.findByIdWithDetails(40L)).thenReturn(Optional.of(expired));

    assertThrows(
        IllegalArgumentException.class, () -> service.adminUpdate(40L, "x", null, null, null));
  }

  @Test
  @DisplayName("void flips a HELD case to VOIDED")
  void adminVoid_heldCase() {
    TitleOpportunity held = heldOpportunityForCrud();
    when(opportunityRepository.findByIdWithDetails(40L)).thenReturn(Optional.of(held));

    service.adminVoid(40L);

    assertEquals(TitleOpportunityStatus.VOIDED, held.getStatus());
    verify(opportunityRepository).save(held);
  }

  @Test
  @DisplayName("void rejects non-HELD rows")
  void adminVoid_rejectsNonHeld() {
    TitleOpportunity cashed = heldOpportunityForCrud();
    cashed.setStatus(TitleOpportunityStatus.CASHED_IN);
    when(opportunityRepository.findByIdWithDetails(40L)).thenReturn(Optional.of(cashed));

    assertThrows(IllegalArgumentException.class, () -> service.adminVoid(40L));
    verify(opportunityRepository, never()).save(any(TitleOpportunity.class));
  }
}
