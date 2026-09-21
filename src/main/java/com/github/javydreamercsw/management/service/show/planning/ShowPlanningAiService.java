/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.show.planning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.service.HolidayService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.planning.dto.AiGeneratedSegmentDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.FeudScriptBeatDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.TournamentSlotPreviewDTO;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ShowPlanningAiService {

  private final SegmentNarrationServiceFactory narrationServiceFactory;
  private final ObjectMapper objectMapper;
  private final SegmentTypeService segmentTypeService;
  private final SegmentRuleService segmentRuleService;
  private final HolidayService holidayService;
  private final ShowPlanningPromptBuilder promptBuilder;

  @Autowired
  ShowPlanningAiService(
      final SegmentNarrationServiceFactory narrationServiceFactory,
      final ObjectMapper objectMapper,
      final SegmentTypeService segmentTypeService,
      final SegmentRuleService segmentRuleService,
      final HolidayService holidayService) {
    this.narrationServiceFactory = narrationServiceFactory;
    this.objectMapper = objectMapper;
    this.segmentTypeService = segmentTypeService;
    this.segmentRuleService = segmentRuleService;
    this.holidayService = holidayService;
    this.promptBuilder =
        new ShowPlanningPromptBuilder(segmentTypeService, segmentRuleService, holidayService);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public ProposedShow planShow(@NonNull final ShowPlanningContextDTO context) {
    int expectedMatches = 0;
    int expectedPromos = 0;
    if (context.getShowTemplate() != null) {
      expectedMatches = Math.max(0, context.getShowTemplate().getExpectedMatches());
      expectedPromos = Math.max(0, context.getShowTemplate().getExpectedPromos());
    }
    // Beats may claim promo slots too — count each claim against the right budget.
    int[] claimed = preClaimedSlotCount(context);
    int freeMatches = Math.max(0, expectedMatches - claimed[0]);
    int freePromos = Math.max(0, expectedPromos - claimed[1]);
    if (freeMatches == 0 && freePromos == 0) {
      // Every slot on this card is pre-determined (scripted beats + tournament brackets). The AI
      // has nothing to propose — skip the call entirely; the deterministic passes build the card.
      log.info(
          "All match/promo slots on this card are pre-determined ({} claim(s)) — skipping the AI"
              + " call",
          claimed[0] + claimed[1]);
      ProposedShow proposedShow = new ProposedShow();
      applyScriptedBeats(proposedShow, context);
      applyTournamentSlots(proposedShow, context);
      applyDefaultRules(proposedShow);
      return proposedShow;
    }
    ProposedShow proposedShow = planShowWithAi(context);
    applyScriptedBeats(proposedShow, context);
    applyTournamentSlots(proposedShow, context);
    applyDefaultRules(proposedShow);
    return proposedShow;
  }

  /**
   * Match and promo slots the deterministic passes will fill without AI help: one per scripted beat
   * (a beat whose type is a promo claims a promo slot, anything else a match slot), plus one per
   * real-participant tournament preview row (always match slots; placeholder rows are additive and
   * claim nothing). Mirrors the prompt's accounting in {@link ShowPlanningPromptBuilder}.
   *
   * @return {{ claimedMatches, claimedPromos }}
   */
  private int[] preClaimedSlotCount(ShowPlanningContextDTO context) {
    int matches = 0;
    int promos = 0;
    if (context.getUpcomingScriptedBeats() != null) {
      for (FeudScriptBeatDTO beat : context.getUpcomingScriptedBeats()) {
        if (beat.getSegmentType() == null || beat.getSegmentType().isBlank()) {
          continue;
        }
        if (isPromoType(beat.getSegmentType())) {
          promos++;
        } else {
          matches++;
        }
      }
    }
    if (context.getTournamentSlots() != null) {
      matches +=
          (int)
              context.getTournamentSlots().stream()
                  .filter(
                      s ->
                          s.getTeams() != null
                              && s.getTeams().stream()
                                  .flatMap(List::stream)
                                  .anyMatch(
                                      n ->
                                          n != null
                                              && !n.isBlank()
                                              && !"Tournament bracket".equals(n)))
                  .count();
    }
    return new int[] {matches, promos};
  }

  /** Promo by well-known code when resolvable, else the lowercase-name heuristic. */
  private boolean isPromoType(String typeName) {
    return segmentTypeService
        .findByName(typeName)
        .map(type -> WellKnownSegmentType.PROMO.matches(type))
        .orElseGet(() -> typeName.toLowerCase().contains("promo"));
  }

  /**
   * Applies deterministic default stipulations where the AI (or a scripted beat) left the rules
   * empty: matches get the "Normal" rule, promos get the "Promo" rule. The prompt asks the AI to
   * omit rules on promos, so relying on it would leave the field nondeterministic — every segment
   * should carry its default rule by the time it reaches the planning grid.
   */
  private void applyDefaultRules(ProposedShow proposedShow) {
    for (ProposedSegment segment : proposedShow.getSegments()) {
      if (segment.getRules() != null && !segment.getRules().isEmpty()) {
        continue;
      }
      // isMatchSegment false ⇒ promo (or unresolvable type falls back to non-promo "Normal",
      // matching the intergender-check idiom where only explicit promos skip match semantics).
      boolean promo = !isMatchSegment(segment);
      String defaultRule = promo ? "Promo" : "Normal";
      segmentRuleService
          .findByName(defaultRule)
          .ifPresentOrElse(
              rule -> segment.setRules(List.of(rule.getName())),
              () ->
                  log.debug(
                      "No '{}' segment rule found; segment '{}' left without rules",
                      defaultRule,
                      segment.getType()));
    }
  }

  /**
   * Deterministically enforces the booker-mandated scripted beats on the AI-proposed card: any AI
   * match segment covering a beat's participants is removed and replaced by a locally-built segment
   * from the beat (exact match type, stipulation, winner control). The AI still books the rest of
   * the card around the slots. Prompt-side instructions remain as a hint, but this pass no longer
   * trusts the AI to honor them.
   */
  private void applyScriptedBeats(ProposedShow proposedShow, ShowPlanningContextDTO context) {
    List<FeudScriptBeatDTO> beats = context.getUpcomingScriptedBeats();
    if (beats == null || beats.isEmpty()) {
      return;
    }
    List<ProposedSegment> segments = proposedShow.getSegments();

    // Drop AI segments that cover a scripted slot (by rivalry id or participant overlap), so the
    // beat segment replaces them rather than duplicating the arc on the card. Match segments are
    // evicted on ANY participant overlap (the beat claims its wrestlers' slot); other segments
    // (promos) only when they cover the beat's full participant set or rivalry — a promo
    // mentioning one arc wrestler stays legitimate (ATW-978m).
    for (FeudScriptBeatDTO beat : beats) {
      Set<Long> beatIds = participantIdsOf(beat);
      Set<String> beatNames = participantNamesOf(beat);
      if (beatIds.isEmpty() && beatNames.isEmpty() && beat.getRivalryId() == null) {
        continue;
      }
      segments.removeIf(segment -> isBeatSlotCovered(segment, beat, beatIds, beatNames));
    }

    // Insert locally-built beat segments ahead of the AI's card.
    List<ProposedSegment> beatSegments = new ArrayList<>();
    for (FeudScriptBeatDTO beat : beats) {
      ProposedSegment beatSegment = buildBeatSegment(beat);
      if (beatSegment != null) {
        // Give the scripted slot a summary in the same shape the AI's segments carry, sourced
        // from the arc context so the planning grid reads like the rest of the card.
        beatSegment.setSummary(summarizeBeat(beat));
        beatSegment.setSource("Scripted beat");
        beatSegments.add(beatSegment);
      }
    }
    beatSegments.addAll(segments);
    proposedShow.setSegments(beatSegments);
    log.info(
        "Applied {} scripted beat(s) deterministically; card now has {} segments",
        beatSegments.size() - segments.size(),
        beatSegments.size());
  }

  /** Builds a ProposedSegment straight from the beat definition; null when unusable. */
  private ProposedSegment buildBeatSegment(FeudScriptBeatDTO beat) {
    if (beat.getSegmentType() == null || beat.getSegmentType().isBlank()) {
      return null;
    }
    ProposedSegment segment = new ProposedSegment();
    segment.setType(beat.getSegmentType());
    if (beat.getSegmentRule() != null && !beat.getSegmentRule().isBlank()) {
      segment.setRules(List.of(beat.getSegmentRule()));
    }
    segment.setRivalryId(beat.getRivalryId());
    segment.setTeams(beat.getTeamNameLists());
    segment.setTeamIds(beat.getTeamIdLists());
    if ("BOOKER_PICKS".equals(beat.getWinnerControl())
        && beat.getPlannedWinnerName() != null
        && !beat.getPlannedWinnerName().isBlank()) {
      segment.setWinners(List.of(beat.getPlannedWinnerName()));
    }
    if (beat.getNotes() != null && !beat.getNotes().isBlank()) {
      segment.setNotes(beat.getNotes());
    }
    if (beat.isTitleSegment() && beat.getTitles() != null && !beat.getTitles().isEmpty()) {
      // setTitles flips isTitleSegment on the proposal.
      segment.setTitles(new HashSet<>(beat.getTitles()));
    }
    if (beat.getContenderTitleId() != null && !beat.isTitleSegment()) {
      // The contender title itself is resolved (by id) when the proposal is approved.
      segment.setIsContenderMatch(true);
      segment.setContenderTitleId(beat.getContenderTitleId());
    }
    return segment;
  }

  /**
   * Deterministically places the show-attached tournament slots (ATW-xbn4) on the AI-proposed card,
   * mirroring the scripted-beat pass: each preview becomes a card row ahead of the AI's segments —
   * participants resolve from the bracket at approval, the row's type/rule/title flags tell the
   * booker (and the approval flow) what the slot is. AI rows that collide with a tournament slot
   * (same segment type, both match segments) are evicted so the tournament owns its slot instead of
   * double-booking the type. Deleting the row on the planning grid means "don't book it here."
   */
  private void applyTournamentSlots(ProposedShow proposedShow, ShowPlanningContextDTO context) {
    List<TournamentSlotPreviewDTO> slots = context.getTournamentSlots();
    if (slots == null || slots.isEmpty()) {
      return;
    }
    List<ProposedSegment> segments = proposedShow.getSegments();
    int before = segments.size();

    // No eviction pass (unlike beats): tournament participants come from the bracket and are
    // unknown at planning time, and type-matching would nuke legitimate AI rows on weekly shows
    // (round matches ride the generic One-on-One type). Both stay on the card — the booker sees
    // the tournament rows and deletes either kind before approving.
    List<ProposedSegment> slotRows = new ArrayList<>();
    for (TournamentSlotPreviewDTO slot : slots) {
      if (slot.getTypeName() == null || slot.getTypeName().isBlank()) {
        continue;
      }
      int matchCount = roundMatchCountOf(slot.getShape());
      // Multi-match previews expand into that many single rows — each books one bracket match,
      // and removing one row removes exactly one match.
      for (int i = 0; i < matchCount; i++) {
        ProposedSegment segment = new ProposedSegment();
        segment.setType(slot.getTypeName());
        if (slot.getRuleName() != null && !slot.getRuleName().isBlank()) {
          segment.setRules(List.of(slot.getRuleName()));
        }
        segment.setSummary(summarizeTournamentSlot(slot));
        segment.setNotes(
            slot.getTournamentName()
                + " — "
                + slot.getShape()
                + " on this show. Participants come from the tournament bracket at approval;"
                + " delete this row to skip this slot on this show.");
        segment.setSource("Tournament");
        // Real match-up when the bracket can supply one (seeded brackets preview actual
        // pairings); placeholder teams otherwise — participants always re-derive from the
        // bracket at approval, so the card's names are informational.
        if (slot.getTeams() != null && !slot.getTeams().isEmpty()) {
          segment.setTeams(slot.getTeams());
          // Winner names are unknowable at preview time — leave winners empty (AI_PICKS
          // semantics); the bracket result mirrors the booked segment's actual winners.
        }
        if (slot.getTitleName() != null && !slot.getTitleName().isBlank()) {
          segment.setIsTitleSegment(true);
        }
        slotRows.add(segment);
      }
    }
    // Tournament slots land ahead of the AI's card, like beats.
    slotRows.addAll(segments);
    proposedShow.setSegments(slotRows);
    log.info(
        "Applied {} tournament slot row(s) deterministically; card now has {} segments",
        segments.size() - before,
        segments.size());
  }

  /** Grid-facing summary for a tournament slot: tournament name plus the slot's shape. */
  private String summarizeTournamentSlot(TournamentSlotPreviewDTO slot) {
    StringBuilder sb = new StringBuilder();
    sb.append("Tournament: ").append(slot.getTournamentName());
    sb.append(" — ").append(slot.getShape());
    if (slot.getTitleName() != null && !slot.getTitleName().isBlank()) {
      sb.append(" (").append(slot.getTitleName()).append(" on the line)");
    }
    return sb.toString();
  }

  /** "N round matches" → N; any other shape ("Payoff final", "Champion showcase") → 1. */
  private int roundMatchCountOf(String shape) {
    if (shape == null) {
      return 1;
    }
    var matcher = java.util.regex.Pattern.compile("(\\d+) round matches?").matcher(shape);
    return matcher.find() ? Math.max(1, Integer.parseInt(matcher.group(1))) : 1;
  }

  /** Grid-facing summary for a scripted slot: arc name plus planned-winner intent. */
  private String summarizeBeat(FeudScriptBeatDTO beat) {
    StringBuilder sb = new StringBuilder();
    sb.append("Scripted beat: ").append(beat.getScriptName());
    if (beat.getSegmentType() != null && !beat.getSegmentType().isBlank()) {
      sb.append(" (").append(beat.getSegmentType()).append(")");
    }
    if ("BOOKER_PICKS".equals(beat.getWinnerControl())
        && beat.getPlannedWinnerName() != null
        && !beat.getPlannedWinnerName().isBlank()) {
      sb.append(" — planned winner: ").append(beat.getPlannedWinnerName());
    }
    return sb.toString();
  }

  private boolean isMatchSegment(ProposedSegment segment) {
    if (segment.getType() == null) {
      return false;
    }
    return segmentTypeService
        .findByName(segment.getType())
        .map(type -> !WellKnownSegmentType.PROMO.matches(type))
        .orElseGet(() -> !segment.getType().toLowerCase().contains("promo"));
  }

  private boolean coversAnyParticipant(ProposedSegment segment, Set<Long> beatIds) {
    return segment.getTeamIds() != null
        && segment.getTeamIds().stream().flatMap(List::stream).anyMatch(beatIds::contains);
  }

  /**
   * True when an AI segment occupies a scripted beat's slot. Signals, in order: the segment carries
   * the beat's rivalry id (any segment kind — that rivalry is arc-reserved); a match segment
   * overlaps the beat's participants by id (a wrestler cannot be double-booked into two matches —
   * established ATW-iukb behavior); or the segment covers the beat's full participant set
   * case-insensitively by name — AI proposals often carry names only, with teamIds reconciled later
   * at approval. Scoped to the beat's own rivalry and participant set: an AI promo sharing one
   * wrestler with the beat is legitimate (participation goal) and must survive.
   */
  private boolean isBeatSlotCovered(
      ProposedSegment segment, FeudScriptBeatDTO beat, Set<Long> beatIds, Set<String> beatNames) {
    if (beat.getRivalryId() != null && beat.getRivalryId().equals(segment.getRivalryId())) {
      return true;
    }
    if (isMatchSegment(segment) && coversAnyParticipant(segment, beatIds)) {
      return true;
    }
    return coversAllParticipantsByName(segment, beatNames);
  }

  /** Case-insensitive full-coverage check of the beat's participant names on a segment. */
  private boolean coversAllParticipantsByName(ProposedSegment segment, Set<String> beatNames) {
    if (beatNames.isEmpty() || segment.getTeams() == null) {
      return false;
    }
    Set<String> segmentNames =
        segment.getTeams().stream()
            .flatMap(List::stream)
            .filter(name -> name != null)
            .map(name -> name.trim().toLowerCase())
            .collect(Collectors.toSet());
    return segmentNames.containsAll(beatNames);
  }

  private Set<Long> participantIdsOf(FeudScriptBeatDTO beat) {
    Set<Long> ids = new HashSet<>();
    if (beat.getParticipantIdLists() != null) {
      beat.getParticipantIdLists().stream().flatMap(List::stream).forEach(ids::add);
    }
    return ids;
  }

  /** Beat participant names in canonical lowercase form, for name-only AI duplicates. */
  private Set<String> participantNamesOf(FeudScriptBeatDTO beat) {
    Set<String> names = new HashSet<>();
    if (beat.getTeamNameLists() != null) {
      beat.getTeamNameLists().stream()
          .flatMap(List::stream)
          .filter(name -> name != null && !name.isBlank())
          .forEach(name -> names.add(name.trim().toLowerCase()));
    }
    return names;
  }

  private ProposedShow planShowWithAi(@NonNull final ShowPlanningContextDTO context) {
    if (narrationServiceFactory.getBestAvailableService() == null) {
      log.warn("No AI service available for show planning.");
      return new ProposedShow();
    }

    String prompt = promptBuilder.build(context);
    log.debug("Sending prompt to AI: {}", prompt);

    String aiResponse = narrationServiceFactory.generateText(prompt);
    log.debug("Received response from AI: {}", aiResponse);

    if (aiResponse == null || aiResponse.trim().isEmpty()) {
      log.warn("AI returned an empty or null response for show planning.");
      return new ProposedShow();
    }

    try {
      // Attempt to extract JSON array from the response, as AI might include conversational text
      String jsonString = extractJsonArray(aiResponse);
      if (jsonString == null) {
        log.error("Could not extract JSON array from AI response. Length: {}", aiResponse.length());
        log.debug("Raw response: {}", aiResponse);
        throw new ShowPlanningException("Could not extract JSON array from AI response");
      }

      log.debug("Extracted JSON (length {}): {}", jsonString.length(), jsonString);

      List<AiGeneratedSegmentDTO> aiSegments =
          objectMapper.readValue(
              jsonString,
              objectMapper
                  .getTypeFactory()
                  .constructCollectionType(List.class, AiGeneratedSegmentDTO.class));
      log.info("Successfully parsed {} segments from AI response", aiSegments.size());

      List<ProposedSegment> proposedSegments =
          aiSegments.stream()
              .map(
                  dto -> {
                    ProposedSegment segment = new ProposedSegment();
                    segment.setType(dto.getType());
                    segment.setNarration(dto.getDescription());
                    segment.setSummary(dto.getOutcome());
                    segment.setNotes(dto.getNotes());
                    segment.setRivalryId(dto.getRivalryId());
                    if (dto.getRules() != null && !dto.getRules().isEmpty()) {
                      segment.setRules(dto.getRules());
                    }
                    if (dto.getTeams() != null && !dto.getTeams().isEmpty()) {
                      segment.setTeams(dto.getTeams());
                    } else if (dto.getParticipants() != null && !dto.getParticipants().isEmpty()) {
                      segment.setTeams(
                          dto.getParticipants().stream()
                              .map(List::of)
                              .collect(Collectors.toList()));
                    }
                    if (dto.getTeamIds() != null && !dto.getTeamIds().isEmpty()) {
                      segment.setTeamIds(dto.getTeamIds());
                    } else if (dto.getParticipantIds() != null
                        && !dto.getParticipantIds().isEmpty()) {
                      segment.setTeamIds(
                          dto.getParticipantIds().stream()
                              .map(List::of)
                              .collect(Collectors.toList()));
                    }
                    return segment;
                  })
              .collect(Collectors.toList());

      ProposedShow proposedShow = new ProposedShow();
      proposedShow.setSegments(proposedSegments);
      return proposedShow;
    } catch (JsonProcessingException e) {
      log.error(
          "Failed to parse AI response into ProposedShow object. AI Response length: {}",
          aiResponse.length());
      log.error("Full AI response that failed to parse:\n{}", aiResponse);
      throw new ShowPlanningException("Failed to parse AI response", e);
    } catch (Exception e) {
      log.error("An unexpected error occurred during show planning: {}", e.getMessage(), e);
      throw new ShowPlanningException("An unexpected error occurred during show planning", e);
    }
  }

  /**
   * Extracts a JSON array from a given string. This is useful when the AI might include
   * conversational text around the JSON output.
   *
   * @param input The string potentially containing a JSON array.
   * @return The extracted JSON array string, or null if not found.
   */
  private String extractJsonArray(final String input) {
    if (input == null || input.trim().isEmpty()) {
      return null;
    }

    String cleaned = input.trim();

    // Remove markdown code blocks if present
    if (cleaned.startsWith("```")) {
      // Find the first newline or the end of the first line
      int firstNewline = cleaned.indexOf('\n');
      if (firstNewline != -1) {
        cleaned = cleaned.substring(firstNewline).trim();
      } else {
        cleaned = cleaned.substring(3).trim();
      }

      if (cleaned.endsWith("```")) {
        cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
      }
    }

    // Search for the array start and end in the cleaned string
    int startIndex = cleaned.indexOf('[');
    int endIndex = cleaned.lastIndexOf(']');

    if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
      return cleaned.substring(startIndex, endIndex + 1);
    }

    return null;
  }
}
