/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowCategory;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.title.BriefcaseCashInMode;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityRepository;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityStatus;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.show.ShowService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end approval IT for NPC briefcase cash-in proposals (ATW-2b9u) on the real service graph:
 * a kept card row books the cash-in (PLANNED appended / AMBUSH inserted right after the champion's
 * match), a deleted row records the decline cooldown, and the show adjudicates end-to-end with the
 * ambush resolving after the champion's match.
 */
class BriefcaseNpcCashInApprovalIT extends ManagementIntegrationTest {

  @Autowired private ShowService showService;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private TitleRepository titleRepository;
  @Autowired private TitleOpportunityRepository opportunityRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.rivalry.RivalryRepository rivalryRepository;

  @Autowired private ShowPlanningService showPlanningService;

  private Universe universe;
  private Wrestler champion;
  private Wrestler holder;
  private Wrestler challenger;
  private Title worldTitle;
  private TitleOpportunity opportunity;
  private Show show;
  private SegmentType singlesType;

  @BeforeEach
  void setUpFixture() {
    universe =
        universeRepository
            .findByName("Default Universe")
            .orElseThrow(() -> new IllegalStateException("Default Universe missing from test DB"));

    champion = seedNpc("Briefcase IT Champ");
    holder = seedNpc("Briefcase IT Holder");
    challenger = seedNpc("Briefcase IT Challenger");

    // A hot active rivalry between the holder and the champion pushes the advisor's score over
    // the proposal threshold (heat is 25 of the 100-point scale).
    com.github.javydreamercsw.management.domain.rivalry.Rivalry rivalry =
        new com.github.javydreamercsw.management.domain.rivalry.Rivalry();
    rivalry.setWrestler1(holder);
    rivalry.setWrestler2(champion);
    rivalry.setHeat(90);
    rivalry.setIsActive(true);
    rivalry.setUniverse(universe);
    rivalryRepository.saveAndFlush(rivalry);

    worldTitle = new Title();
    worldTitle.setName("Briefcase IT World " + System.nanoTime());
    worldTitle.setIsActive(true);
    worldTitle.setUniverse(universe);
    worldTitle.setChampionshipType(
        com.github.javydreamercsw.management.domain.title.ChampionshipType.SINGLE);
    worldTitle.setTier(com.github.javydreamercsw.base.domain.wrestler.WrestlerTier.ICON);
    worldTitle.awardTitleTo(List.of(champion), Instant.now());
    worldTitle = titleRepository.saveAndFlush(worldTitle);

    opportunity = new TitleOpportunity();
    opportunity.setName("Briefcase IT case " + System.nanoTime());
    opportunity.setStatus(TitleOpportunityStatus.HELD);
    opportunity.setWrestler(holder);
    opportunity.setUniverse(universe);
    opportunity.setEarnedAt(LocalDate.now().minusDays(60));
    // Expiry inside ~4 game-months: urgency (35·u²) adds ~9, pushing ICON(20) + heat 90 (~23)
    // over the proposal threshold of 50 — the advisor must see this case as cash-in-worthy.
    opportunity.setExpiryDate(LocalDate.now().plusDays(60));
    opportunity = opportunityRepository.saveAndFlush(opportunity);

    ShowType weeklyType =
        showTypeRepository.findAll().stream()
            .filter(t -> t.getCategory() == ShowCategory.WEEKLY)
            .findFirst()
            .orElseThrow();
    show =
        showService.createShow(
            "Briefcase IT Show " + System.nanoTime(),
            "ATW-2b9u fixture",
            weeklyType.getId(),
            LocalDate.now().plusDays(7),
            null,
            null,
            universe.getId(),
            null,
            null,
            null);
    singlesType =
        segmentTypeRepository.findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode()).orElseThrow();
  }

  private Wrestler seedNpc(String name) {
    Wrestler w = new Wrestler();
    w.setName(name + " " + System.nanoTime());
    w.setActive(true);
    w.setIsPlayer(false);
    w.setGender(Gender.MALE);
    w = wrestlerRepository.saveAndFlush(w);
    WrestlerState state = new WrestlerState();
    state.setWrestler(w);
    state.setFans(1_000_000L);
    state.setUniverse(universe);
    w.getWrestlerStates().add(state);
    return wrestlerRepository.saveAndFlush(w);
  }

  private ProposedSegment singles(String w1, String w2) {
    ProposedSegment segment = new ProposedSegment();
    segment.setType(singlesType.getName());
    segment.setTeams(List.of(List.of(w1), List.of(w2)));
    return segment;
  }

  private ProposedSegment briefcaseRow(BriefcaseCashInMode mode) {
    ProposedSegment row = new ProposedSegment();
    row.setType(singlesType.getName());
    row.setSource(ProposedSegment.SOURCE_BRIEFCASE);
    row.setBriefcaseOpportunityId(opportunity.getId());
    row.setBriefcaseMode(mode);
    return row;
  }

  @Test
  @DisplayName("A kept PLANNED row books the cash-in segment and spends the case")
  void plannedCashIn_keptRow_booksTitleSegmentOnShow() {
    assertThat(segmentRepository.findByShowOrderBySegmentOrderAsc(show)).isEmpty();

    showPlanningService.approveSegments(
        show,
        List.of(
            singles(champion.getName(), challenger.getName()),
            briefcaseRow(BriefcaseCashInMode.PLANNED)));

    List<Segment> saved = segmentRepository.findByShowOrderBySegmentOrderAsc(show);
    // The AI-proposed slot + the cash-in segment appended after the card.
    assertThat(saved).hasSize(2);
    Segment cashIn = saved.get(1);
    assertThat(cashIn.getIsTitleSegment()).isTrue();
    assertThat(cashIn.getTitles()).extracting(Title::getId).containsExactly(worldTitle.getId());
    assertThat(cashIn.getWrestlers())
        .extracting(Wrestler::getId)
        .containsExactlyInAnyOrder(champion.getId(), holder.getId());
    TitleOpportunity after = opportunityRepository.findById(opportunity.getId()).orElseThrow();
    assertThat(after.getStatus()).isEqualTo(TitleOpportunityStatus.CASHED_IN);
    assertThat(after.getCashInMode()).isEqualTo(BriefcaseCashInMode.PLANNED);
  }

  @Test
  @DisplayName("A deleted row records the decline cooldown and the case stays HELD")
  void plannedCashIn_deletedRow_marksCooldown() {
    showPlanningService.approveSegments(
        show, List.of(singles(champion.getName(), challenger.getName())));

    TitleOpportunity after = opportunityRepository.findById(opportunity.getId()).orElseThrow();
    assertThat(after.getStatus()).isEqualTo(TitleOpportunityStatus.HELD);
    assertThat(after.getLastDeclinedAt()).isNotNull();
    // Only the AI-proposed slot booked — no cash-in segment.
    assertThat(segmentRepository.findByShowOrderBySegmentOrderAsc(show)).hasSize(1);
  }

  @Test
  @DisplayName("A kept AMBUSH row inserts the cash-in right after the champion's match")
  void ambush_keptRow_booksAfterChampionMatch() {
    showPlanningService.approveSegments(
        show,
        List.of(
            singles(champion.getName(), challenger.getName()),
            briefcaseRow(BriefcaseCashInMode.AMBUSH)));

    List<Segment> saved = segmentRepository.findByShowOrderBySegmentOrderAsc(show);
    // Card slot, then the ambush immediately after the champion's match.
    assertThat(saved).hasSize(2);
    Segment ambush = saved.get(1);
    assertThat(ambush.getIsTitleSegment()).isTrue();
    assertThat(ambush.getWrestlers())
        .extracting(Wrestler::getId)
        .containsExactlyInAnyOrder(champion.getId(), holder.getId());
    assertThat(ambush.getAdjudicationStatus()).isEqualTo(AdjudicationStatus.PENDING);
    TitleOpportunity after = opportunityRepository.findById(opportunity.getId()).orElseThrow();
    assertThat(after.getStatus()).isEqualTo(TitleOpportunityStatus.CASHED_IN);
    assertThat(after.getCashInMode()).isEqualTo(BriefcaseCashInMode.AMBUSH);
  }

  @Test
  @DisplayName("The show adjudicates end-to-end: champion's match, then the ambush")
  void ambush_endToEnd_adjudicates() {
    showPlanningService.approveSegments(
        show,
        List.of(
            singles(champion.getName(), challenger.getName()),
            briefcaseRow(BriefcaseCashInMode.AMBUSH)));

    showService.adjudicateShow(show.getId());

    List<Segment> saved = segmentRepository.findByShowOrderBySegmentOrderAsc(show);
    assertThat(saved).hasSize(2);
    assertThat(saved)
        .allSatisfy(
            s -> assertThat(s.getAdjudicationStatus()).isEqualTo(AdjudicationStatus.ADJUDICATED));
    TitleOpportunity after = opportunityRepository.findById(opportunity.getId()).orElseThrow();
    assertThat(after.getStatus()).isEqualTo(TitleOpportunityStatus.CASHED_IN);
    assertThat(after.getCashedAtSegment()).isNotNull();
  }
}
