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
package com.github.javydreamercsw.management.service.segment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.campaign.BackstageActionHistory;
import com.github.javydreamercsw.management.domain.campaign.BackstageActionHistoryRepository;
import com.github.javydreamercsw.management.domain.campaign.BackstageActionType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.segment.promo.PromoHookDTO;
import com.github.javydreamercsw.management.dto.segment.promo.PromoOutcomeDTO;
import com.github.javydreamercsw.management.dto.segment.promo.SmartPromoResponseDTO;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SmartPromoService {

  private final SegmentNarrationServiceFactory aiFactory;
  private final ObjectMapper objectMapper;
  private final CampaignService campaignService;
  private final CampaignStateRepository campaignStateRepository;
  private final BackstageActionHistoryRepository actionHistoryRepository;
  private final SegmentRuleRepository segmentRuleRepository;

  private static final String PROMO_CONTEXT_SYSTEM_PROMPT =
      """
      You are a professional wrestling promo director.
      Your goal is to generate an engaging opening for a promo and 3-4 "Rhetorical Hooks" for the player to choose from.
      Each hook represents a different strategy (e.g., Insult the City, Challenge Honor, Pander to Fans, Intimidate).

      REQUIRED JSON STRUCTURE:
      {
        "opener": "Narrative text setting the scene (2-3 sentences).",
        "hooks": [
          {
            "hook": "Strategy Name",
            "label": "Short UI Label",
            "text": "The actual dialogue the player would say.",
            "alignmentShift": 1 (Face) or -1 (Heel),
            "difficulty": 4 (Standard 1d6 target)
          }
        ],
        "opponentName": "Name of the wrestler being addressed (if any)"
      }
      """;

  private static final String PROMO_OUTCOME_SYSTEM_PROMPT =
      """
      You are a professional wrestling promo director.
      Based on the player's chosen rhetorical hook, generate the immediate aftermath.
      Include the opponent's retort (if applicable), the crowd's reaction, and a summary of the impact.

      REQUIRED JSON STRUCTURE:
      {
        "retort": "Opponent's response (1-2 sentences).",
        "crowdReaction": "How the fans reacted (e.g., 'Nuclear heat', 'Cheering wildly').",
        "success": true/false,
        "alignmentShift": final value,
        "momentumBonus": 1-3,
        "finalNarration": "A complete summary of the segment for history records."
      }
      """;

  /**
   * Generates the initial context for a promo, including an opening narrative and rhetorical hooks.
   *
   * @param campaign The current campaign.
   * @param opponent The wrestler being addressed (optional).
   * @return The promo context.
   */
  public SmartPromoResponseDTO generatePromoContext(Campaign campaign, Wrestler opponent) {
    var aiService = aiFactory.getBestAvailableService();
    if (aiService == null || !aiService.isAvailable()) {
      log.warn("No AI service available for promo generation.");
      return createFallbackResponse(campaign, opponent);
    }

    String prompt = buildContextPrompt(campaign, opponent);
    try {
      String aiResponse = aiService.generateText(PROMO_CONTEXT_SYSTEM_PROMPT + "\n\n" + prompt);
      return parseJsonResponse(aiResponse, SmartPromoResponseDTO.class);
    } catch (Exception e) {
      log.error("Failed to generate promo context", e);
      return createFallbackResponse(campaign, opponent);
    }
  }

  /**
   * Processes the chosen hook, determines success, and generates the final outcome.
   *
   * @param campaign The current campaign.
   * @param opponent The wrestler being addressed (optional).
   * @param chosenHook The hook selected by the player.
   * @return The promo outcome.
   */
  public PromoOutcomeDTO processPromoHook(
      Campaign campaign, Wrestler opponent, PromoHookDTO chosenHook) {
    var aiService = aiFactory.getBestAvailableService();
    if (aiService == null || !aiService.isAvailable()) {
      log.warn("No AI service available for promo outcome.");
      return createFallbackOutcome(campaign, opponent, chosenHook);
    }

    String prompt = buildOutcomePrompt(campaign, opponent, chosenHook);
    try {
      String aiResponse = aiService.generateText(PROMO_OUTCOME_SYSTEM_PROMPT + "\n\n" + prompt);
      PromoOutcomeDTO outcome = parseJsonResponse(aiResponse, PromoOutcomeDTO.class);

      // Record the outcome
      recordOutcome(campaign, opponent, outcome);

      return outcome;
    } catch (Exception e) {
      log.error("Failed to generate promo outcome", e);
      return createFallbackOutcome(campaign, opponent, chosenHook);
    }
  }

  private void recordOutcome(Campaign campaign, Wrestler opponent, PromoOutcomeDTO outcome) {
    var state = campaign.getState();

    // 1. Update alignment
    if (outcome.getAlignmentShift() != 0) {
      campaignService.shiftAlignment(campaign, outcome.getAlignmentShift());
    }

    // 2. Update CampaignState
    state.setActionsTaken(state.getActionsTaken() + 1);
    state.setLastActionType(BackstageActionType.PROMO);
    state.setLastActionSuccess(outcome.isSuccess());
    state.setMomentumBonus(state.getMomentumBonus() + outcome.getMomentumBonus());
    campaignStateRepository.save(state);

    // 3. Create Segment
    createPromoSegment(campaign, opponent, outcome);

    // 4. Save History
    BackstageActionHistory history =
        BackstageActionHistory.builder()
            .campaign(campaign)
            .actionType(BackstageActionType.PROMO)
            .actionDate(LocalDateTime.now())
            .diceRolled(0) // No dice rolled for smart promos
            .successes(outcome.isSuccess() ? 1 : 0)
            .outcomeDescription(outcome.getFinalNarration())
            .build();
    actionHistoryRepository.save(history);
  }

  private void createPromoSegment(Campaign campaign, Wrestler opponent, PromoOutcomeDTO outcome) {
    try {
      var show = campaignService.getOrCreateCampaignShow(campaign);
      var promoType = campaignService.getPromoSegmentType();

      var segment = new com.github.javydreamercsw.management.domain.show.segment.Segment();
      segment.setShow(show);
      segment.setSegmentType(promoType);
      segment.setSegmentDate(java.time.Instant.now());
      segment.setNarration(outcome.getFinalNarration());
      segment.setStatus(
          com.github.javydreamercsw.management.domain.show.segment.SegmentStatus.COMPLETED);
      segment.setAdjudicationStatus(
          com.github.javydreamercsw.management.domain.AdjudicationStatus.ADJUDICATED);

      // Add participants
      segment.addParticipant(campaign.getWrestler());
      if (opponent != null) {
        segment.addParticipant(opponent);
      }

      if (outcome.isSuccess()) {
        segment.setWinners(java.util.List.of(campaign.getWrestler()));
      }

      // Add "Promo" rule
      segmentRuleRepository.findByName("Promo").ifPresent(segment::addSegmentRule);

      campaignService.saveSegment(segment);
      log.info("Created smart promo segment for campaign {}", campaign.getId());
    } catch (Exception e) {
      log.error("Failed to create promo segment", e);
    }
  }

  private String buildContextPrompt(Campaign campaign, Wrestler opponent) {
    StringBuilder sb = new StringBuilder();
    sb.append("PLAYER:\n");
    sb.append("- Name: ").append(campaign.getWrestler().getName()).append("\n");
    sb.append("- Bio: ").append(campaign.getWrestler().getDescription()).append("\n");
    if (campaign.getWrestler().getAlignment() != null) {
      sb.append("- Alignment: ")
          .append(campaign.getWrestler().getAlignment().getAlignmentType())
          .append("\n");
    }

    if (opponent != null) {
      sb.append("\nOPPONENT:\n");
      sb.append("- Name: ").append(opponent.getName()).append("\n");
      sb.append("- Bio: ").append(opponent.getDescription()).append("\n");
      if (opponent.getAlignment() != null) {
        sb.append("- Alignment: ").append(opponent.getAlignment().getAlignmentType()).append("\n");
      }
    }

    sb.append("\nTASK: Generate the opening narrative and 3-4 rhetorical hooks. Return JSON ONLY.");
    return sb.toString();
  }

  private String buildOutcomePrompt(Campaign campaign, Wrestler opponent, PromoHookDTO chosenHook) {
    StringBuilder sb = new StringBuilder();
    sb.append("PLAYER: ").append(campaign.getWrestler().getName()).append("\n");
    if (opponent != null) {
      sb.append("OPPONENT: ").append(opponent.getName()).append("\n");
    }
    sb.append("CHOSEN HOOK: ").append(chosenHook.getHook()).append("\n");
    sb.append("DIALOGUE: \"").append(chosenHook.getText()).append("\"\n");

    sb.append(
        "\nTASK: Generate the reaction, retort, and final summary. Return JSON ONLY. Success"
            + " should be based on character consistency.");
    return sb.toString();
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

  private SmartPromoResponseDTO createFallbackResponse(Campaign campaign, Wrestler opponent) {
    return SmartPromoResponseDTO.builder()
        .opener("You step into the ring, ready to speak your mind.")
        .hooks(
            java.util.List.of(
                PromoHookDTO.builder()
                    .hook("Standard Promo")
                    .label("Speak")
                    .text("I'm the best there is!")
                    .alignmentShift(0)
                    .difficulty(4)
                    .build()))
        .opponentName(opponent != null ? opponent.getName() : null)
        .build();
  }

  private PromoOutcomeDTO createFallbackOutcome(
      Campaign campaign, Wrestler opponent, PromoHookDTO hook) {
    return PromoOutcomeDTO.builder()
        .retort(opponent != null ? "Whatever you say." : null)
        .crowdReaction("The crowd reacts with mild interest.")
        .success(true)
        .alignmentShift(hook.getAlignmentShift())
        .momentumBonus(1)
        .finalNarration("You cut a standard promo.")
        .build();
  }
}
