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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignPhase;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStatus;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.SegmentParticipantRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

  @Mock private CampaignRepository campaignRepository;
  @Mock private CampaignStateRepository campaignStateRepository;
  @Mock private WrestlerAlignmentRepository wrestlerAlignmentRepository;
  @Mock private CampaignChapterService chapterService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private ShowService showService;
  @Mock private SegmentService segmentService;
  @Mock private SeasonRepository seasonRepository;
  @Mock private SegmentTypeRepository segmentTypeRepository;
  @Mock private ShowTypeRepository showTypeRepository;
  @Mock private SegmentParticipantRepository participantRepository;
  @Mock private SegmentRuleRepository segmentRuleRepository;
  @Mock private SegmentRepository segmentRepository;
  @Mock private TournamentService tournamentService;
  @Mock private ShowTemplateRepository showTemplateRepository;

  @InjectMocks private CampaignService campaignService;

  @Test
  void testStartCampaign() {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);

    when(campaignRepository.save(any(Campaign.class))).thenAnswer(i -> i.getArguments()[0]);
    lenient()
        .when(campaignRepository.findActiveByWrestler(any()))
        .thenReturn(java.util.Optional.empty());
    lenient().when(wrestlerAlignmentRepository.findByWrestler(any())).thenReturn(Optional.empty());
    lenient().when(wrestlerAlignmentRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    Campaign campaign = campaignService.startCampaign(wrestler);

    assertThat(campaign).isNotNull();
    assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
    assertThat(campaign.getState()).isNotNull();
    // Chapter ID depends on mock behavior, but we verified the logic
    assertThat(campaign.getState().getPendingL1Picks()).isZero(); // Neutral start

    verify(campaignRepository, org.mockito.Mockito.atLeastOnce()).save(any(Campaign.class));
    verify(campaignStateRepository).save(any(CampaignState.class));
  }

  @Test
  void testShiftAlignment_NeutralToFace() {
    Wrestler wrestler = new Wrestler();
    Campaign campaign = new Campaign();
    campaign.setWrestler(wrestler);

    CampaignState state = new CampaignState();
    state.setActiveCards(new ArrayList<>());
    campaign.setState(state);

    WrestlerAlignment alignment = new WrestlerAlignment();
    alignment.setAlignmentType(AlignmentType.NEUTRAL);
    alignment.setLevel(0);

    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    campaignService.shiftAlignment(campaign, 1);

    assertThat(alignment.getAlignmentType()).isEqualTo(AlignmentType.FACE);
    assertThat(alignment.getLevel()).isEqualTo(1);
    verify(wrestlerAlignmentRepository).save(alignment);
  }

  @Test
  void testShiftAlignment_FaceToNeutral() {
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

    campaignService.shiftAlignment(campaign, -1);
    assertThat(alignment.getAlignmentType()).isEqualTo(AlignmentType.NEUTRAL);
    assertThat(alignment.getLevel()).isZero();
  }

  @Test
  void testShiftAlignment_HeelDeepening() {
    Wrestler wrestler = new Wrestler();
    Campaign campaign = new Campaign();
    campaign.setWrestler(wrestler);

    CampaignState state = new CampaignState();
    state.setActiveCards(new ArrayList<>());
    campaign.setState(state);

    WrestlerAlignment alignment = new WrestlerAlignment();
    alignment.setAlignmentType(AlignmentType.HEEL);
    alignment.setLevel(2);

    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    // Shift toward Heel (-1)
    campaignService.shiftAlignment(campaign, -1);
    assertThat(alignment.getAlignmentType()).isEqualTo(AlignmentType.HEEL);
    assertThat(alignment.getLevel()).isEqualTo(3);
  }

  @Test
  void testCreateMatchForEncounter() {
    Wrestler player = new Wrestler();
    player.setName("Player");

    Wrestler opponent = new Wrestler();
    opponent.setName("Opponent");

    Campaign campaign = new Campaign();
    campaign.setWrestler(player);

    CampaignState state = new CampaignState();
    campaign.setState(state);

    when(wrestlerRepository.findByName("Opponent")).thenReturn(Optional.of(opponent));
    when(seasonRepository.findByName("Campaign Mode")).thenReturn(Optional.of(new Season()));

    ShowType showType = org.mockito.Mockito.mock(ShowType.class);
    when(showType.getId()).thenReturn(2L);
    when(showTypeRepository.findByName("Weekly")).thenReturn(Optional.of(showType));

    Show show = new Show();
    show.setId(50L);
    when(showService.createShow(any(), any(), any(), any(), any(), any())).thenReturn(show);

    SegmentType segmentType = org.mockito.Mockito.mock(SegmentType.class);
    lenient().when(segmentType.getId()).thenReturn(1L);

    when(segmentTypeRepository.findByName("One on One")).thenReturn(Optional.of(segmentType));

    SegmentRule normalRule = new SegmentRule();
    normalRule.setName("Normal");
    when(segmentRuleRepository.findByName("Normal")).thenReturn(Optional.of(normalRule));

    Segment segment = new Segment();
    segment.setId(100L);
    when(segmentService.createSegment(any(), any(), any())).thenReturn(segment);

    Segment result =
        campaignService.createMatchForEncounter(
            campaign, "Opponent", "Test Narration", "One on One", "Normal");

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(100L);
    assertThat(result.getNarration()).isEqualTo("Test Narration");
    assertThat(state.getCurrentMatch()).isEqualTo(segment);
    verify(participantRepository, org.mockito.Mockito.times(2)).save(any());
  }

  @Test
  void testHandleLevelChange_NeutralToFace() {
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

    // Transition from 0 to 1
    campaignService.handleLevelChange(campaign, 0, 1);

    // Should gain a Level 1 pick
    assertThat(state.getPendingL1Picks()).isEqualTo(1);
    verify(campaignStateRepository).save(state);
  }

  @Test
  void testProcessMatchResult_Chapter2() {
    Campaign campaign = new Campaign();
    campaign.setId(1L);
    Wrestler wrestler = new Wrestler();
    campaign.setWrestler(wrestler);
    CampaignState state = new CampaignState();
    state.setCurrentChapterId("tournament");

    Segment match = new Segment();
    Show show = new Show();
    match.setShow(show);
    state.setCurrentMatch(match);

    campaign.setState(state);

    when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

    // Ensure alignment is present
    WrestlerAlignment alignment = new WrestlerAlignment();
    alignment.setAlignmentType(AlignmentType.NEUTRAL);
    alignment.setLevel(0);
    when(wrestlerAlignmentRepository.findByWrestler(any())).thenReturn(Optional.of(alignment));

    CampaignChapterDTO chapter =
        CampaignChapterDTO.builder()
            .id("tournament")
            .tournament(true)
            .rules(
                CampaignChapterDTO.ChapterRules.builder()
                    .victoryPointsWin(3)
                    .victoryPointsLoss(-1)
                    .qualifyingMatches(4)
                    .minWinsToQualify(3)
                    .build())
            .build();

    when(chapterService.getChapter("tournament")).thenReturn(Optional.of(chapter));

    campaignService.processMatchResult(campaign, true); // Win 1

    assertThat(state.getWins()).isEqualTo(1);
    assertThat(state.getMatchesPlayed()).isEqualTo(1);
    assertThat(state.getVictoryPoints()).isEqualTo(3);
    assertThat(state.getCurrentPhase()).isEqualTo(CampaignPhase.POST_MATCH);

    campaignService.processMatchResult(campaign, false); // Loss 1

    assertThat(state.getLosses()).isEqualTo(1);
    assertThat(state.getMatchesPlayed()).isEqualTo(2);
    assertThat(state.getVictoryPoints()).isEqualTo(2); // 3 - 1
  }

  @Test
  void testAdvanceChapter() {
    Campaign campaign = new Campaign();
    CampaignState state = new CampaignState();
    state.setCurrentChapterId("beginning");
    campaign.setState(state);

    CampaignChapterDTO ch2 = new CampaignChapterDTO();
    ch2.setId("tournament");
    ch2.setTournament(true);
    when(chapterService.findAvailableChapters(any())).thenReturn(List.of(ch2));

    campaignService.advanceChapter(campaign);

    assertThat(state.getCurrentChapterId()).isEqualTo("tournament");
    assertThat(state.getCompletedChapterIds()).contains("beginning");
    verify(campaignStateRepository).save(state);
  }

  @Test
  void testCompletePostMatch() {
    Campaign campaign = new Campaign();
    CampaignState state = new CampaignState();
    state.setCurrentPhase(CampaignPhase.POST_MATCH);
    state.setActionsTaken(3);
    state.setCurrentMatch(new Segment());
    campaign.setState(state);

    campaignService.completePostMatch(campaign);

    assertThat(state.getCurrentPhase()).isEqualTo(CampaignPhase.BACKSTAGE);
    assertThat(state.getActionsTaken()).isZero();
    assertThat(state.getCurrentMatch()).isNull();
    verify(campaignStateRepository).save(state);
  }
}
