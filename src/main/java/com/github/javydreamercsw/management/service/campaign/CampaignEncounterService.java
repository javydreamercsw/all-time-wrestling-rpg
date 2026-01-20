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
import com.github.javydreamercsw.management.domain.campaign.CampaignEncounter;
import com.github.javydreamercsw.management.domain.campaign.CampaignEncounterRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.CampaignEncounterResponseDTO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignEncounterService {

  private final SegmentNarrationServiceFactory aiFactory;
  private final CampaignEncounterRepository encounterRepository;
  private final CampaignStateRepository stateRepository;
  private final CampaignChapterService chapterService;
  private final CampaignService campaignService;
  private final ObjectMapper objectMapper;

  @Transactional
  public CampaignEncounterResponseDTO generateEncounter(Campaign campaign) {
    CampaignState state = campaign.getState();
    CampaignChapterDTO chapter =
        chapterService
            .getChapter(state.getCurrentChapter())
            .orElseThrow(
                () -> new IllegalStateException("Chapter not found: " + state.getCurrentChapter()));

    List<CampaignEncounter> history =
        encounterRepository.findByCampaignOrderByEncounterDateAsc(campaign);

    String prompt = buildPrompt(campaign, chapter, history);

    try {
      String aiResponse = aiFactory.generateText(prompt);
      // Clean AI response if it contains markdown code blocks
      String json = aiResponse.replaceAll("```json", "").replaceAll("```", "").trim();
      CampaignEncounterResponseDTO response =
          objectMapper.readValue(json, CampaignEncounterResponseDTO.class);

      // Save the encounter record (choice will be updated later)
      CampaignEncounter encounter =
          CampaignEncounter.builder()
              .campaign(campaign)
              .chapterNumber(chapter.getChapterNumber())
              .narrativeText(response.getNarrative())
              .encounterDate(LocalDateTime.now())
              .build();
      encounterRepository.save(encounter);

      return response;
    } catch (Exception e) {
      log.error("Error generating AI encounter", e);
      throw new RuntimeException("Failed to generate campaign encounter", e);
    }
  }

  private String buildPrompt(
      Campaign campaign, CampaignChapterDTO chapter, List<CampaignEncounter> history) {
    StringBuilder sb = new StringBuilder();
    sb.append(chapter.getAiSystemPrompt()).append("\n\n");

    sb.append("PLAYER CONTEXT:\n");
    sb.append("- Wrestler: ").append(campaign.getWrestler().getName()).append("\n");
    sb.append("- Alignment: ")
        .append(campaign.getWrestler().getAlignment().getAlignmentType())
        .append(" (Level ")
        .append(campaign.getWrestler().getAlignment().getLevel())
        .append(")\n");
    sb.append("- Tournament Progress: ")
        .append(campaign.getState().getWins())
        .append(" Wins, ")
        .append(campaign.getState().getLosses())
        .append(" Losses\n");

    if (campaign.getState().getRival() != null) {
      sb.append("- Current Rival: ").append(campaign.getState().getRival().getName()).append("\n");
    }

    if (!history.isEmpty()) {
      sb.append("\nRECENT HISTORY (Player Decisions):\n");
      // Take last 5 for context
      int start = Math.max(0, history.size() - 5);
      for (int i = start; i < history.size(); i++) {
        CampaignEncounter e = history.get(i);
        if (e.getPlayerChoice() != null) {
          String text = e.getNarrativeText();
          String summary = text.substring(0, Math.min(100, text.length())).replace("\n", " ");
          sb.append("- Encounter: ").append(summary).append("...\n");
          sb.append("  Player Chose: ").append(e.getPlayerChoice()).append("\n");
        }
      }
    }

    sb.append("\nINSTRUCTIONS:\n");
    sb.append("1. Generate a professional wrestling narrative segment appropriate for Chapter ")
        .append(chapter.getChapterNumber())
        .append(" (")
        .append(chapter.getTitle())
        .append(").\n");
    sb.append("2. Provide exactly 2 or 3 distinct choices for the player.\n");
    sb.append(
        "3. For each choice, define the 'alignmentShift' (positive value moves toward Babyface,"
            + " negative toward Heel), 'vpReward' (Victory Points granted immediately), and"
            + " 'nextPhase' (MATCH or POST_MATCH).\n");
    sb.append(
        "4. If 'nextPhase' is MATCH, you may optionally provide a 'forcedOpponentId' if the"
            + " story dictates a specific opponent (use null if any opponent is fine).\n");
    sb.append("5. IMPORTANT: Return ONLY a valid JSON object matching the following structure:\n");
    sb.append("{\n");
    sb.append("  \"narrative\": \"The story text here...\",\n");
    sb.append("  \"choices\": [\n");
    sb.append(
        "    { \"text\": \"Full choice description\", \"label\": \"Short button label\","
            + " \"alignmentShift\": 1, \"vpReward\": 0, \"nextPhase\": \"MATCH\","
            + " \"forcedOpponentId\": null }\n");
    sb.append("  ]\n");
    sb.append("}\n");

    return sb.toString();
  }

  @Transactional
  public void recordEncounterChoice(Campaign campaign, CampaignEncounterResponseDTO.Choice choice) {
    List<CampaignEncounter> encounters =
        encounterRepository.findByCampaignOrderByEncounterDateAsc(campaign);
    if (encounters.isEmpty()) {
      throw new IllegalStateException("No encounter record found to update.");
    }

    CampaignEncounter latest = encounters.get(encounters.size() - 1);
    latest.setPlayerChoice(choice.getText());
    latest.setAlignmentShift(choice.getAlignmentShift());
    latest.setVpReward(choice.getVpReward());
    encounterRepository.save(latest);

    // Apply consequences
    campaignService.shiftAlignment(campaign, choice.getAlignmentShift());

    CampaignState state = campaign.getState();
    state.setVictoryPoints(state.getVictoryPoints() + choice.getVpReward());
    stateRepository.save(state);

    log.info(
        "Recorded choice for wrestler {}: {}. Alignment shift:જી",
        campaign.getWrestler().getName(),
        choice.getText(),
        choice.getAlignmentShift());
  }

  public java.util.Optional<CampaignEncounter> getLatestEncounter(Campaign campaign) {
    List<CampaignEncounter> encounters =
        encounterRepository.findByCampaignOrderByEncounterDateAsc(campaign);
    return encounters.isEmpty()
        ? java.util.Optional.empty()
        : java.util.Optional.of(encounters.get(encounters.size() - 1));
  }
}
