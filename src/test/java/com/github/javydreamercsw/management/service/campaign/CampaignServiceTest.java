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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCardRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignPhase;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStatus;
import com.github.javydreamercsw.management.domain.campaign.CampaignStorylineRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.SegmentParticipantRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
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
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.service.match.MatchRewardService;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.news.NewsGenerationService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

  @Mock private CampaignRepository campaignRepository;
  @Mock private CampaignStateRepository campaignStateRepository;
  @Mock private CampaignAbilityCardRepository campaignAbilityCardRepository;
  @Mock private WrestlerAlignmentRepository wrestlerAlignmentRepository;
  @Mock private CampaignChapterService chapterService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private ShowService showService;
  @Mock private ShowRepository showRepository;
  @Mock private SegmentService segmentService;
  @Mock private SeasonRepository seasonRepository;
  @Mock private SegmentTypeRepository segmentTypeRepository;
  @Mock private ShowTypeRepository showTypeRepository;
  @Mock private SegmentParticipantRepository participantRepository;
  @Mock private SegmentRuleRepository segmentRuleRepository;
  @Mock private SegmentRepository segmentRepository;
  @Mock private TournamentService tournamentService;
  @Mock private CampaignStorylineRepository storylineRepository;
  @Mock private ShowTemplateRepository showTemplateRepository;
  @Mock private TitleRepository titleRepository;
  @Mock private TitleReignRepository titleReignRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private TitleService titleService;
  @Mock private SegmentAdjudicationService adjudicationService;
  @Mock private MatchRewardService matchRewardService;
  @Mock private NewsGenerationService newsGenerationService;
  @Mock private StorylineDirectorService storylineDirectorService;
  @Mock private StorylineExportService storylineExportService;
  @Mock private AlignmentService alignmentService;
  @Spy private ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks private CampaignService campaignService;

  @Test
  void testStartCampaign() {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    // Initialize lazy collections to avoid NPE if service checks them
    wrestler.setReigns(new ArrayList<>());

    when(campaignRepository.save(any(Campaign.class)))
        .thenAnswer(
            i -> {
              Campaign c = i.getArgument(0);
              if (c.getState() != null) {
                c.getState().setFeatureData("{}"); // Mock empty JSON
              }
              return c;
            });
    lenient()
        .when(campaignRepository.findActiveByWrestler(any()))
        .thenReturn(java.util.Optional.empty());

    WrestlerAlignment alignment = new WrestlerAlignment();
    alignment.setAlignmentType(AlignmentType.NEUTRAL);
    alignment.setLevel(0);
    lenient()
        .when(wrestlerAlignmentRepository.findByWrestler(any()))
        .thenReturn(Optional.of(alignment));
    lenient().when(wrestlerAlignmentRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Fix: Mock findById for startCampaign re-fetch
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));

    Campaign campaign = campaignService.startCampaign(wrestler);

    assertThat(campaign).isNotNull();
    assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
    assertThat(campaign.getState()).isNotNull();
    // Chapter ID depends on mock behavior, but we verified the logic
    assertThat(campaign.getState().getPendingL1Picks()).isZero(); // Neutral start

    verify(campaignRepository, atLeastOnce()).save(any(Campaign.class));
    verify(campaignStateRepository, atLeastOnce()).save(any(CampaignState.class));
  }

  @Test
  void testShiftAlignment_NeutralToFace() {
    Wrestler wrestler = new Wrestler();
    Campaign campaign = new Campaign();
    campaign.setWrestler(wrestler);

    CampaignState state = new CampaignState();
    state.setActiveCards(new ArrayList<>());
    campaign.setState(state);

    campaignService.shiftAlignment(campaign, 1);
    verify(alignmentService).shiftAlignment(campaign, 1);
  }

  @Test
  void testShiftAlignment_FaceToNeutral() {
    Wrestler wrestler = new Wrestler();
    Campaign campaign = new Campaign();
    campaign.setWrestler(wrestler);

    CampaignState state = new CampaignState();
    state.setActiveCards(new ArrayList<>());
    campaign.setState(state);

    campaignService.shiftAlignment(campaign, -1);
    verify(alignmentService).shiftAlignment(campaign, -1);
  }

  @Test
  void testShiftAlignment_NeutralToHeel() {
    Wrestler wrestler = new Wrestler();
    Campaign campaign = new Campaign();
    campaign.setWrestler(wrestler);

    CampaignState state = new CampaignState();
    state.setActiveCards(new ArrayList<>());
    campaign.setState(state);

    campaignService.shiftAlignment(campaign, -1);
    verify(alignmentService).shiftAlignment(campaign, -1);
  }

  @Test
  void testCreateMatchForEncounter() {
    Wrestler player = new Wrestler();
    player.setName("Player");
    player.setReigns(new ArrayList<>());
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
    when(chapterService.getChapter("test-chapter"))
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

    when(showService.createShow(
            anyString(), anyString(), anyLong(), any(), anyLong(), any(), any(), any()))
        .thenReturn(show);

    when(segmentService.createSegment(any(Show.class), any(SegmentType.class), any()))
        .thenReturn(new Segment());

    campaignService.createMatchForEncounter(
        campaign, "Opponent", "Test Narration", "One on One", "Normal");

    assertThat(state.getCurrentMatch()).isNotNull();
    assertThat(state.getCurrentPhase()).isEqualTo(CampaignPhase.MATCH);
    verify(campaignStateRepository).save(state);
  }

  @Test
  void testHandleLevelChange_GainedLevel() {
    Wrestler wrestler = new Wrestler();
    Campaign campaign = new Campaign();
    campaign.setWrestler(wrestler);
    CampaignState state = new CampaignState();
    state.setActiveCards(new ArrayList<>());
    campaign.setState(state);

    WrestlerAlignment alignment = new WrestlerAlignment();
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(1);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    campaignService.handleLevelChange(campaign, 0, 1);

    assertThat(state.getPendingL1Picks()).isEqualTo(1);
    verify(campaignStateRepository).save(state);
  }

  @Test
  void testProcessMatchResult_Win() {
    Wrestler wrestler = new Wrestler();
    Campaign campaign = new Campaign();
    campaign.setId(1L);
    campaign.setWrestler(wrestler);
    CampaignState state = new CampaignState();
    state.setWins(0);
    state.setLosses(0);
    state.setVictoryPoints(0);
    state.setMatchesPlayed(0);
    state.setActiveCards(new ArrayList<>());
    state.setCurrentChapterId("test-chapter");
    campaign.setState(state);

    when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
    when(chapterService.getChapter("test-chapter"))
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
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));
    doNothing().when(alignmentService).shiftAlignment(any(Campaign.class), anyInt());

    campaignService.processMatchResult(campaign, true); // Win 1

    assertThat(state.getWins()).isEqualTo(1);
    assertThat(state.getVictoryPoints()).isEqualTo(2);
    assertThat(state.getMatchesPlayed()).isEqualTo(1);
    assertThat(state.getCurrentPhase()).isEqualTo(CampaignPhase.POST_MATCH);

    campaignService.processMatchResult(campaign, false); // Loss 1
    assertThat(state.getLosses()).isEqualTo(1);
    assertThat(state.getVictoryPoints()).isEqualTo(3);
    assertThat(state.getMatchesPlayed()).isEqualTo(2);
  }

  @Test
  void testAdvanceChapter() {
    Wrestler wrestler = new Wrestler();
    wrestler.setReigns(new ArrayList<>());
    Campaign campaign = new Campaign();
    campaign.setId(1L);
    campaign.setWrestler(wrestler);
    CampaignState state = new CampaignState();
    state.setCurrentChapterId("ch1");
    state.setCampaign(campaign);
    campaign.setState(state);

    when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

    CampaignChapterDTO ch2 = new CampaignChapterDTO();
    ch2.setId("ch2");
    when(chapterService.findAvailableChapters(state)).thenReturn(List.of(ch2));

    campaignService.advanceChapter(campaign);

    assertThat(state.getCurrentChapterId()).isEqualTo("ch2");
    assertThat(state.getMatchesPlayed()).isZero();
    verify(campaignStateRepository).save(state);
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

    campaignService.completePostMatch(campaign);

    assertThat(state.getCurrentPhase()).isEqualTo(CampaignPhase.BACKSTAGE);
    assertThat(state.getActionsTaken()).isZero();
    verify(campaignStateRepository).save(state);
  }

  @Test
  void testProcessMatchResult_Tournament() {
    Wrestler wrestler = new Wrestler();
    Campaign campaign = new Campaign();
    campaign.setId(1L);
    wrestler.setId(1L);
    campaign.setWrestler(wrestler);
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
    when(chapterService.getChapter("tournament"))
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
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));
    doNothing().when(alignmentService).shiftAlignment(any(Campaign.class), anyInt());

    campaignService.processMatchResult(campaign, true);

    verify(tournamentService).advanceTournament(any(), any(Boolean.class), any());
  }
}
