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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignEncounter;
import com.github.javydreamercsw.management.domain.campaign.CampaignEncounterRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.CampaignEncounterResponseDTO;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackstageEncounterService {

  private final SegmentNarrationServiceFactory aiFactory;
  private final CampaignEncounterRepository encounterRepository;
  private final CampaignStateRepository stateRepository;
  private final CampaignService campaignService;
  private final WrestlerRepository wrestlerRepository;
  private final ObjectMapper objectMapper;
  private final Random random = new Random();

  /**
   * Checks if a random backstage encounter should be triggered for the given campaign.
   *
   * @param campaign The active campaign
   * @return true if an encounter should be triggered
   */
  @Transactional
  public boolean shouldTriggerEncounter(Campaign campaign) {
    CampaignState state = campaign.getState();

    // Only trigger if no actions have been taken today
    if (state.getActionsTaken() != 0 || state.getCurrentGameDate() == null) {
      return false;
    }

    // Check if we've already had an encounter today
    if (state.getFeatureData() != null && !state.getFeatureData().isBlank()) {
      try {
        Map<String, Object> data =
            objectMapper.readValue(
                state.getFeatureData(), new TypeReference<Map<String, Object>>() {});
        String lastDateStr = (String) data.get("lastBackstageEncounterDate");
        if (lastDateStr != null) {
          java.time.LocalDate lastDate = java.time.LocalDate.parse(lastDateStr);
          if (lastDate.equals(state.getCurrentGameDate())) {
            log.debug("Backstage encounter already occurred today: {}", lastDate);
            return false;
          }
        }
      } catch (Exception e) {
        log.warn("Failed to parse featureData for campaign state: {}", state.getId(), e);
      }
    }

    // 20% chance to trigger
    boolean triggered = random.nextInt(100) < 20;
    if (triggered) {
      log.info("Triggering random backstage encounter for campaign: {}", campaign.getId());
      updateLastEncounterDate(state);
    }
    return triggered;
  }

    private void updateLastEncounterDate(CampaignState state) {
      if (state.getCurrentGameDate() == null) {
        log.warn("Cannot update last encounter date: currentGameDate is null");
        return;
      }
      try {
        Map<String, Object> data = new HashMap<>();
        if (state.getFeatureData() != null && !state.getFeatureData().isBlank()) {
          data = objectMapper.readValue(state.getFeatureData(), new TypeReference<Map<String, Object>>() {});
        }
        data.put("lastBackstageEncounterDate", state.getCurrentGameDate().toString());
        state.setFeatureData(objectMapper.writeValueAsString(data));
        stateRepository.save(state);
      } catch (Exception e) {
        log.error("Failed to update lastBackstageEncounterDate in featureData", e);
      }
    }

  @Transactional
  public CampaignEncounterResponseDTO generateBackstageEncounter(Campaign campaign) {
    CampaignState state = campaign.getState();
    CampaignChapterDTO chapter =
        campaignService
            .getCurrentChapter(campaign)
            .orElseThrow(
                () ->
                    new IllegalStateException("Chapter not found: " + state.getCurrentChapterId()));

    String prompt = buildBackstagePrompt(campaign, chapter);

    try {
      String aiResponse = aiFactory.generateText(prompt);
      // Clean AI response - find the first { and last } to extract JSON
      int start = aiResponse.indexOf('{');
      int end = aiResponse.lastIndexOf('}');

      if (start == -1 || end == -1 || end <= start) {
        log.error("AI response did not contain valid JSON: {}", aiResponse);
        throw new RuntimeException("AI response did not contain valid JSON.");
      }

      String json = aiResponse.substring(start, end + 1).trim();
      CampaignEncounterResponseDTO response =
          objectMapper.readValue(json, CampaignEncounterResponseDTO.class);

      // Save the encounter record (choice will be updated later)
      CampaignEncounter encounter =
          CampaignEncounter.builder()
              .campaign(campaign)
              .chapterId("BACKSTAGE_SITUATION")
              .narrativeText(response.getNarrative())
              .encounterDate(LocalDateTime.now())
              .build();
      encounterRepository.save(encounter);

      return response;
    } catch (Exception e) {
      log.error("Error generating AI backstage encounter", e);
      throw new RuntimeException("Failed to generate backstage encounter", e);
    }
  }

  private String buildBackstagePrompt(Campaign campaign, CampaignChapterDTO chapter) {
    Wrestler player = campaign.getWrestler();
    StringBuilder sb = new StringBuilder();

    sb.append("You are the Backstage Director for a professional wrestling RPG.\n");
    sb.append("Generate a unique 'Backstage Situation' for the player wrestler: ")
        .append(player.getName())
        .append(".\n\n");

    sb.append("WRESTLER CONTEXT:\n");
    sb.append("- Tier: ").append(player.getTier()).append("\n");
    if (player.getAlignment() != null) {
      sb.append("- Alignment: ")
          .append(player.getAlignment().getAlignmentType())
          .append(" (Level ")
          .append(player.getAlignment().getLevel())
          .append(")\n");
    }
    if (player.getFaction() != null) {
      sb.append("- Faction: ").append(player.getFaction().getName()).append("\n");
    }
    if (campaign.getState().getRival() != null) {
      sb.append("- Current Rival: ").append(campaign.getState().getRival().getName()).append("\n");
    }

    sb.append("\nCURRENT CHAPTER: ").append(chapter.getTitle()).append("\n");
    sb.append("CHAPTER CONTEXT: ").append(chapter.getShortDescription()).append("\n\n");

    sb.append("SITUATION EXAMPLES:\n");
    sb.append("- Meeting a veteran who offers advice or a challenge.\n");
    sb.append("- A confrontation with a rival in the locker room.\n");
    sb.append("- Being stopped for a surprise interview by a backstage reporter.\n");
    sb.append(
        "- Witnessing a dispute between other wrestlers and deciding whether to intervene.\n");
    sb.append("- A conversation with the General Manager about future opportunities.\n\n");

    sb.append("AVAILABLE ROSTER (Possible participants in the situation):\n");
    wrestlerRepository.findAll().stream()
        .filter(w -> !w.getName().equals(player.getName()))
        .limit(10)
        .forEach(
            w ->
                sb.append("- ")
                    .append(w.getName())
                    .append(" (Tier: ")
                    .append(w.getTier())
                    .append(")\n"));

    sb.append("\nINSTRUCTIONS:\n");
    sb.append("1. The situation should be concise (1-3 paragraphs).\n");
    sb.append("2. Provide 2 or 3 distinct choices for the player.\n");
    sb.append("3. Choices can result in:\n");
    sb.append("   - alignmentShift (e.g., +1 for Face behavior, -1 for Heel behavior).\n");
    sb.append("   - momentumBonus (e.g., +1 or +2 for the next match).\n");
    sb.append(
        "   - outcomeText (Narrative text explaining what happened immediately after the"
            + " choice).\n");
    sb.append("4. All choices for these situations should have nextPhase: 'BACKSTAGE'.\n");
    sb.append("5. IMPORTANT: Your response MUST be a valid JSON object.\n\n");

    sb.append("REQUIRED JSON STRUCTURE:\n");
    sb.append("{\n");
    sb.append("  \"narrative\": \"The situation description...\",\n");
    sb.append("  \"choices\": [\n");
    sb.append("    {\n");
    sb.append("      \"text\": \"The choice description (e.g., Shake hands and show respect)\",\n");
    sb.append("      \"label\": \"Respect\",\n");
    sb.append("      \"alignmentShift\": 1,\n");
    sb.append("      \"momentumBonus\": 1,\n");
    sb.append(
        "      \"outcomeText\": \"The veteran nods in approval, giving you a boost of"
            + " confidence.\",\n");
    sb.append("      \"nextPhase\": \"BACKSTAGE\"\n");
    sb.append("    }\n");
    sb.append("  ]\n");
    sb.append("}\n");

    return sb.toString();
  }

  @Transactional
  public void recordBackstageChoice(Campaign campaign, CampaignEncounterResponseDTO.Choice choice) {
    List<CampaignEncounter> encounters =
        encounterRepository.findByCampaignOrderByEncounterDateAsc(campaign);
    if (encounters.isEmpty()) {
      throw new IllegalStateException("No encounter record found to update.");
    }

    CampaignEncounter latest = encounters.get(encounters.size() - 1);
    latest.setPlayerChoice(choice.getText());
    latest.setAlignmentShift(choice.getAlignmentShift());
    encounterRepository.save(latest);

    CampaignState state = campaign.getState();

    // Apply alignment shift
    campaignService.shiftAlignment(campaign, choice.getAlignmentShift());

    // Apply momentum bonus
    state.setMomentumBonus(state.getMomentumBonus() + choice.getMomentumBonus());

    // Consume an action
    state.setActionsTaken(state.getActionsTaken() + 1);

    stateRepository.save(state);

    log.info(
        "Recorded backstage choice for {}: {}. Alignment shift: {}, Momentum bonus: {}",
        campaign.getWrestler().getName(),
        choice.getText(),
        choice.getAlignmentShift(),
        choice.getMomentumBonus());
  }
}
