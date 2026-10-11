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
package com.github.javydreamercsw.management.service.title;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.title.*;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.GameSettingService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for the deterministic NPC briefcase cash-in advisor (ATW-2b9u): scoring components,
 * hard gates, cooldown, and ordering.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BriefcaseCashInAdvisorTest {

  private static final LocalDate GAME_DATE = LocalDate.of(2026, 10, 8);
  private static final LocalDate EARNED_AT = LocalDate.of(2026, 6, 8); // 122 days ago

  @Mock private TitleOpportunityRepository opportunityRepository;
  @Mock private TitleReignRepository titleReignRepository;

  @Mock private SegmentRepository segmentRepository;

  @Mock private TitleRepository titleRepository;
  @Mock private RivalryRepository rivalryRepository;

  @Mock private WrestlerRepository wrestlerRepository;

  @Mock private GameSettingService gameSettingService;

  private BriefcaseCashInAdvisorService advisor;

  private Show show;
  private Wrestler holder;
  private Wrestler champion;
  private Title title;
  private TitleOpportunity opportunity;

  private final Clock fixedClock =
      Clock.fixed(
          GAME_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

  @BeforeEach
  void setUp() {
    advisor =
        new BriefcaseCashInAdvisorService(
            opportunityRepository,
            titleReignRepository,
            segmentRepository,
            titleRepository,
            rivalryRepository,
            wrestlerRepository,
            gameSettingService,
            fixedClock);

    show = new Show();
    show.setName("Weekly Show");

    holder = npcWrestler("Case Holder");
    champion = npcWrestler("Reigning Champ");
    title = new Title();
    title.setName("ATW World Championship");
    title.setTier(WrestlerTier.MAIN_EVENTER);
    title.setIsActive(true);

    opportunity = new TitleOpportunity();
    opportunity.setId(7L);
    opportunity.setName("Time Vault briefcase");
    opportunity.setStatus(TitleOpportunityStatus.HELD);
    opportunity.setWrestler(holder);
    opportunity.setEarnedAt(EARNED_AT);
    opportunity.setExpiryDate(GAME_DATE.plusDays(200)); // far from expiry

    TitleReign reign = new TitleReign();
    reign.setStartDate(GAME_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant());
    reign.setChampions(Set.of(champion));

    lenient().when(gameSettingService.isNpcBriefcaseCashInEnabled()).thenReturn(true);
    lenient().when(gameSettingService.getCurrentGameDate()).thenReturn(GAME_DATE);
    lenient()
        .when(opportunityRepository.findByStatus(TitleOpportunityStatus.HELD))
        .thenReturn(List.of(opportunity));
    lenient().when(titleRepository.findByIsActiveTrue()).thenReturn(List.of(title));
    lenient()
        .when(titleReignRepository.findByTitleIdAndEndDateIsNull(title.getId()))
        .thenReturn(List.of(reign));
    lenient()
        .when(rivalryRepository.findActiveRivalryBetween(holder, champion))
        .thenReturn(Optional.empty());
  }

  private long wrestlerIdSequence = 1L;

  private Wrestler npcWrestler(String name) {
    Wrestler w = new Wrestler();
    w.setId(wrestlerIdSequence++);
    w.setName(name);
    w.setIsPlayer(false);
    w.setActive(true);
    return w;
  }

  // ── Scoring components ─────────────────────────────────────────────────────

  @Test
  @DisplayName("scoring: expiry urgency raises the score")
  void evaluate_expiryUrgencyRaisesScore() {
    int base =
        advisor.scoreOf(
            new BriefcaseCashInAdvisorService.BriefcaseInputs(
                200, 365, 0, WrestlerTier.ROOKIE, 0, DefenseFrequencyType.WEEKLY, false));
    int urgent =
        advisor.scoreOf(
            new BriefcaseCashInAdvisorService.BriefcaseInputs(
                30, 365, 0, WrestlerTier.ROOKIE, 0, DefenseFrequencyType.WEEKLY, false));
    assertThat(urgent).isGreaterThan(base);
  }

  @Test
  @DisplayName("scoring: rivalry heat raises the score")
  void evaluate_heatRaisesScore() {
    int cold =
        advisor.scoreOf(
            new BriefcaseCashInAdvisorService.BriefcaseInputs(
                200, 365, 0, WrestlerTier.ROOKIE, 0, DefenseFrequencyType.WEEKLY, false));
    int hot =
        advisor.scoreOf(
            new BriefcaseCashInAdvisorService.BriefcaseInputs(
                200, 365, 80, WrestlerTier.ROOKIE, 0, DefenseFrequencyType.WEEKLY, false));
    assertThat(hot).isGreaterThan(cold);
  }

  @Test
  @DisplayName("scoring: title prestige raises the score")
  void evaluate_prestigeRaisesScore() {
    int rookie =
        advisor.scoreOf(
            new BriefcaseCashInAdvisorService.BriefcaseInputs(
                200, 365, 0, WrestlerTier.ROOKIE, 0, DefenseFrequencyType.WEEKLY, false));
    int icon =
        advisor.scoreOf(
            new BriefcaseCashInAdvisorService.BriefcaseInputs(
                200, 365, 0, WrestlerTier.ICON, 0, DefenseFrequencyType.WEEKLY, false));
    assertThat(icon).isGreaterThan(rookie);
  }

  @Test
  @DisplayName("scoring: champion defense staleness raises the score")
  void evaluate_stalenessRaisesScore() {
    int fresh =
        advisor.scoreOf(
            new BriefcaseCashInAdvisorService.BriefcaseInputs(
                200, 365, 0, WrestlerTier.ROOKIE, 1, DefenseFrequencyType.WEEKLY, false));
    int stale =
        advisor.scoreOf(
            new BriefcaseCashInAdvisorService.BriefcaseInputs(
                200, 365, 0, WrestlerTier.ROOKIE, 30, DefenseFrequencyType.WEEKLY, false));
    assertThat(stale).isGreaterThan(fresh);
  }

  @Test
  @DisplayName("scoring: a PLE show adds a bonus")
  void evaluate_pleAddsBonus() {
    int weekly =
        advisor.scoreOf(
            new BriefcaseCashInAdvisorService.BriefcaseInputs(
                200, 365, 0, WrestlerTier.ROOKIE, 0, DefenseFrequencyType.WEEKLY, false));
    int ple =
        advisor.scoreOf(
            new BriefcaseCashInAdvisorService.BriefcaseInputs(
                200, 365, 0, WrestlerTier.ROOKIE, 0, DefenseFrequencyType.WEEKLY, true));
    assertThat(ple).isEqualTo(weekly + 5);
  }

  // ── Proposals: gates ───────────────────────────────────────────────────────

  @Test
  @DisplayName("propose: above-threshold case proposes with PLANNED mode")
  void propose_aboveThreshold_proposesWithPlannedMode() {
    // Hot fixture: ICON title, warm (not run-in hot) rivalry, stale reign, near expiry → over
    // threshold with a PLANNED mode (heat 60 < RUN_IN_HEAT_THRESHOLD 70).
    title.setTier(WrestlerTier.ICON);
    opportunity.setExpiryDate(GAME_DATE.plusDays(40));
    Rivalry rivalry = new Rivalry();
    rivalry.setHeat(60);
    lenient()
        .when(rivalryRepository.findActiveRivalryBetween(holder, champion))
        .thenReturn(Optional.of(rivalry));
    lenient()
        .when(segmentRepository.findByTitle(title))
        .thenReturn(
            List.of(segmentDefendedOn(GAME_DATE.minusDays(21)))); // stale beyond the weekly cadence

    List<BriefcaseCashInAdvisorService.BriefcaseProposal> proposals = advisor.proposalsFor(show);

    assertThat(proposals).hasSize(1);
    assertThat(proposals.get(0).mode()).isEqualTo(BriefcaseCashInMode.PLANNED);
    assertThat(proposals.get(0).opportunity()).isSameAs(opportunity);
    assertThat(proposals.get(0).title()).isSameAs(title);
    assertThat(proposals.get(0).champions()).containsExactly(champion);
  }

  private Segment segmentDefendedOn(LocalDate date) {
    Segment segment = new Segment();
    segment.setSegmentDate(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    return segment;
  }

  @Test
  @DisplayName("propose: below-threshold case proposes nothing")
  void propose_belowThreshold_returnsEmpty() {
    // Rookie title, fresh reign, no rivalry, far from expiry → low score, no proposal.
    title.setTier(WrestlerTier.ROOKIE);
    TitleReign reign = new TitleReign();
    reign.setStartDate(GAME_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant());
    reign.setChampions(Set.of(champion));
    lenient()
        .when(titleReignRepository.findByTitleIdAndEndDateIsNull(title.getId()))
        .thenReturn(List.of(reign));

    assertThat(advisor.proposalsFor(show)).isEmpty();
  }

  @Test
  @DisplayName("propose: excludes player holders")
  void propose_excludesPlayerHolder() {
    holder.setIsPlayer(true);
    assertThat(advisor.proposalsFor(show)).isEmpty();
  }

  @Test
  @DisplayName("propose: excludes expired or non-HELD opportunities")
  void propose_excludesExpiredOrNonHeld() {
    opportunity.setExpiryDate(GAME_DATE.minusDays(1));
    assertThat(advisor.proposalsFor(show)).isEmpty();

    opportunity.setExpiryDate(GAME_DATE.plusDays(200));
    opportunity.setStatus(TitleOpportunityStatus.CASHED_IN);
    assertThat(advisor.proposalsFor(show)).isEmpty();
  }

  @Test
  @DisplayName("propose: excludes inactive holders")
  void propose_excludesInactiveHolder() {
    holder.setActive(false);
    assertThat(advisor.proposalsFor(show)).isEmpty();
  }

  @Test
  @DisplayName("propose: skips a title with no reigning champion (vacant)")
  void propose_skipsVacantTitle() {
    lenient()
        .when(titleReignRepository.findByTitleIdAndEndDateIsNull(title.getId()))
        .thenReturn(List.of());
    assertThat(advisor.proposalsFor(show)).isEmpty();
  }

  @Test
  @DisplayName("propose: respects gender division rules")
  void propose_respectsGenderDivisionRules() {
    holder.setGender(Gender.MALE);
    title.setGender(Gender.FEMALE);
    assertThat(advisor.proposalsFor(show)).isEmpty();

    // A gendered briefcase never challenges the other division's title.
    title.setGender(Gender.MALE);
    opportunity.setGender(Gender.FEMALE);
    assertThat(advisor.proposalsFor(show)).isEmpty();
  }

  @Test
  @DisplayName("propose: respects the cooldown window after a decline")
  void propose_respectsCooldownWindow() {
    opportunity.setLastDeclinedAt(GAME_DATE.minusDays(10));

    assertThat(advisor.proposalsFor(show)).isEmpty();
  }

  @Test
  @DisplayName("propose: cooldown is overridden in the top urgency band")
  void propose_cooldownOverriddenInTopUrgencyBand() {
    opportunity.setLastDeclinedAt(GAME_DATE.minusDays(10));
    opportunity.setExpiryDate(GAME_DATE.plusDays(20)); // inside the last quarter of the window
    Rivalry rivalry = new Rivalry();
    rivalry.setHeat(80);
    lenient()
        .when(rivalryRepository.findActiveRivalryBetween(holder, champion))
        .thenReturn(Optional.of(rivalry));

    List<BriefcaseCashInAdvisorService.BriefcaseProposal> proposals = advisor.proposalsFor(show);

    assertThat(proposals).hasSize(1);
  }

  @Test
  @DisplayName("propose: returns nothing when the feature setting is disabled")
  void propose_settingDisabled_returnsEmpty() {
    lenient().when(gameSettingService.isNpcBriefcaseCashInEnabled()).thenReturn(false);
    assertThat(advisor.proposalsFor(show)).isEmpty();
  }

  // ── Proposals: ordering ────────────────────────────────────────────────────

  @Test
  @DisplayName("propose: multiple candidates order by score descending")
  void propose_ordersByScoreDescending() {
    Title iconTitle = new Title();
    iconTitle.setName("ATW Icon Championship");
    iconTitle.setTier(WrestlerTier.ICON);
    iconTitle.setIsActive(true);

    Title rookieTitle = new Title();
    rookieTitle.setName("ATW Rookie Championship");
    rookieTitle.setTier(WrestlerTier.ROOKIE);
    rookieTitle.setIsActive(true);

    TitleReign iconReign = new TitleReign();
    iconReign.setStartDate(GAME_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant());
    iconReign.setChampions(Set.of(champion));
    TitleReign rookieReign = new TitleReign();
    rookieReign.setStartDate(GAME_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant());
    rookieReign.setChampions(Set.of(champion));

    // Hot shared inputs so both clear the threshold and only prestige separates them.
    Rivalry rivalry = new Rivalry();
    rivalry.setHeat(90);
    lenient()
        .when(rivalryRepository.findActiveRivalryBetween(holder, champion))
        .thenReturn(Optional.of(rivalry));
    opportunity.setExpiryDate(GAME_DATE.plusDays(40));
    lenient()
        .when(segmentRepository.findByTitle(iconTitle))
        .thenReturn(List.of(segmentDefendedOn(GAME_DATE.minusDays(21))));
    lenient()
        .when(segmentRepository.findByTitle(rookieTitle))
        .thenReturn(List.of(segmentDefendedOn(GAME_DATE.minusDays(21))));

    lenient()
        .when(titleRepository.findByIsActiveTrue())
        .thenReturn(List.of(rookieTitle, iconTitle));
    lenient()
        .when(titleReignRepository.findByTitleIdAndEndDateIsNull(iconTitle.getId()))
        .thenReturn(List.of(iconReign));
    lenient()
        .when(titleReignRepository.findByTitleIdAndEndDateIsNull(rookieTitle.getId()))
        .thenReturn(List.of(rookieReign));

    List<BriefcaseCashInAdvisorService.BriefcaseProposal> proposals = advisor.proposalsFor(show);

    assertThat(proposals).hasSize(2);
    assertThat(proposals.get(0).title()).isSameAs(iconTitle);
    assertThat(proposals.get(1).title()).isSameAs(rookieTitle);
    assertThat(proposals.get(0).score()).isGreaterThanOrEqualTo(proposals.get(1).score());
  }

  @Test
  @DisplayName("RUN_IN: a cold rivalry proposes PLANNED, never RUN_IN")
  void runIn_coldRivalry_neverSelected() {
    // Fixture rivalry: holder vs champion has no heat (Optional.empty stub in setUp).
    List<BriefcaseCashInAdvisorService.BriefcaseProposal> proposals = advisor.proposalsFor(show);

    assertThat(proposals)
        .allSatisfy(p -> assertThat(p.mode()).isNotEqualTo(BriefcaseCashInMode.RUN_IN));
  }

  @Test
  @DisplayName("RUN_IN: a hot rivalry (heat >= 70) proposes RUN_IN instead of PLANNED")
  void runIn_hotRivalry_proposesRunIn() {
    Rivalry rivalry = new Rivalry();
    rivalry.setHeat(85);
    lenient()
        .when(rivalryRepository.findActiveRivalryBetween(holder, champion))
        .thenReturn(Optional.of(rivalry));
    lenient()
        .when(segmentRepository.findByTitle(title))
        .thenReturn(List.of(segmentDefendedOn(GAME_DATE.minusDays(21))));
    opportunity.setExpiryDate(GAME_DATE.plusDays(40));

    List<BriefcaseCashInAdvisorService.BriefcaseProposal> proposals = advisor.proposalsFor(show);

    assertThat(proposals).hasSize(1);
    assertThat(proposals.get(0).mode()).isEqualTo(BriefcaseCashInMode.RUN_IN);
  }

  @Test
  @DisplayName("RUN_IN: heat just below the threshold proposes PLANNED")
  void runIn_heatBelowThreshold_proposesPlanned() {
    Rivalry rivalry = new Rivalry();
    rivalry.setHeat(69);
    lenient()
        .when(rivalryRepository.findActiveRivalryBetween(holder, champion))
        .thenReturn(Optional.of(rivalry));
    lenient()
        .when(segmentRepository.findByTitle(title))
        .thenReturn(List.of(segmentDefendedOn(GAME_DATE.minusDays(21))));
    opportunity.setExpiryDate(GAME_DATE.plusDays(40));

    List<BriefcaseCashInAdvisorService.BriefcaseProposal> proposals = advisor.proposalsFor(show);

    assertThat(proposals).hasSize(1);
    assertThat(proposals.get(0).mode()).isEqualTo(BriefcaseCashInMode.PLANNED);
  }
}
