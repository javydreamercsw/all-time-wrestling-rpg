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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStoryline;
import com.github.javydreamercsw.management.domain.campaign.CampaignStorylineRepository;
import com.github.javydreamercsw.management.domain.campaign.StorylineMilestone;
import com.github.javydreamercsw.management.domain.campaign.StorylineMilestoneRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StorylineDirectorServiceTest {

  private StorylineDirectorService storylineDirectorService;
  private SegmentNarrationServiceFactory aiFactory;
  private SegmentNarrationService aiService;
  private CampaignStorylineRepository storylineRepository;
  private StorylineMilestoneRepository milestoneRepository;
  private CampaignStateRepository stateRepository;
  private CampaignChapterService chapterService;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    aiFactory = mock(SegmentNarrationServiceFactory.class);
    aiService = mock(SegmentNarrationService.class);
    when(aiFactory.getBestAvailableService()).thenReturn(aiService);
    when(aiService.isAvailable()).thenReturn(true);

    storylineRepository = mock(CampaignStorylineRepository.class);
    milestoneRepository = mock(StorylineMilestoneRepository.class);
    stateRepository = mock(CampaignStateRepository.class);
    chapterService = mock(CampaignChapterService.class);
    objectMapper = new ObjectMapper();

    storylineDirectorService =
        new StorylineDirectorService(
            aiFactory,
            storylineRepository,
            milestoneRepository,
            stateRepository,
            chapterService,
            objectMapper);
  }

  @Test
  void testInitializeStoryline() throws Exception {
    Campaign campaign = new Campaign();
    campaign.setId(1L);
    CampaignState state = new CampaignState();
    state.setCurrentChapterId("beginning");
    campaign.setState(state);
    Wrestler player = new Wrestler();
    player.setName("Test Wrestler");
    campaign.setWrestler(player);

    CampaignChapterDTO chapter =
        CampaignChapterDTO.builder()
            .id("beginning")
            .title("The Beginning")
            .introText("Start your career.")
            .build();

    when(chapterService.getChapter("beginning")).thenReturn(Optional.of(chapter));
    when(storylineRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
    when(milestoneRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    String aiJsonResponse =
        """
        {
          "title": "The Rookie Sensation",
          "description": "Prove you're not just another face in the crowd.",
          "milestones": [
            {
              "title": "First Impression",
              "description": "Win your debut match.",
              "narrativeGoal": "Establish credibility.",
              "order": 0,
              "nextOnSuccessIndex": 1,
              "nextOnFailureIndex": 1
            },
            {
              "title": "Locker Room Respect",
              "description": "Earn respect from veterans.",
              "narrativeGoal": "Show heart.",
              "order": 1,
              "nextOnSuccessIndex": null,
              "nextOnFailureIndex": null
            }
          ]
        }
        """;

    when(aiFactory.generateText(any())).thenReturn(aiJsonResponse);

    CampaignStoryline result = storylineDirectorService.initializeStoryline(campaign);

    assertNotNull(result);
    assertEquals("The Rookie Sensation", result.getTitle());
    assertNotNull(result.getCurrentMilestone());
    assertEquals("First Impression", result.getCurrentMilestone().getTitle());
    assertEquals(
        StorylineMilestone.MilestoneStatus.ACTIVE, result.getCurrentMilestone().getStatus());

    verify(stateRepository).save(state);
    assertEquals(result, state.getActiveStoryline());
  }

  @Test
  void testEvaluateProgressAdvance() {
    Campaign campaign = new Campaign();
    campaign.setId(1L);
    CampaignState state = new CampaignState();
    campaign.setState(state);

    CampaignStoryline storyline = new CampaignStoryline();
    storyline.setTitle("Test Storyline");
    storyline.setStatus(CampaignStoryline.StorylineStatus.ACTIVE);
    state.setActiveStoryline(storyline);

    StorylineMilestone m1 = new StorylineMilestone();
    m1.setTitle("Milestone 1");
    m1.setStatus(StorylineMilestone.MilestoneStatus.ACTIVE);

    StorylineMilestone m2 = new StorylineMilestone();
    m2.setTitle("Milestone 2");
    m2.setStatus(StorylineMilestone.MilestoneStatus.PENDING);

    m1.setNextMilestoneOnSuccess(m2);

    storyline.setCurrentMilestone(m1);

    storylineDirectorService.evaluateProgress(campaign, true);

    assertEquals(StorylineMilestone.MilestoneStatus.COMPLETED, m1.getStatus());
    assertEquals(StorylineMilestone.MilestoneStatus.ACTIVE, m2.getStatus());
    assertEquals(m2, storyline.getCurrentMilestone());
    assertEquals(storyline, state.getActiveStoryline());

    verify(milestoneRepository, times(1)).save(any());
    verify(storylineRepository).save(storyline);
    verify(stateRepository).save(state);
  }

  @Test
  void testEvaluateProgressComplete() {
    Campaign campaign = new Campaign();
    campaign.setId(1L);
    CampaignState state = new CampaignState();
    campaign.setState(state);

    CampaignStoryline storyline = new CampaignStoryline();
    storyline.setTitle("Test Storyline");
    storyline.setStatus(CampaignStoryline.StorylineStatus.ACTIVE);
    state.setActiveStoryline(storyline);

    StorylineMilestone m1 = new StorylineMilestone();
    m1.setTitle("Milestone 1");
    m1.setStatus(StorylineMilestone.MilestoneStatus.ACTIVE);
    m1.setNextMilestoneOnSuccess(null);

    storyline.setCurrentMilestone(m1);

    storylineDirectorService.evaluateProgress(campaign, true);

    assertEquals(StorylineMilestone.MilestoneStatus.COMPLETED, m1.getStatus());
    assertEquals(CampaignStoryline.StorylineStatus.COMPLETED, storyline.getStatus());
    assertNull(state.getActiveStoryline());

    verify(milestoneRepository).save(m1);
    verify(storylineRepository).save(storyline);
    verify(stateRepository).save(state);
  }
}
