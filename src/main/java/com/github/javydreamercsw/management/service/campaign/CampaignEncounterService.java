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
import com.github.javydreamercsw.management.domain.campaign.CampaignPhase;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
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
  private final WrestlerRepository wrestlerRepository;
  private final TeamRepository teamRepository;
  private final FactionRepository factionRepository;
  private final ObjectMapper objectMapper;

  @Transactional
  public CampaignEncounterResponseDTO generateEncounter(Campaign campaign) {
    CampaignState state = campaign.getState();
    CampaignChapterDTO chapter =
        chapterService
            .getChapter(state.getCurrentChapterId())
            .orElseThrow(
                () ->
                    new IllegalStateException("Chapter not found: " + state.getCurrentChapterId()));

    List<CampaignEncounter> history =
        encounterRepository.findByCampaignOrderByEncounterDateAsc(campaign);

    String prompt = buildPrompt(campaign, chapter, history);

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
              .chapterId(chapter.getId())
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
    if (campaign.getWrestler().getDescription() != null) {
      sb.append("- Bio: ").append(campaign.getWrestler().getDescription()).append("\n");
    }
    sb.append("- Gender: ").append(campaign.getWrestler().getGender()).append("\n");
    sb.append("- Tier: ").append(campaign.getWrestler().getTier()).append("\n");
    if (campaign.getWrestler().getAlignment() != null) {
      sb.append("- Alignment: ")
          .append(campaign.getWrestler().getAlignment().getAlignmentType())
          .append(" (Level ")
          .append(campaign.getWrestler().getAlignment().getLevel())
          .append(")\n");
    }

    if (campaign.getWrestler().getFaction() != null) {
      sb.append("- Faction: ").append(campaign.getWrestler().getFaction().getName()).append("\n");
    }

    sb.append("- Tournament Progress: ")
        .append(campaign.getState().getWins())
        .append(" Wins, ")
        .append(campaign.getState().getLosses())
        .append(" Losses\n");

    if (campaign.getState().getRival() != null) {
      sb.append("- Current Rival: ").append(campaign.getState().getRival().getName()).append("\n");
    }

    // Check for partner ID in feature data
    try {
      if (chapter.isTagTeam()) {
        sb.append("- Tag Team Campaign: YES\n");
        // We need to access feature data manually or via CampaignService helper if public.
        // Since getFeatureValue is private in CampaignService, we assume direct access or
        // refactor.
        // However, we can read the JSON blob directly here since we have ObjectMapper.
        String featureDataJson = campaign.getState().getFeatureData();
        Long partnerId = null;
        if (featureDataJson != null) {
          java.util.Map<String, Object> data =
              objectMapper.readValue(
                  featureDataJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
          Object val = data.get("partnerId");
          if (val instanceof Number) {
            partnerId = ((Number) val).longValue();
          }
        }

        if (partnerId != null) {
          wrestlerRepository
              .findById(partnerId)
              .ifPresent(p -> sb.append("- Tag Partner: ").append(p.getName()).append("\n"));
        } else {
          sb.append("- Tag Partner: NONE (Player is looking for a partner)\n");
        }
      }

      // Check for Finals Phase in feature data
      boolean isFinalsPhase = false;
      String featureDataJson = campaign.getState().getFeatureData();
      if (featureDataJson != null) {
        java.util.Map<String, Object> data =
            objectMapper.readValue(
                featureDataJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        Object val = data.get("finalsPhase");
        if (val instanceof Boolean) {
          isFinalsPhase = (Boolean) val;
        }
      }

      if (isFinalsPhase && !chapter.isTournament()) {
        sb.append("\n*** CHAPTER FINALE PHASE ***\n");
        sb.append(
            "The player has reached the climax of this chapter. The story must now lead to the"
                + " final showdown.\n");
        if (chapter.getRules().getFinalMatchType() != null) {
          sb.append("The match type is mandated to be: ")
              .append(chapter.getRules().getFinalMatchType())
              .append(".\n");
        }
        if (chapter.getRules().getFinalMatchRules() != null
            && !chapter.getRules().getFinalMatchRules().isEmpty()) {
          sb.append("The match rules are mandated to be: ")
              .append(String.join(", ", chapter.getRules().getFinalMatchRules()))
              .append(".\n");
        }
      }
    } catch (Exception e) {
      log.error("Error parsing feature data in prompt builder", e);
    }

    if (campaign.getState().getCurrentPhase() == CampaignPhase.POST_MATCH
        && campaign.getState().getCurrentMatch() != null) {
      Segment match = campaign.getState().getCurrentMatch();
      boolean won =
          match.getWinners().stream()
              .anyMatch(w -> w.getId().equals(campaign.getWrestler().getId()));
      sb.append("\nIMMEDIATE MATCH RESULT:\n");
      sb.append("- Just competed in a ")
          .append(match.getSegmentType().getName())
          .append(" match.\n");
      if (!match.getSegmentRules().isEmpty()) {
        sb.append("- Stipulations: ");
        match
            .getSegmentRules()
            .forEach(
                rule -> {
                  sb.append(rule.getName());
                  if (rule.getDescription() != null && !rule.getDescription().isBlank()) {
                    sb.append(" (").append(rule.getDescription()).append(")");
                  }
                  sb.append("; ");
                });
        sb.append("\n");
      }
      sb.append("- Result: ").append(won ? "VICTORY" : "DEFEAT").append("\n");
      String opponents =
          match.getWrestlers().stream()
              .filter(w -> !w.getId().equals(campaign.getWrestler().getId()))
              .map(Wrestler::getName)
              .collect(java.util.stream.Collectors.joining(", "));
      sb.append("- Opponents: ").append(opponents).append("\n");
    }

    // List available NPCs for the AI to pick from
    sb.append("\nAVAILABLE ROSTER (Possible Opponents/Participants):\n");
    wrestlerRepository.findAll().stream()
        .filter(w -> !w.getName().equals(campaign.getWrestler().getName()))
        .limit(20) // Don't overwhelm but give choice
        .forEach(
            w -> {
              sb.append("- ").append(w.getName());
              sb.append(" (Tier: ").append(w.getTier());
              sb.append(", Gender: ").append(w.getGender()).append(")");
              if (w.getDescription() != null) {
                sb.append(". Bio: ").append(w.getDescription());
              }
              sb.append("\n");
            });

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

    sb.append("\nFINAL INSTRUCTIONS (CRITICAL):\n");
    if (campaign.getState().getCurrentPhase() == CampaignPhase.POST_MATCH) {
      sb.append(
          "1. Generate a 'Post-Match' narrative segment. This should be the immediate aftermath of"
              + " the match result mentioned above.\n");
      sb.append("2. Reflect the winner/loser behavior based on the player's alignment.\n");
      sb.append(
          "3. Provide choices that define the player's reaction or immediate next steps (e.g.,"
              + " backstage interview, locker room confrontation).\n");
    } else {
      sb.append("1. Generate a professional wrestling narrative segment appropriate for chapter ")
          .append(chapter.getId())
          .append(" (")
          .append(chapter.getTitle())
          .append(").\n");
      sb.append("2. Provide exactly 2 or 3 distinct choices for the player.\n");
    }
    sb.append(
        "3. For each choice, define the 'alignmentShift' (positive value moves toward Babyface,"
            + " negative toward Heel), 'vpReward' (Victory Points granted immediately, ONLY for"
            + " choices that lead to a MATCH), and"
            + " 'nextPhase' (MATCH, POST_MATCH, or BACKSTAGE). Use BACKSTAGE to end the current"
            + " story loop and return to management.\n");
    sb.append(
        "4. If 'nextPhase' is MATCH, you may optionally provide a 'forcedOpponentName' (string) if"
            + " the story dictates a specific opponent from the ROSTER above. Also provide a"
            + " 'matchType' (string) from this list: ['One on One', 'Tag Team', 'Free-for-All',"
            + " 'Abu Dhabi Rumble', 'Promo', 'Handicap Match', 'Faction Beatdown',"
            + " 'GM Office Confrontation', 'Performance Review']. Defaults to 'One on One'.\n");
    sb.append(
        "5. If 'nextPhase' is MATCH, you may also provide a 'segmentRules' (list of strings) for"
            + " special stipulations (e.g., ['No DQ', 'Cage Match', 'Submission Only']). Available"
            + " rules: Normal, Hardcore, Submission, No DQ, Cage, Ladder, Table, Last Man Standing,"
            + " Iron Man.\n");

    Long currentPartnerId = null;
    try {
      if (campaign.getState().getFeatureData() != null) {
        java.util.Map<String, Object> data =
            objectMapper.readValue(
                campaign.getState().getFeatureData(),
                new com.fasterxml.jackson.core.type.TypeReference<>() {});
        Object val = data.get("partnerId");
        if (val instanceof Number) {
          currentPartnerId = ((Number) val).longValue();
        }
      }
    } catch (Exception e) {
      log.error("Error reading partnerId from feature data", e);
    }

    if (chapter.isTagTeam() && currentPartnerId == null) {
      sb.append(
          "6. The player is currently looking for a Tag Team partner. Generate narrative and"
              + " choices that involve scouting, teaming up with, or impressing a potential partner"
              + " from the ROSTER (who is not already in a team/faction).\n");
    }

    if ("fighting_champion".equals(chapter.getId())) {
      sb.append(
          "7. In this chapter (Open Challenge), when nextPhase is MATCH, you MUST select a random"
              + " credible opponent from the ROSTER above as the one who answers the challenge.\n");
    }

    sb.append(
        "8. IMPORTANT: Your response MUST be a valid JSON object. Do not include any conversational"
            + " filler before or after the JSON.\n");
    sb.append("REQUIRED JSON STRUCTURE:\n");
    sb.append("{\n");
    sb.append("  \"narrative\": \"The story text here...\",\n");
    sb.append("  \"choices\": [\n");
    sb.append(
        "    { \"text\": \"Full choice description\", \"label\": \"Short button label\","
            + " \"alignmentShift\": 1, \"vpReward\": 5, \"nextPhase\": \"MATCH\","
            + " \"forcedOpponentName\": null, \"matchType\": \"One on One\", \"segmentRules\":"
            + " [\"No DQ\"] }\n");
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

    // Only allow VP reward if it leads to a match
    int finalVp = "MATCH".equals(choice.getNextPhase()) ? choice.getVpReward() : 0;
    latest.setVpReward(finalVp);
    encounterRepository.save(latest);

    // Apply consequences
    campaignService.shiftAlignment(campaign, choice.getAlignmentShift());

    CampaignState state = campaign.getState();
    state.setVictoryPoints(state.getVictoryPoints() + finalVp);
    stateRepository.save(state);

    log.info(
        "Recorded choice for wrestler {}: {}. Alignment shift: {}",
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
