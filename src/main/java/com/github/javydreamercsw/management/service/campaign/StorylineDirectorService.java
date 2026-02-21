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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStoryline;
import com.github.javydreamercsw.management.domain.campaign.CampaignStorylineRepository;
import com.github.javydreamercsw.management.domain.campaign.StorylineMilestone;
import com.github.javydreamercsw.management.domain.campaign.StorylineMilestoneRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.StorylineArcDTO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorylineDirectorService {

  private final SegmentNarrationServiceFactory aiFactory;
  private final CampaignStorylineRepository storylineRepository;
  private final StorylineMilestoneRepository milestoneRepository;
  private final CampaignStateRepository stateRepository;
  private final CampaignChapterService chapterService;
  private final ObjectMapper objectMapper;

  private static final String STORYLINE_SYSTEM_PROMPT =
      """
      You are the Lead Creative Writer for All Time Wrestling.
      Your task is to generate a structured Storyline Arc for a player in Campaign Mode.
      The storyline should have a clear theme, title, and exactly 3-4 progressive milestones.

      Each milestone MUST include a 'narrativeGoal' which provides instructions for the AI Director
      during future encounters.

      Milestones can branch: 'nextOnSuccessIndex' and 'nextOnFailureIndex' should point to the
      0-based index of the next milestone in the list. Use null if the storyline ends.

      REQUIRED JSON STRUCTURE:
      {
        "title": "Storyline Title",
        "description": "High-level summary of the arc.",
        "milestones": [
          {
            "title": "Milestone Title",
            "description": "What happens in this stage.",
            "narrativeGoal": "Goal for AI encounters (e.g., 'Establish a rivalry with OMZ through backstage heat.')",
            "order": 0,
            "nextOnSuccessIndex": 1,
            "nextOnFailureIndex": 2
          }
        ]
      }
      """;

  @Transactional
  public CampaignStoryline initializeStoryline(Campaign campaign) {
    log.info("Initializing new storyline arc for campaign: {}", campaign.getId());
    CampaignState state = campaign.getState();
    CampaignChapterDTO chapter =
        chapterService.getChapter(state.getCurrentChapterId()).orElseThrow();

    String prompt =
        String.format(
            "Generate a storyline arc for Chapter: %s. Player Alignment: %s. Chapter Intro: %s",
            chapter.getTitle(),
            campaign.getWrestler().getAlignment() != null
                ? campaign.getWrestler().getAlignment().getAlignmentType()
                : "NEUTRAL",
            chapter.getIntroText());

    try {
      String aiResponse = aiFactory.generateText(STORYLINE_SYSTEM_PROMPT + "\n\n" + prompt);
      StorylineArcDTO dto = parseJsonResponse(aiResponse, StorylineArcDTO.class);

      CampaignStoryline storyline =
          CampaignStoryline.builder()
              .campaign(campaign)
              .title(dto.getTitle())
              .description(dto.getDescription())
              .startedAt(LocalDateTime.now())
              .status(CampaignStoryline.StorylineStatus.ACTIVE)
              .build();

      storyline = storylineRepository.save(storyline);

      List<StorylineMilestone> milestones = new ArrayList<>();
      for (StorylineArcDTO.MilestoneDTO mDto : dto.getMilestones()) {
        StorylineMilestone milestone =
            StorylineMilestone.builder()
                .storyline(storyline)
                .title(mDto.getTitle())
                .description(mDto.getDescription())
                .narrativeGoal(mDto.getNarrativeGoal())
                .order(mDto.getOrder())
                .status(StorylineMilestone.MilestoneStatus.PENDING)
                .build();
        milestones.add(milestoneRepository.save(milestone));
      }

      // Link branches
      for (int i = 0; i < dto.getMilestones().size(); i++) {
        StorylineArcDTO.MilestoneDTO mDto = dto.getMilestones().get(i);
        StorylineMilestone milestone = milestones.get(i);

        if (mDto.getNextOnSuccessIndex() != null
            && mDto.getNextOnSuccessIndex() < milestones.size()) {
          milestone.setNextMilestoneOnSuccess(milestones.get(mDto.getNextOnSuccessIndex()));
        }
        if (mDto.getNextOnFailureIndex() != null
            && mDto.getNextOnFailureIndex() < milestones.size()) {
          milestone.setNextMilestoneOnFailure(milestones.get(mDto.getNextOnFailureIndex()));
        }
        milestoneRepository.save(milestone);
      }

      if (!milestones.isEmpty()) {
        StorylineMilestone first = milestones.get(0);
        first.setStatus(StorylineMilestone.MilestoneStatus.ACTIVE);
        milestoneRepository.save(first);
        storyline.setCurrentMilestone(first);
      }

      storyline = storylineRepository.save(storyline);
      state.setActiveStoryline(storyline);
      stateRepository.save(state);

      return storyline;
    } catch (Exception e) {
      log.error("Failed to initialize storyline arc via AI", e);
      return createFallbackStoryline(campaign);
    }
  }

  @Transactional
  public void evaluateProgress(Campaign campaign, boolean success) {
    CampaignState state = campaign.getState();
    CampaignStoryline storyline = state.getActiveStoryline();

    if (storyline == null || storyline.getStatus() != CampaignStoryline.StorylineStatus.ACTIVE) {
      return;
    }

    StorylineMilestone current = storyline.getCurrentMilestone();
    if (current == null) return;

    log.info(
        "Evaluating progress for storyline: {}. Milestone: {}. Success: {}",
        storyline.getTitle(),
        current.getTitle(),
        success);

    current.setStatus(
        success
            ? StorylineMilestone.MilestoneStatus.COMPLETED
            : StorylineMilestone.MilestoneStatus.FAILED);
    milestoneRepository.save(current);

    StorylineMilestone next =
        success ? current.getNextMilestoneOnSuccess() : current.getNextMilestoneOnFailure();

    if (next != null) {
      next.setStatus(StorylineMilestone.MilestoneStatus.ACTIVE);
      storyline.setCurrentMilestone(next);
      log.info("Advanced to next milestone: {}", next.getTitle());
    } else {
      storyline.setStatus(CampaignStoryline.StorylineStatus.COMPLETED);
      storyline.setEndedAt(LocalDateTime.now());
      state.setActiveStoryline(null);
      log.info("Storyline arc completed: {}", storyline.getTitle());
    }

    storylineRepository.save(storyline);
    stateRepository.save(state);
  }

  @Transactional
  public void abandonStoryline(CampaignStoryline storyline) {
    if (storyline.getStatus() == CampaignStoryline.StorylineStatus.COMPLETED) {
      log.info("Storyline {} is already completed, no need to abandon.", storyline.getTitle());
      return;
    }
    log.info("Abandoning storyline: {}", storyline.getTitle());
    storyline.setStatus(CampaignStoryline.StorylineStatus.ABANDONED);
    storyline.setEndedAt(LocalDateTime.now());
    storylineRepository.save(storyline);
    
    // Clear the active storyline from campaign state
    CampaignState campaignState = storyline.getCampaign().getState();
    if (campaignState != null && campaignState.getActiveStoryline() != null && 
        campaignState.getActiveStoryline().equals(storyline)) {
        campaignState.setActiveStoryline(null);
        stateRepository.save(campaignState);
    }
  }

  private <T> T parseJsonResponse(String aiResponse, Class<T> clazz) throws Exception {
    int start = aiResponse.indexOf('{');
    int end = aiResponse.lastIndexOf('}');
    if (start == -1 || end == -1) {
      throw new RuntimeException("No JSON found in AI response");
    }
    String json = aiResponse.substring(start, end + 1).trim();
    return objectMapper.readValue(json, clazz);
  }

  private CampaignStoryline createFallbackStoryline(Campaign campaign) {
    log.warn("Using fallback storyline for campaign: {}", campaign.getId());
    CampaignStoryline storyline =
        CampaignStoryline.builder()
            .campaign(campaign)
            .title("The Hard Road")
            .description("A standard journey through the ranks.")
            .startedAt(LocalDateTime.now())
            .status(CampaignStoryline.StorylineStatus.ACTIVE)
            .build();
    storyline = storylineRepository.save(storyline);

    StorylineMilestone m1 =
        StorylineMilestone.builder()
            .storyline(storyline)
            .title("Making a Name")
            .description("Prove you belong in the ring.")
            .narrativeGoal("Establish the player as a credible competitor.")
            .order(0)
            .status(StorylineMilestone.MilestoneStatus.ACTIVE)
            .build();

    m1 = milestoneRepository.save(m1);
    storyline.setCurrentMilestone(m1);

    campaign.getState().setActiveStoryline(storyline);
    stateRepository.save(campaign.getState());

    return storylineRepository.save(storyline);
  }
}
