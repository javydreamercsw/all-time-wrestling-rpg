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
package com.github.javydreamercsw.management.service.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignPhase;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.SegmentParticipantRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentParticipant;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.StaticEncounterDTO.StaticChoiceDTO.BonusVpCondition;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.news.NewsGenerationService;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchResultProcessorServiceTest {

  @Mock private CampaignRepository campaignRepository;
  @Mock private CampaignStateRepository campaignStateRepository;
  @Mock private WrestlerAlignmentRepository wrestlerAlignmentRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private ShowRepository showRepository;
  @Mock private SegmentRepository segmentRepository;
  @Mock private SegmentRuleRepository segmentRuleRepository;
  @Mock private SeasonRepository seasonRepository;
  @Mock private SegmentTypeRepository segmentTypeRepository;
  @Mock private ShowTypeRepository showTypeRepository;
  @Mock private ShowTemplateRepository showTemplateRepository;
  @Mock private SegmentParticipantRepository participantRepository;
  @Mock private TournamentService tournamentService;
  @Mock private TitleRepository titleRepository;
  @Mock private TitleReignRepository titleReignRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private TitleService titleService;
  @Mock private SegmentAdjudicationService adjudicationService;
  @Mock private NewsGenerationService newsGenerationService;
  @Mock private StorylineDirectorService storylineDirectorService;
  @Mock private WrestlerStatusService wrestlerStatusService;
  @Mock private FeatureDataService featureDataService;
  @Mock private CampaignService campaignService;
  @Mock private CampaignEncounterService campaignEncounterService;

  @InjectMocks private MatchResultProcessorService service;

  @BeforeEach
  void setUpFeatureDataMock() {
    // @Lazy field-injected beans are not set by @InjectMocks; inject manually
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "campaignService", campaignService);
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "campaignEncounterService", campaignEncounterService);
    org.mockito.Mockito.lenient()
        .when(
            featureDataService.getFeatureValue(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
        .thenAnswer(inv -> inv.getArgument(3));
    org.mockito.Mockito.lenient()
        .when(campaignEncounterService.getCurrentChoiceBonusConditions(any()))
        .thenReturn(Collections.emptyList());
  }

  @Test
  void testCreateMatchForEncounter() {
    Wrestler player = new Wrestler();
    player.setName("Player");
    player.setReigns(new LinkedHashSet<>());
    Campaign campaign = new Campaign();
    campaign.setId(1L);
    campaign.setWrestler(player);

    CampaignState state = new CampaignState();
    campaign.setState(state);

    when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

    Season season = new Season();
    season.setId(1L);
    Show show = new Show();
    show.setId(1L);

    state.setCurrentChapterId("test-chapter");

    when(seasonRepository.findByName("Campaign Mode")).thenReturn(Optional.of(season));
    when(campaignService.getCurrentChapter(campaign))
        .thenReturn(Optional.of(CampaignChapterDTO.builder().id("test-chapter").build()));

    SegmentType matchType = new SegmentType();
    matchType.setId(1L);
    when(segmentTypeRepository.findByName(any())).thenReturn(Optional.of(matchType));

    ShowType weekly = new ShowType();

    weekly.setId(1L);

    when(showTypeRepository.findByName("Weekly")).thenReturn(Optional.of(weekly));

    Wrestler opponent = new Wrestler();

    opponent.setName("Opponent");

    when(wrestlerRepository.findByName("Opponent")).thenReturn(Optional.of(opponent));

    when(showRepository.saveAndFlush(any())).thenReturn(show);

    when(segmentRepository.save(any(Segment.class))).thenReturn(new Segment());

    service.createMatchForEncounter(campaign, "Opponent", "Test Narration", "One on One", "Normal");

    assertThat(state.getCurrentMatch()).isNotNull();
    assertThat(state.getCurrentPhase()).isEqualTo(CampaignPhase.MATCH);
    verify(campaignStateRepository).save(state);
  }

  @Test
  void testProcessMatchResult_Win() {
    Wrestler wrestler = new Wrestler();
    Universe universe = Universe.builder().name("Test Universe").build();
    Campaign campaign = new Campaign();
    campaign.setId(1L);
    campaign.setWrestler(wrestler);
    campaign.setUniverse(universe);
    CampaignState state = new CampaignState();
    state.setWins(0);
    state.setLosses(0);
    state.setVictoryPoints(0);
    state.setMatchesPlayed(0);
    state.setActiveCards(new ArrayList<>());
    state.setCurrentChapterId("test-chapter");
    campaign.setState(state);

    when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
    when(campaignService.getCurrentChapter(campaign))
        .thenReturn(
            Optional.of(
                CampaignChapterDTO.builder()
                    .rules(
                        CampaignChapterDTO.ChapterRules.builder()
                            .victoryPointsWin(2)
                            .victoryPointsLoss(1)
                            .build())
                    .build()));

    WrestlerAlignment alignment = new WrestlerAlignment();
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(1);
    when(wrestlerAlignmentRepository.findByWrestlerAndUniverse(wrestler, universe))
        .thenReturn(Optional.of(alignment));

    service.processMatchResult(campaign, true); // Win 1

    assertThat(state.getWins()).isEqualTo(1);
    assertThat(state.getVictoryPoints()).isEqualTo(2);
    assertThat(state.getMatchesPlayed()).isEqualTo(1);
    assertThat(state.getCurrentPhase()).isEqualTo(CampaignPhase.POST_MATCH);

    service.processMatchResult(campaign, false); // Loss 1
    assertThat(state.getLosses()).isEqualTo(1);
    assertThat(state.getVictoryPoints()).isEqualTo(3);
    assertThat(state.getMatchesPlayed()).isEqualTo(2);
  }

  @Test
  void testCompletePostMatch() {
    Campaign campaign = new Campaign();
    campaign.setId(1L);
    CampaignState state = new CampaignState();
    state.setCurrentPhase(CampaignPhase.POST_MATCH);
    state.setActionsTaken(1);

    campaign.setState(state);

    when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

    service.completePostMatch(campaign);

    assertThat(state.getCurrentPhase()).isEqualTo(CampaignPhase.BACKSTAGE);
    assertThat(state.getActionsTaken()).isZero();
    verify(campaignStateRepository).save(state);
  }

  @Test
  void testProcessMatchResult_Tournament() {
    Wrestler wrestler = new Wrestler();
    Universe universe = Universe.builder().name("Test Universe").build();
    Campaign campaign = new Campaign();
    campaign.setId(1L);
    wrestler.setId(1L);
    campaign.setWrestler(wrestler);
    campaign.setUniverse(universe);
    CampaignState state = new CampaignState();
    state.setWins(0);
    state.setLosses(0);
    state.setVictoryPoints(0);
    state.setMatchesPlayed(0);
    state.setActiveCards(new ArrayList<>());
    state.setCurrentChapterId("tournament");

    Segment currentMatch = new Segment();
    currentMatch.setShow(new Show());
    state.setCurrentMatch(currentMatch);

    campaign.setState(state);

    when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
    when(campaignService.getCurrentChapter(campaign))
        .thenReturn(
            Optional.of(
                CampaignChapterDTO.builder()
                    .id("tournament")
                    .tournament(true)
                    .rules(
                        CampaignChapterDTO.ChapterRules.builder()
                            .victoryPointsWin(2)
                            .victoryPointsLoss(1)
                            .build())
                    .build()));

    WrestlerAlignment alignment = new WrestlerAlignment();
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(1);
    when(wrestlerAlignmentRepository.findByWrestlerAndUniverse(wrestler, universe))
        .thenReturn(Optional.of(alignment));

    service.processMatchResult(campaign, true);

    verify(tournamentService).advanceTournament(any(), any(Boolean.class), any());
  }

  // ── bonus VP tests ──────────────────────────────────────────────────────────

  @Test
  void bonusVp_awardedWhenFinishTypeAndCardMatch() {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setReigns(new LinkedHashSet<>());
    Campaign campaign = buildCampaignWithWrestler(wrestler, 0);

    SegmentParticipant participant = new SegmentParticipant();
    participant.setWrestler(wrestler);
    participant.setFinishType("PINFALL");
    participant.setWinningCardName("Running Powerslam");
    Segment match = new Segment();
    match.setShow(new Show());
    match.getParticipants().add(participant);
    campaign.getState().setCurrentMatch(match);

    BonusVpCondition condition =
        BonusVpCondition.builder()
            .vpReward(2)
            .description("Won by pinfall with finisher")
            .requireWin(true)
            .finishType("PINFALL")
            .requireFinisherCard(true)
            .build();
    when(campaignEncounterService.getCurrentChoiceBonusConditions(campaign.getState()))
        .thenReturn(List.of(condition));

    service.processMatchResult(campaign, true);

    // base 2 VP (win) + 2 bonus = 4
    assertThat(campaign.getState().getVictoryPoints()).isEqualTo(4);
  }

  @Test
  void bonusVp_notAwardedWhenWinRequiredAndLoss() {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setReigns(new LinkedHashSet<>());
    Campaign campaign = buildCampaignWithWrestler(wrestler, 0);

    BonusVpCondition condition =
        BonusVpCondition.builder()
            .vpReward(2)
            .description("Win-only bonus")
            .requireWin(true)
            .build();
    when(campaignEncounterService.getCurrentChoiceBonusConditions(campaign.getState()))
        .thenReturn(List.of(condition));

    service.processMatchResult(campaign, false);

    // base -1 VP (loss), no bonus
    assertThat(campaign.getState().getVictoryPoints()).isEqualTo(-1);
  }

  @Test
  void bonusVp_awardedWhenCardGroupsAllSatisfied() {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setReigns(new LinkedHashSet<>());
    Campaign campaign = buildCampaignWithWrestler(wrestler, 0);

    Map<String, Integer> played = new HashMap<>();
    played.put("German Suplex", 2);
    played.put("Olympic Slam", 1);
    SegmentParticipant participant = new SegmentParticipant();
    participant.setWrestler(wrestler);
    participant.setCardsPlayed(played);
    Segment match = new Segment();
    match.setShow(new Show());
    match.getParticipants().add(participant);
    campaign.getState().setCurrentMatch(match);

    BonusVpCondition condition =
        BonusVpCondition.builder()
            .vpReward(2)
            .description("2+ German Suplexes and the Olympic Slam")
            .requireWin(false)
            .cardGroups(
                List.of(
                    BonusVpCondition.CardExecutionGroup.builder()
                        .anyOf(List.of("German Suplex", "Series of German Suplexes"))
                        .minCount(2)
                        .build(),
                    BonusVpCondition.CardExecutionGroup.builder()
                        .anyOf(List.of("Olympic Slam"))
                        .minCount(1)
                        .build()))
            .build();
    when(campaignEncounterService.getCurrentChoiceBonusConditions(campaign.getState()))
        .thenReturn(List.of(condition));

    service.processMatchResult(campaign, false);

    // base -1 VP (loss) + 2 bonus = 1
    assertThat(campaign.getState().getVictoryPoints()).isEqualTo(1);
  }

  @Test
  void bonusVp_notAwardedWhenCardGroupPartiallyMet() {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setReigns(new LinkedHashSet<>());
    Campaign campaign = buildCampaignWithWrestler(wrestler, 0);

    Map<String, Integer> played = new HashMap<>();
    played.put("German Suplex", 1); // only 1, needs 2
    played.put("Olympic Slam", 1);
    SegmentParticipant participant = new SegmentParticipant();
    participant.setWrestler(wrestler);
    participant.setCardsPlayed(played);
    Segment match = new Segment();
    match.setShow(new Show());
    match.getParticipants().add(participant);
    campaign.getState().setCurrentMatch(match);

    BonusVpCondition condition =
        BonusVpCondition.builder()
            .vpReward(2)
            .description("2+ German Suplexes and the Olympic Slam")
            .requireWin(false)
            .cardGroups(
                List.of(
                    BonusVpCondition.CardExecutionGroup.builder()
                        .anyOf(List.of("German Suplex", "Series of German Suplexes"))
                        .minCount(2)
                        .build(),
                    BonusVpCondition.CardExecutionGroup.builder()
                        .anyOf(List.of("Olympic Slam"))
                        .minCount(1)
                        .build()))
            .build();
    when(campaignEncounterService.getCurrentChoiceBonusConditions(campaign.getState()))
        .thenReturn(List.of(condition));

    service.processMatchResult(campaign, false);

    // base -1 VP (loss), card group not fully met → no bonus
    assertThat(campaign.getState().getVictoryPoints()).isEqualTo(-1);
  }

  @Test
  void processMatchResult_alignmentFallback_whenUniverseScopedLookupReturnsEmpty() {
    Wrestler wrestler = new Wrestler();
    Universe universe = Universe.builder().name("Test Universe").build();
    Campaign campaign = new Campaign();
    campaign.setId(1L);
    campaign.setWrestler(wrestler);
    campaign.setUniverse(universe);
    CampaignState state = new CampaignState();
    state.setWins(0);
    state.setLosses(0);
    state.setVictoryPoints(0);
    state.setMatchesPlayed(0);
    state.setActiveCards(new ArrayList<>());
    state.setCurrentChapterId("test-chapter");
    campaign.setState(state);

    when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
    when(campaignService.getCurrentChapter(campaign))
        .thenReturn(
            Optional.of(
                CampaignChapterDTO.builder()
                    .rules(
                        CampaignChapterDTO.ChapterRules.builder()
                            .victoryPointsWin(2)
                            .victoryPointsLoss(-1)
                            .build())
                    .build()));

    when(wrestlerAlignmentRepository.findByWrestlerAndUniverse(wrestler, universe))
        .thenReturn(Optional.empty());
    WrestlerAlignment faceAlignment = new WrestlerAlignment();
    faceAlignment.setAlignmentType(AlignmentType.FACE);
    faceAlignment.setLevel(1);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler))
        .thenReturn(Optional.of(faceAlignment));

    service.processMatchResult(campaign, true);

    assertThat(state.isPromoUnlocked())
        .as("Face alignment found via fallback must unlock Promo action")
        .isTrue();
    assertThat(state.isAttackUnlocked()).isFalse();
  }

  private Campaign buildCampaignWithWrestler(final Wrestler wrestler, final int initialVp) {
    Universe universe = Universe.builder().name("Test Universe").build();
    Campaign campaign = new Campaign();
    campaign.setId(1L);
    campaign.setWrestler(wrestler);
    campaign.setUniverse(universe);
    CampaignState state = new CampaignState();
    state.setWins(0);
    state.setLosses(0);
    state.setVictoryPoints(initialVp);
    state.setMatchesPlayed(0);
    state.setActiveCards(new ArrayList<>());
    state.setCurrentChapterId("test-chapter");
    campaign.setState(state);

    when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
    when(campaignService.getCurrentChapter(campaign))
        .thenReturn(
            Optional.of(
                CampaignChapterDTO.builder()
                    .rules(
                        CampaignChapterDTO.ChapterRules.builder()
                            .victoryPointsWin(2)
                            .victoryPointsLoss(-1)
                            .build())
                    .build()));
    WrestlerAlignment alignment = new WrestlerAlignment();
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(1);
    when(wrestlerAlignmentRepository.findByWrestlerAndUniverse(wrestler, universe))
        .thenReturn(Optional.of(alignment));
    return campaign;
  }
}
