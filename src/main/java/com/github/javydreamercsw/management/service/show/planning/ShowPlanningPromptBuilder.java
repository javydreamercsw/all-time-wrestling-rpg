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
package com.github.javydreamercsw.management.service.show.planning;

import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.service.HolidayService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.planning.dto.FeudScriptBeatDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningRivalryDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.TournamentSlotPreviewDTO;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ShowPlanningPromptBuilder {

  private final SegmentTypeService segmentTypeService;
  private final SegmentRuleService segmentRuleService;
  private final HolidayService holidayService;

  /** Strips characters that could be used to escape or hijack the AI prompt. */
  static String sanitize(final String value) {
    if (value == null) {
      return "";
    }
    return value.replaceAll("[\\[\\]{}|`\\\\]", "").trim();
  }

  /**
   * A rivalry is mixed-gender when its participants resolve to more than one distinct gender in the
   * roster. Participants missing from the roster (e.g. injured) contribute no information.
   */
  private static boolean isMixedGender(
      final ShowPlanningRivalryDTO rivalry, final Map<String, String> genderByName) {
    if (rivalry.getParticipants() == null) {
      return false;
    }
    Set<String> genders =
        rivalry.getParticipants().stream()
            .map(ShowPlanningPromptBuilder::sanitize)
            .map(genderByName::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    return genders.size() > 1;
  }

  public String build(@NonNull final ShowPlanningContextDTO context) {
    StringBuilder prompt = new StringBuilder();
    prompt.append(
        """
        You are a professional wrestling show planner. Your task is to create a compelling and\
         coherent show by generating a list of segments in JSON format.

        """);
    if (context.isPremiumLiveEvent()) {
      prompt.append(
          """
          **THIS IS A PREMIUM LIVE EVENT (PLE).** Apply PLE-specific booking rules (see below).

          """);
    }
    prompt.append("Here is the context for the show:\n");

    if (context.getShowTemplate() != null) {
      prompt
          .append("Show Template Name: ")
          .append(sanitize(context.getShowTemplate().getShowName()))
          .append("\n");
      prompt
          .append("Show Template Description: ")
          .append(sanitize(context.getShowTemplate().getDescription()))
          .append("\n");
      prompt
          .append("Expected Matches: ")
          .append(context.getShowTemplate().getExpectedMatches())
          .append("\n");
      prompt
          .append("Expected Promos: ")
          .append(context.getShowTemplate().getExpectedPromos())
          .append("\n");
      // Template assignments (ATW-0331): event-only types allowed on this template's shows and
      // encouraged rules the AI should prefer.
      if (!context.getShowTemplate().getEventSegmentTypes().isEmpty()) {
        prompt
            .append("Event Segment Types (special formats allowed ONLY on this show): ")
            .append(
                context.getShowTemplate().getEventSegmentTypes().stream()
                    .map(ShowPlanningPromptBuilder::sanitize)
                    .collect(Collectors.joining(", ")))
            .append("\n");
      }
      if (!context.getShowTemplate().getEncouragedRules().isEmpty()) {
        prompt
            .append("Encouraged Stipulation Matches (prefer these where appropriate): ")
            .append(
                context.getShowTemplate().getEncouragedRules().stream()
                    .map(ShowPlanningPromptBuilder::sanitize)
                    .collect(Collectors.joining(", ")))
            .append("\n");
      }
    }

    if (context.getShowDate() != null) {
      Optional<String> holidayTheme = holidayService.getHolidayTheme(context.getShowDate());
      holidayTheme.ifPresent(
          theme ->
              prompt
                  .append("\nHoliday Theme: ")
                  .append(sanitize(theme))
                  .append(
                      """
                      . Please incorporate this theme into the show's segments where\
                       appropriate. Including, but not limited to, commentators mentioning\
                       them, wrestlers, referencing them in promos and having matches with\
                       the themes.
                      """));
    }

    if (context.getRecentSegments() != null && !context.getRecentSegments().isEmpty()) {
      prompt.append("Recent Segments (last 3 weeks, up to 20):\n");
      context.getRecentSegments().stream()
          .limit(20)
          .forEach(
              segment -> {
                prompt
                    .append("- Type: ")
                    .append(
                        segment.getSegmentType() != null
                            ? sanitize(segment.getSegmentType())
                            : "Unknown")
                    .append(", Name: ")
                    .append(sanitize(segment.getName()))
                    .append(", Participants: ")
                    .append(
                        segment.getParticipants().stream()
                            .map(ShowPlanningPromptBuilder::sanitize)
                            .collect(Collectors.joining(", ")));
                if (segment.getWinners() != null && !segment.getWinners().isEmpty()) {
                  prompt
                      .append(", Winners: ")
                      .append(
                          segment.getWinners().stream()
                              .map(ShowPlanningPromptBuilder::sanitize)
                              .collect(Collectors.joining(", ")));
                }
                prompt
                    .append(", Show: ")
                    .append(sanitize(segment.getShowName()))
                    .append(", Date: ")
                    .append(segment.getShowDate())
                    .append("\n");
              });
      prompt.append(
          """
          **Anti-Repetition Rules (based on Recent Segments above):**
          - For any rivalry or wrestler pairing that already appeared in Recent Segments, vary the\
           segment format this show. If they competed in a Match last time, book a Promo or\
           Backstage confrontation this time.
          - Do NOT use the same match stipulation for the same rivalry two shows in a row.
          - Every show must advance storylines: each segment for a recurring rivalry should\
           escalate tension (new stakes, interference, title implications) or shift momentum\
           (different winner, surprise turn). Repeating the same result or format is not\
           acceptable.
          - Wrestlers who won their last segment should be booked in stronger roles; wrestlers who\
           lost should be rebuilding or seeking revenge.
          """);
    }

    // Gender lookup for mixed-gender rivalry detection (names sanitized like the roster output)
    Map<String, String> genderByName = new HashMap<>();
    if (context.getFullRoster() != null) {
      context.getFullRoster().forEach(w -> genderByName.put(sanitize(w.getName()), w.getGender()));
    }

    if (context.getCurrentRivalries() != null && !context.getCurrentRivalries().isEmpty()) {
      prompt.append("Current Rivalries (heat ≥ 10 only):\n");
      context
          .getCurrentRivalries()
          .forEach(
              rivalry -> {
                String classification;
                if (!context.isIntergenderAllowed() && isMixedGender(rivalry, genderByName)) {
                  // A match would violate the intergender restriction — advance the feud with
                  // a non-match segment instead of leaving the model with contradictory rules.
                  classification = "CONFRONTATION_ONLY";
                } else if (rivalry.getHeat() >= 30) {
                  // On a regular show, max-heat feuds are saved for the PLE — no stipulation
                  // matches on weeklies. On a PLE they escalate to STIPULATION_REQUIRED.
                  classification =
                      context.isPremiumLiveEvent()
                          ? "STIPULATION_REQUIRED"
                          : "PLE_RESOLUTION_REQUIRED";
                } else if (rivalry.getHeat() >= 20) {
                  classification = "PLE_RESOLUTION_ELIGIBLE";
                } else {
                  classification = "MUST_BOOK";
                }
                prompt
                    .append("- Id: ")
                    .append(rivalry.getId())
                    .append(", Name: ")
                    .append(sanitize(rivalry.getName()))
                    .append(", Participants: ")
                    .append(
                        rivalry.getParticipants().stream()
                            .map(ShowPlanningPromptBuilder::sanitize)
                            .collect(Collectors.joining(", ")))
                    .append(", Heat: ")
                    .append(rivalry.getHeat())
                    .append(", Priority: ")
                    .append(rivalry.getPriority())
                    .append(", Classification: ")
                    .append(classification)
                    .append("\n");
              });
      prompt.append("\n**Rivalry Classification Rules:**\n");
      prompt.append(
          """
          - MUST_BOOK (Heat 10-19): This rivalry MUST have a segment on this show. Book a match \
          or a promo confrontation — do not skip it.
          - PLE_RESOLUTION_ELIGIBLE (Heat 20-29): This rivalry is hot enough to headline a PLE. \
          On a regular show, build tension with a confrontation, brawl, or non-finish match. \
          On a PLE, give it a decisive match.
          - PLE_RESOLUTION_REQUIRED (Heat ≥ 30, regular show): This rivalry has reached maximum \
          intensity and MUST be saved for the next PLE. On this regular show, book a confrontation, \
          brawl, or non-finish segment to build anticipation — do NOT book a decisive match or \
          stipulation here. Reserve the stipulation for the upcoming PLE.
          - STIPULATION_REQUIRED (Heat ≥ 30, PLE only): This rivalry has reached maximum intensity \
          and MUST have a match with a stipulation from the Available Stipulation Matches list below. \
          Use a decisive, no-DQ finish — do not end with a count-out or disqualification.
          - CONFRONTATION_ONLY: This rivalry pairs wrestlers of different genders while intergender \
          matches are disabled. It MUST still appear on the card, but as a Promo, backstage \
          confrontation, or brawl — NEVER as a match. This satisfies its booking requirement, \
          including on PLEs, and overrides every match/stipulation requirement above.
          """);
      List<SegmentRule> highHeatRules = segmentRuleService.getHighHeatRules();
      List<String> highHeatRuleDescriptions =
          highHeatRules.stream()
              .map(
                  rule ->
                      "%s (%s)"
                          .formatted(sanitize(rule.getName()), sanitize(rule.getDescription())))
              .collect(Collectors.toList());
      prompt
          .append("Available Stipulation Matches: ")
          .append(String.join(", ", highHeatRuleDescriptions))
          .append("\n");
      prompt.append(
          "For STIPULATION_REQUIRED rivalries the segment's `rules` array MUST contain at"
              + " least one name from Available Stipulation Matches above.\n\n");
    }

    List<SegmentRule> standardRules = segmentRuleService.getStandardRules();
    if (!standardRules.isEmpty()) {
      List<String> standardRuleDescriptions =
          standardRules.stream()
              .map(
                  rule ->
                      "%s (%s)"
                          .formatted(sanitize(rule.getName()), sanitize(rule.getDescription())))
              .collect(Collectors.toList());
      prompt
          .append("Available Standard Rules: ")
          .append(String.join(", ", standardRuleDescriptions))
          .append("\n");
    }

    if (context.getRecentDramaEvents() != null && !context.getRecentDramaEvents().isEmpty()) {
      prompt.append("\nRecent Dramatic Events (last 30 days):\n");
      prompt.append(
          "Consider these story beats when booking the card — escalate, resolve, or reference"
              + " them:\n");
      context
          .getRecentDramaEvents()
          .forEach(line -> prompt.append("- ").append(sanitize(line)).append("\n"));
      prompt.append("\n");
    }

    if (context.getChampionships() != null && !context.getChampionships().isEmpty()) {
      prompt.append("Championships:\n");
      context
          .getChampionships()
          .forEach(
              championship ->
                  prompt
                      .append("- Name: ")
                      .append(sanitize(championship.getChampionshipName()))
                      .append(", Champion: ")
                      .append(sanitize(championship.getChampionName()))
                      .append(", Contender: ")
                      .append(sanitize(championship.getContenderName()))
                      .append(", Defense Frequency: ")
                      .append(
                          championship.getDefenseFrequencyType() != null
                              ? championship.getDefenseFrequencyType().getDisplayName()
                              : "None")
                      .append(", Days since last defense: ")
                      .append(championship.getDaysSinceLastDefense())
                      .append(championship.isOverdue() ? " (OVERDUE)" : "")
                      .append("\n"));
    }

    if (context.getFullRoster() != null && !context.getFullRoster().isEmpty()) {
      // Wrestlers already locked into pre-determined slots (scripted beats, real-participant
      // tournament rows) are removed from the rendered roster — the deterministic passes book
      // them, so listing them would only invite the AI to double-book or waste tokens.
      Set<String> preBookedNames = preBookedWrestlerNames(context);
      prompt.append("\nFull Roster:\n");
      context.getFullRoster().stream()
          .filter(w -> !preBookedNames.contains(sanitize(w.getName())))
          .forEach(
              wrestler -> {
                prompt
                    .append("- Id: ")
                    .append(wrestler.getId())
                    .append(", Name: ")
                    .append(sanitize(wrestler.getName()))
                    .append(", Gender: ")
                    .append(wrestler.getGender())
                    .append(", Tier: ")
                    .append(wrestler.getTier());
                if (wrestler.getAlignment() != null) {
                  prompt.append(", Alignment: ").append(wrestler.getAlignment());
                }
                prompt.append(", Injured: ").append(wrestler.isInjured()).append("\n");
              });
      prompt.append(
          "IMPORTANT: You MUST only book wrestlers listed in the Full Roster above."
              + " Do not use names from other sections (e.g. recent segments or rivalries)"
              + " that are not present in this roster.\n");
    }

    if (context.getFactions() != null && !context.getFactions().isEmpty()) {
      prompt.append("\nFactions:\n");
      context
          .getFactions()
          .forEach(
              faction -> {
                prompt.append("- Name: ").append(sanitize(faction.getName()));
                if (faction.getLeader() != null) {
                  prompt.append(", Leader: ").append(sanitize(faction.getLeader()));
                }
                if (faction.getMembers() != null && !faction.getMembers().isEmpty()) {
                  prompt
                      .append(", Members: ")
                      .append(
                          faction.getMembers().stream()
                              .map(ShowPlanningPromptBuilder::sanitize)
                              .collect(Collectors.joining(", ")));
                }
                prompt.append("\n");
              });
    }

    if (context.getNextPle() != null) {
      prompt.append("Next PLE (Premium Live Event):\n");
      prompt.append("- Name: ").append(sanitize(context.getNextPle().getPleName())).append("\n");
      prompt.append("- Date: ").append(context.getNextPle().getPleDate()).append("\n");
      prompt.append("- Summary: ").append(sanitize(context.getNextPle().getSummary())).append("\n");
      if (context.getNextPle().getMatches() != null
          && !context.getNextPle().getMatches().isEmpty()) {
        prompt.append("  Scheduled Matches:\n");
        context
            .getNextPle()
            .getMatches()
            .forEach(
                match ->
                    prompt
                        .append("  - Name: ")
                        .append(sanitize(match.getName()))
                        .append(", Participants: ")
                        .append(
                            match.getParticipants().stream()
                                .map(ShowPlanningPromptBuilder::sanitize)
                                .collect(Collectors.joining(", ")))
                        .append("\n"));
      }
    }

    // Scripted-beat and tournament slots are NOT listed in the prompt: the deterministic passes
    // (applyScriptedBeats/applyTournamentSlots) inject those rows onto the card and claim their
    // wrestlers from the roster above, so spelling them out would only burn tokens. The count
    // line below already subtracts the claimed slots.

    prompt.append("\n**Booking Rules & Participation Goal:**\n");
    prompt.append(
        """
        - Goal: Every healthy (non-injured) wrestler MUST participate in at least one segment\
         per week.
        """);
    prompt.append(
        """
        - Prioritize active feuds (especially high priority ones) and title defenses if the days\
         since last defense exceeds the frequency.
        """);
    prompt.append(
        """
        - To ensure 100% participation, use multi-man matches (Triple Threat, Fatal Four-Way,\
         Battle Royale) or Faction-based tag matches to consolidate many wrestlers into few\
         segments.
        """);
    prompt.append(
        """
        - If healthy wrestlers remain after booking matches, assign them to Promos or Backstage\
         segments.
        """);
    prompt.append(
        """
        - Within the same calendar day, avoid having a wrestler in more than one match. They can\
         participate in promos and any other capacity as long as it doesn't involve\
         officially participating in the match.
        """);
    prompt.append(
        """
        - Within the same calendar week, avoid having a wrestler in more than one match. The\
         exception is for Premium Live Event (PLE) where this is not avoidable.
        """);
    if (context.isIntergenderAllowed()) {
      prompt.append(
          """
          - Intergender matches are ENABLED: matches may freely mix male and female wrestlers.
          """);
    } else {
      prompt.append(
          """
          - Intergender matches are DISABLED: every match MUST have all participants of the same\
           gender (see Gender in the Full Roster). Never book a male wrestler against or teaming\
           with a female wrestler in a match. Promos, backstage segments, and other non-match\
           segments MAY mix genders.
          """);
    }
    if (context.isPremiumLiveEvent()) {
      prompt.append(
          """

          **PLE-Specific Booking Rules:**
          - ALL rivalries at Heat ≥ 10 MUST have a match on this card — no deferral to a future show.\
           Exception: CONFRONTATION_ONLY rivalries are advanced with a non-match segment instead.
          - ALL rivalries at Heat ≥ 30 MUST use a stipulation match from the Available Stipulation\
           Matches list above (CONFRONTATION_ONLY rivalries excepted).
          - Every active championship MUST be defended on this card.
          - Matches should have clear, decisive finishes — PLE is not the place for count-out or\
           disqualification endings.
          """);
    }

    // Event-only types (e.g. Abu Dhabi Rumble) are special PLE formats the AI must never
    // propose as an ordinary segment; a Booker/Admin can still pick them manually (ATW-0331).
    List<SegmentType> segmentTypes =
        segmentTypeService.findAll().stream().filter(type -> !type.isEventOnly()).toList();
    List<String> segmentTypeDescriptions =
        segmentTypes.stream()
            .map(
                type ->
                    "%s (%s)".formatted(sanitize(type.getName()), sanitize(type.getDescription())))
            .collect(Collectors.toList());
    prompt
        .append("\nAvailable Segment Types: ")
        .append(String.join(", ", segmentTypeDescriptions))
        .append("\n");

    prompt.append(
        """

        IMPORTANT: Use the provided context to generate a compelling and coherent show. \
        The segments should build on existing rivalries and championships. \
        If a `Next PLE` is provided, the show should build towards it.

        """);
    if (!context.isIntergenderAllowed()) {
      prompt.append(
          """
          REMINDER — intergender matches are DISABLED: before finalizing, verify every match\
           segment's teams contain wrestlers of ONE gender only (check the Gender column in the\
           Full Roster). Rewrite any match that mixes genders. Promos and backstage segments are\
           exempt.

          """);
    }

    prompt.append("\nHere is the JSON schema for a single segment:\n");
    prompt.append("```json\n");
    prompt.append("{\n");
    prompt.append("  \"segmentId\": \"string\",\n");
    if (!segmentTypes.isEmpty()) {
      prompt
          .append("  \"type\": \"string\", // e.g., \"")
          .append(sanitize(segmentTypes.getFirst().getName()))
          .append("\"\n");
    } else {
      prompt.append("  \"type\": \"string\", // e.g., \"Match\"\n");
    }
    prompt.append("  \"description\": \"string\",\n");
    prompt.append("  \"outcome\": \"string\",\n");
    prompt.append(
        "  \"notes\": \"string\", // Optional instructions/feedback for future AI narration\n");
    prompt.append(
        "  \"teams\": [[\"string\"]], // List of teams by name; each inner array is one team."
            + " For a 1v1 match: [[\"WrestlerA\"],[\"WrestlerB\"]]."
            + " For a tag match: [[\"A\",\"B\"],[\"C\",\"D\"]]."
            + " For promos/non-match segments with no opposing sides: [[\"A\",\"B\",\"C\"]].\n");
    prompt.append(
        "  \"teamIds\": [[number]], // REQUIRED: same structure as teams but using the wrestler Id"
            + " from the Full Roster. Must match the teams array exactly."
            + " For a 1v1 match: [[101],[202]]. For a tag match: [[101,102],[203,204]].\n");
    prompt.append(
        "  \"rivalryId\": number, // Optional: the Id of the rivalry this match resolves; omit or"
            + " null if not rivalry-driven\n");
    prompt.append(
        "  \"rules\": [\"string\"] // Optional list of rule names. For STIPULATION_REQUIRED"
            + " rivalries use a name from Available Stipulation Matches (required). For other match"
            + " segments use names from Available Standard Rules where they add drama."
            + " Promos and backstage segments should omit this or use an empty array.\n");
    prompt.append("}\n");
    prompt.append("```\n\n");
    int[] preClaimed = preClaimedSlotCount(context);
    int expectedMatches = context.getShowTemplate().getExpectedMatches();
    int expectedPromos = context.getShowTemplate().getExpectedPromos();
    prompt.append("Generate a JSON array of exactly ");
    if (preClaimed[0] > 0) {
      prompt
          .append(Math.max(0, expectedMatches - preClaimed[0]))
          .append(" matches (")
          .append(expectedMatches)
          .append(" total; ")
          .append(preClaimed[0])
          .append(" pre-determined match slot(s) are booked automatically)");
    } else {
      prompt.append(expectedMatches);
    }
    prompt.append(" matches");
    if (preClaimed[1] > 0) {
      prompt
          .append(" and exactly ")
          .append(Math.max(0, expectedPromos - preClaimed[1]))
          .append(" promos (")
          .append(expectedPromos)
          .append(" total; ")
          .append(preClaimed[1])
          .append(" pre-determined promo slot(s) are booked automatically)");
    } else {
      prompt.append(" and ").append(expectedPromos).append(" promos");
    }
    prompt
        .append(" for the show. Each segment")
        .append(
            " should adhere to the provided schema. Ensure the segments flow logically and build")
        .append(
            """
             towards a compelling narrative. **Be concise with descriptions and outcomes to\
             ensure the entire JSON array fits in the response.**

            """)
        .append(
            """
            IMPORTANT: **Be extremely concise with your internal thoughts/reasoning to save\
             output tokens for the JSON.** The 'participants' field MUST be\
            """)
        .append(
            " populated with relevant wrestler names from the provided context. The response MUST")
        .append(
            " be a valid JSON array, and ONLY the JSON array. Do not include any conversational")
        .append(" text or explanations outside the JSON.\n\n");
    prompt.append("JSON:\n");
    return prompt.toString();
  }

  /**
   * How many match/promo slots on this card are pre-determined: one per scripted beat (a promo-type
   * beat claims a promo slot, anything else a match slot), plus one per real-participant tournament
   * row. Placeholder tournament rows don't claim AI slots — the bracket booking is additive, and
   * the claimed-wrestler warning above keeps the AI off those wrestlers. Mirrors the accounting in
   * {@code ShowPlanningAiService} so the prompt's counts agree with the card.
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
                  .filter(s -> !realParticipantNames(s).isEmpty())
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
   * Sanitized names of every wrestler already locked into a pre-determined slot on this card:
   * scripted-beat participants (feud pair plus any externals/custom team layout) and
   * real-participant tournament rows. Placeholder tournament rows claim nobody — participants
   * resolve from the bracket at approval. Rendered roster entries matching these names are dropped
   * from the prompt so the AI cannot double-book them.
   */
  private Set<String> preBookedWrestlerNames(ShowPlanningContextDTO context) {
    Set<String> names = new HashSet<>();
    if (context.getUpcomingScriptedBeats() != null) {
      for (FeudScriptBeatDTO beat : context.getUpcomingScriptedBeats()) {
        beat.getTeamNameLists().stream()
            .flatMap(List::stream)
            .filter(name -> name != null && !name.isBlank())
            .map(ShowPlanningPromptBuilder::sanitize)
            .forEach(names::add);
      }
    }
    if (context.getTournamentSlots() != null) {
      context.getTournamentSlots().stream()
          .map(this::realParticipantNames)
          .flatMap(List::stream)
          .map(ShowPlanningPromptBuilder::sanitize)
          .forEach(names::add);
    }
    return names;
  }

  /** Real participant names across a slot's teams; empty for placeholders. */
  private List<String> realParticipantNames(TournamentSlotPreviewDTO slot) {
    if (slot.getTeams() == null) {
      return List.of();
    }
    return slot.getTeams().stream()
        .flatMap(List::stream)
        .filter(n -> n != null && !n.isBlank() && !"Tournament bracket".equals(n))
        .toList();
  }
}
