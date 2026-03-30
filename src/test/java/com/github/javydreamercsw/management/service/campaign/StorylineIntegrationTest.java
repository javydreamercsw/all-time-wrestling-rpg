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
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCardRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStoryline;
import com.github.javydreamercsw.management.domain.campaign.CampaignStorylineRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.SegmentParticipantRepository;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.news.NewsGenerationService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorylineIntegrationTest {

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
  @Mock private NewsGenerationService newsGenerationService;
  @Mock private StorylineDirectorService storylineDirectorService;
  @Mock private StorylineExportService storylineExportService;
  @Mock private AlignmentService alignmentService;
  private ObjectMapper objectMapper = new ObjectMapper();

  private CampaignService campaignService;

  @BeforeEach
  void setUp() {
    campaignService =
        new CampaignService(
            campaignRepository,
            campaignStateRepository,
            campaignAbilityCardRepository,
            wrestlerAlignmentRepository,
            chapterService,
            alignmentService,
            wrestlerRepository,
            showService,
            showRepository,
            segmentService,
            segmentRepository,
            segmentRuleRepository,
            seasonRepository,
            segmentTypeRepository,
            showTypeRepository,
            showTemplateRepository,
            participantRepository,
            tournamentService,
            storylineRepository,
            titleRepository,
            titleReignRepository,
            teamRepository,
            titleService,
            adjudicationService,
            newsGenerationService,
            storylineDirectorService,
            storylineExportService,
            objectMapper);
  }

  @Test
  void testGetCurrentChapterWithAiStoryline() {
    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Wrestler");

    Campaign campaign = new Campaign();
    campaign.setWrestler(wrestler);

    CampaignState state = new CampaignState();
    state.setCampaign(campaign);
    state.setCurrentChapterId("The Unbreakable Spirit"); // AI Title

    CampaignStoryline activeStoryline = new CampaignStoryline();
    activeStoryline.setTitle("The Unbreakable Spirit");
    activeStoryline.setDescription("AI Storyline Description");
    state.setActiveStoryline(activeStoryline);

    campaign.setState(state);

    // Mock chapterService to NOT find this ID (simulating it's not a static chapter)
    when(chapterService.getChapter("The Unbreakable Spirit")).thenReturn(Optional.empty());

    // Mock storylineExportService to convert the AI storyline to a DTO
    CampaignChapterDTO aiChapterDTO =
        CampaignChapterDTO.builder()
            .id("the_unbreakable_spirit")
            .title("The Unbreakable Spirit")
            .shortDescription("AI Storyline Description")
            .build();
    when(storylineExportService.toChapterDTO(activeStoryline)).thenReturn(aiChapterDTO);

    // Act
    Optional<CampaignChapterDTO> result = campaignService.getCurrentChapter(campaign);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get().getTitle()).isEqualTo("The Unbreakable Spirit");
    assertThat(result.get().getShortDescription()).isEqualTo("AI Storyline Description");
  }

  @Test
  void testAdvanceToAiStoryline() {
    Wrestler wrestler = new Wrestler();
    wrestler.setReigns(new ArrayList<>());
    Campaign campaign = new Campaign();
    campaign.setId(1L);
    campaign.setWrestler(wrestler);

    CampaignState state = new CampaignState();
    state.setCurrentChapterId("ch1");
    state.setCompletedChapterIds(new ArrayList<>());
    state.setCampaign(campaign);
    campaign.setState(state);

    when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

    // Simulate no available predefined chapters
    when(chapterService.findAvailableChapters(state)).thenReturn(List.of());

    // Mock AI director initializing a new storyline
    CampaignStoryline newStoryline = new CampaignStoryline();
    newStoryline.setTitle("AI Generated Arc");
    when(storylineDirectorService.initializeStoryline(any(), any())).thenReturn(newStoryline);

    // Act
    Optional<String> nextChapterId = campaignService.advanceChapter(campaign);

    // Assert
    assertThat(nextChapterId).isPresent();
    assertThat(nextChapterId.get()).isEqualTo("AI Generated Arc");
    assertThat(state.getCurrentChapterId()).isEqualTo("AI Generated Arc");
    assertThat(state.getActiveStoryline()).isEqualTo(newStoryline);
    assertThat(state.getCompletedChapterIds()).contains("ch1");
  }

  @Test
  void testAdvanceToAiStoryline_WithMissingContext() {
    Wrestler wrestler = new Wrestler();
    wrestler.setReigns(new ArrayList<>());
    Campaign campaign = new Campaign();
    campaign.setId(1L);
    campaign.setWrestler(wrestler);

    CampaignState state = new CampaignState();
    state.setCurrentChapterId("unknown_id");
    state.setCompletedChapterIds(new ArrayList<>());
    state.setCampaign(campaign);
    campaign.setState(state);

    when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

    // Simulate current chapter lookup failing
    when(chapterService.getChapter("unknown_id")).thenReturn(Optional.empty());

    // Simulate no available predefined chapters
    when(chapterService.findAvailableChapters(state)).thenReturn(List.of());

    // Mock AI director initializing a new storyline - should succeed even with null context
    CampaignStoryline newStoryline = new CampaignStoryline();
    newStoryline.setTitle("Resilient Arc");
    when(storylineDirectorService.initializeStoryline(any(), any())).thenReturn(newStoryline);

    // Act
    Optional<String> nextChapterId = campaignService.advanceChapter(campaign);

    // Assert
    assertThat(nextChapterId).isPresent();
    assertThat(nextChapterId.get()).isEqualTo("Resilient Arc");
    assertThat(state.getCurrentChapterId()).isEqualTo("Resilient Arc");
  }
}
