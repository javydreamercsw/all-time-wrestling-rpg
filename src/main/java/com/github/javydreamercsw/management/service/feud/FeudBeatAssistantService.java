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
package com.github.javydreamercsw.management.service.feud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.dto.feud.AiSuggestedOpponentDTO;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.drama.DramaEventService;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI assistant for story arc beat editing: proposes ONE external opponent for a beat, chosen from
 * the eligible roster (active, not already in the feud, not injured, gender-compatible when
 * intergender matches are disabled). The suggestion is advisory — the booker confirms or overrides
 * it in the beat editor, and the returned wrestler id is always validated against the candidate
 * list server-side.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeudBeatAssistantService {

  /** Hard cap on candidates echoed into the prompt (context budget). */
  private static final int MAX_PROMPT_CANDIDATES = 40;

  private static final String SYSTEM_PROMPT =
      """
      You are a professional wrestling booker's assistant. You will be given a story arc beat \
      and a list of candidate wrestlers (as id=name). Pick the ONE candidate who makes the most \
      compelling external participant for the beat — a surprise opponent or run-in that elevates \
      the feud. Respond with a single JSON object, no other text:
      {"wrestlerId": <id from the candidate list>, "name": "<candidate name>", "rationale": \
      "<one sentence why this opponent elevates the beat>"}
      """;

  private final SegmentNarrationServiceFactory aiFactory;
  private final ObjectMapper objectMapper;
  private final FeudScriptRepository feudScriptRepository;
  private final WrestlerService wrestlerService;
  private final GameSettingService gameSettingService;
  private final InjuryService injuryService;
  private final UniverseContextService universeContextService;
  private final DramaEventService dramaEventService;

  /**
   * Suggests one external opponent for the given beat. Throws when no AI provider is available, no
   * candidate is eligible, or the AI returns an unusable answer — callers surface the error and the
   * booker's manual selection is left untouched.
   *
   * @param script the arc (source of feud heat and participant context)
   * @param feudParticipants the beat's feud wrestlers (excluded from candidates)
   * @param segmentType the beat's segment type name (promos skip the gender filter)
   * @param segmentRule the beat's stipulation, may be null
   * @param notes the beat's notes, may be null
   */
  @Transactional(readOnly = true)
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')"
          + " or @universeAuthz.hasRoleInCurrentUniverse('BOOKER')")
  public AiSuggestedOpponentDTO suggestOpponent(
      @NonNull FeudScript script,
      @NonNull List<Wrestler> feudParticipants,
      String segmentType,
      String segmentRule,
      String notes) {
    if (aiFactory.getBestAvailableService() == null) {
      throw new IllegalStateException("No AI providers available");
    }
    // UI dialogs hand over a script detached from the render request's session; reload so the
    // LAZY rivalry/feud proxies resolve inside this transaction instead of throwing no-session.
    if (script.getId() != null) {
      script = feudScriptRepository.findById(script.getId()).orElse(script);
    }

    Set<Long> feudIds = feudParticipants.stream().map(Wrestler::getId).collect(Collectors.toSet());
    Set<Gender> feudGenders =
        feudParticipants.stream().map(Wrestler::getGender).collect(Collectors.toSet());
    boolean genderRestricted =
        !gameSettingService.isIntergenderMatchesEnabled() && !isPromo(segmentType);
    if (genderRestricted && feudGenders.size() > 1) {
      throw new IllegalStateException(
          "Intergender matches are disabled; the arc's participants are mixed-gender");
    }
    Gender requiredGender =
        genderRestricted && feudGenders.size() == 1 ? feudGenders.iterator().next() : null;

    Long universeId = universeContextService.getCurrentUniverseId();
    List<Wrestler> candidates =
        wrestlerService.findAllFiltered(null, requiredGender, universeId, null, null).stream()
            .filter(w -> !feudIds.contains(w.getId()))
            // Active injuries only — healed injuries are history, not unavailability.
            .filter(
                w -> injuryService.getActiveInjuriesForWrestler(w.getId(), universeId).isEmpty())
            .toList();
    if (candidates.isEmpty()) {
      throw new IllegalStateException("No eligible external opponents available");
    }

    String prompt =
        SYSTEM_PROMPT
            + "\n"
            + buildContext(script, feudParticipants, segmentType, segmentRule, notes, candidates);
    log.debug("Opponent proposal prompt:\n{}", prompt);

    try {
      String aiResponse = aiFactory.generateText(prompt);
      int start = aiResponse.indexOf('{');
      int end = aiResponse.lastIndexOf('}');
      if (start == -1 || end == -1 || end <= start) {
        throw new IllegalStateException("AI response did not contain valid JSON.");
      }
      AiSuggestedOpponentDTO dto =
          objectMapper.readValue(
              aiResponse.substring(start, end + 1).trim(), AiSuggestedOpponentDTO.class);
      boolean valid =
          dto.getWrestlerId() != null
              && candidates.stream().anyMatch(c -> c.getId().equals(dto.getWrestlerId()));
      if (!valid) {
        throw new IllegalStateException(
            "AI suggested an ineligible wrestler: " + dto.getWrestlerId());
      }
      log.info(
          "AI proposed opponent {} for arc '{}': {}",
          dto.getName(),
          script.getName(),
          dto.getRationale());
      return dto;
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to generate opponent suggestion", e);
      throw new IllegalStateException("Failed to generate opponent suggestion", e);
    }
  }

  private boolean isPromo(String segmentType) {
    return segmentType != null && segmentType.toLowerCase().contains("promo");
  }

  private String buildContext(
      FeudScript script,
      List<Wrestler> feudParticipants,
      String segmentType,
      String segmentRule,
      String notes,
      List<Wrestler> candidates) {
    StringBuilder sb = new StringBuilder();
    sb.append("STORY ARC: ").append(script.getName()).append("\n");
    sb.append("FEUD WRESTLERS: ")
        .append(feudParticipants.stream().map(Wrestler::getName).collect(Collectors.joining(", ")))
        .append("\n");
    sb.append("HEAT: ")
        .append(
            script.getRivalry() != null
                ? script.getRivalry().getHeat()
                : script.getFeud() != null ? script.getFeud().getHeat() : 0)
        .append("\n");
    sb.append("BEAT: [").append(segmentType != null ? segmentType : "unspecified type");
    if (segmentRule != null && !segmentRule.isBlank()) {
      sb.append(" - ").append(segmentRule);
    }
    sb.append("]");
    if (notes != null && !notes.isBlank()) {
      sb.append(" — \"").append(notes).append("\"");
    }
    sb.append("\n");

    List<DramaEvent> drama = dramaEventService.getRecentEvents();
    if (!drama.isEmpty()) {
      sb.append("RECENT DRAMA:\n");
      drama.stream()
          .limit(5)
          .forEach(
              de ->
                  sb.append("- ")
                      .append(de.getPrimaryWrestler().getName())
                      .append(": ")
                      .append(truncate(de.getDescription(), 80))
                      .append("\n"));
    }

    sb.append("CANDIDATES (pick exactly one id from this list):\n");
    Set<Long> seen = new LinkedHashSet<>();
    for (Wrestler candidate : candidates) {
      if (seen.size() >= MAX_PROMPT_CANDIDATES) {
        break;
      }
      String tier =
          candidate
              .getDefaultState()
              .map(WrestlerState::getTier)
              .map(Object::toString)
              .orElse("UNKNOWN");
      seen.add(candidate.getId());
      sb.append("- id=")
          .append(candidate.getId())
          .append(" name='")
          .append(candidate.getName())
          .append("' tier=")
          .append(tier)
          .append("\n");
    }
    return sb.toString();
  }

  private String truncate(String text, int max) {
    if (text == null) {
      return "";
    }
    return text.length() <= max ? text : text.substring(0, max - 3) + "...";
  }
}
