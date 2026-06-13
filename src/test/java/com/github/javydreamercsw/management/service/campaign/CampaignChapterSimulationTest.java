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
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignPhase;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.ChapterCriteriaDTO;
import com.github.javydreamercsw.management.dto.campaign.ChapterPointDTO;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Structural simulation of campaign_chapters.json. Catches authoring errors that would trap players
 * before anyone reaches that point in the game.
 *
 * <p>Seven checks:
 *
 * <ol>
 *   <li>FAIL — every exit point must be reachable under some achievable state.
 *   <li>FAIL — static encounter IDs must be unique within each chapter.
 *   <li>FAIL — routing targets (nextEncounterId / onWinNextEncounterId / onLossNextEncounterId)
 *       must reference encounter IDs that exist in the same chapter.
 *   <li>FAIL — STATIC_ONLY / AI_WITH_FALLBACK chapters must have enough MATCH steps to satisfy exit
 *       criteria that require minMatchesPlayed.
 *   <li>FAIL — expansion codes in requiredExpansions / requiredExpansion fields must match a known
 *       code in expansions.json.
 *   <li>WARN — every exit state should have at least one static successor chapter; if not, logs a
 *       warning (expansion boundary or AI handoff, not a bug).
 * </ol>
 */
class CampaignChapterSimulationTest {

  private static final Logger log = LoggerFactory.getLogger(CampaignChapterSimulationTest.class);

  private CampaignChapterService chapterService;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    FeatureDataService featureDataService =
        new FeatureDataService(objectMapper, mock(CampaignStateRepository.class));
    com.github.javydreamercsw.management.service.expansion.ExpansionService expansionService =
        mock(com.github.javydreamercsw.management.service.expansion.ExpansionService.class);
    org.mockito.Mockito.when(
            expansionService.isExpansionEnabled(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(true);
    chapterService = new CampaignChapterService(objectMapper, featureDataService, expansionService);
    chapterService.init();
  }

  // ---------------------------------------------------------------------------
  // Check 1: every exit point is reachable
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Every exit point is reachable under some achievable game state")
  void allExitPointsAreReachable() {
    List<String> failures = new ArrayList<>();

    for (CampaignChapterDTO chapter : chapterService.getAllChapters()) {
      if (chapter.getExitPoints() == null || chapter.getExitPoints().isEmpty()) {
        // No exit points defined — chapter has no completion condition (unusual but not invalid).
        continue;
      }

      int vpWin = vpWin(chapter);
      int vpLoss = vpLoss(chapter);
      int maxMatchesNeeded = maxMinMatchesPlayed(chapter) + 1;

      for (ChapterPointDTO point : chapter.getExitPoints()) {
        boolean reachable = false;

        outer:
        for (int matches = 0; matches < maxMatchesNeeded + 1; matches++) {
          for (int wins = 0; wins < matches + 1; wins++) {
            int losses = matches - wins;
            int vp = wins * vpWin + losses * vpLoss;

            // Try all combinations of the three boolean feature flags
            for (boolean tw : new boolean[] {false, true}) {
              for (boolean fq : new boolean[] {false, true}) {
                for (boolean wf : new boolean[] {false, true}) {
                  CampaignState state =
                      buildState(chapter, matches, wins, vp, tw, fq, wf, point.getCriteria());
                  if (areAllCriteriaMet(point.getCriteria(), state)) {
                    reachable = true;
                    break outer;
                  }
                }
              }
            }
          }
        }

        if (!reachable) {
          failures.add(
              String.format(
                  "[%s] Exit point \"%s\": unreachable — no simulated state satisfies its"
                      + " criteria (vpWin=%d, vpLoss=%d, maxMatches=%d). Criteria: %s",
                  chapter.getId(),
                  point.getName(),
                  vpWin,
                  vpLoss,
                  maxMatchesNeeded,
                  describeCriteria(point.getCriteria())));
        }
      }
    }

    assertThat(failures)
        .as("CHAPTER VALIDATION FAILURES:\n" + String.join("\n", failures))
        .isEmpty();
  }

  // ---------------------------------------------------------------------------
  // Check 2: static encounter IDs are unique within each chapter
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Static encounter IDs are unique within each chapter")
  void staticEncounterIdsAreUnique() {
    List<String> failures = new ArrayList<>();

    for (CampaignChapterDTO chapter : chapterService.getAllChapters()) {
      if (!chapter.hasStaticEncounters()) {
        continue;
      }
      java.util.Set<String> seen = new java.util.HashSet<>();
      for (var encounter : chapter.getStaticEncounters()) {
        if (encounter.getId() == null) {
          failures.add("[" + chapter.getId() + "] Encounter has null id: " + encounter.getTitle());
          continue;
        }
        if (!seen.add(encounter.getId())) {
          failures.add("[" + chapter.getId() + "] Duplicate encounter id: " + encounter.getId());
        }
      }
    }

    assertThat(failures)
        .as("DUPLICATE ENCOUNTER ID FAILURES:\n" + String.join("\n", failures))
        .isEmpty();
  }

  // ---------------------------------------------------------------------------
  // Check 3: routing targets exist within the same chapter
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Routing targets in static choices point to valid encounter IDs within the chapter")
  void staticChoiceRoutingTargetsExist() {
    List<String> failures = new ArrayList<>();

    for (CampaignChapterDTO chapter : chapterService.getAllChapters()) {
      if (!chapter.hasStaticEncounters()) {
        continue;
      }
      java.util.Set<String> ids =
          chapter.getStaticEncounters().stream()
              .map(com.github.javydreamercsw.management.dto.campaign.StaticEncounterDTO::getId)
              .collect(java.util.stream.Collectors.toSet());

      for (var encounter : chapter.getStaticEncounters()) {
        if (encounter.getChoices() == null) {
          continue;
        }
        for (var choice : encounter.getChoices()) {
          checkRoutingTarget(
              failures,
              chapter.getId(),
              encounter.getId(),
              "nextEncounterId",
              choice.getNextEncounterId(),
              ids);
          checkRoutingTarget(
              failures,
              chapter.getId(),
              encounter.getId(),
              "onWinNextEncounterId",
              choice.getOnWinNextEncounterId(),
              ids);
          checkRoutingTarget(
              failures,
              chapter.getId(),
              encounter.getId(),
              "onLossNextEncounterId",
              choice.getOnLossNextEncounterId(),
              ids);
        }
      }
    }

    assertThat(failures).as("ROUTING TARGET FAILURES:\n" + String.join("\n", failures)).isEmpty();
  }

  private void checkRoutingTarget(
      List<String> failures,
      String chapterId,
      String encounterId,
      String field,
      String target,
      java.util.Set<String> validIds) {
    if (target != null && !validIds.contains(target)) {
      failures.add(
          String.format(
              "[%s] Encounter '%s': %s='%s' does not exist in chapter",
              chapterId, encounterId, field, target));
    }
  }

  // ---------------------------------------------------------------------------
  // Check 5: static chapters have enough MATCH steps
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Static chapters have enough MATCH encounters to satisfy exit criteria")
  void staticChaptersHaveSufficientMatchSteps() {
    List<String> failures = new ArrayList<>();

    for (CampaignChapterDTO chapter : chapterService.getAllChapters()) {
      if (!chapter.hasStaticEncounters()) {
        continue;
      }

      int requiredMatches = maxMinMatchesPlayed(chapter);
      if (requiredMatches == 0) {
        continue;
      }

      long matchSteps =
          chapter.getStaticEncounters().stream()
              .flatMap(
                  e ->
                      e.getChoices() == null
                          ? java.util.stream.Stream.empty()
                          : e.getChoices().stream())
              .filter(c -> c.getNextPhase() != null && c.getNextPhase() == CampaignPhase.MATCH)
              .count();

      if (matchSteps < requiredMatches) {
        failures.add(
            String.format(
                "[%s] Static chapter needs %d MATCH steps to satisfy exit criteria, but only %d"
                    + " MATCH choice(s) found in staticEncounters.",
                chapter.getId(), requiredMatches, matchSteps));
      }
    }

    assertThat(failures)
        .as("STATIC CHAPTER MATCH STEP FAILURES:\n" + String.join("\n", failures))
        .isEmpty();
  }

  // ---------------------------------------------------------------------------
  // ---------------------------------------------------------------------------
  // Check 6: requiredExpansions and requiredExpansion codes are known
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Expansion codes in chapters and choices reference known expansions")
  void expansionCodesAreKnown() {
    // Load known codes from expansions.json
    java.util.Set<String> known;
    try {
      com.fasterxml.jackson.databind.ObjectMapper om =
          new com.fasterxml.jackson.databind.ObjectMapper();
      java.io.InputStream is = getClass().getResourceAsStream("/expansions.json");
      java.util.List<java.util.Map<String, String>> raw =
          om.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<>() {});
      known =
          raw.stream()
              .map(m -> m.get("expansion_code"))
              .collect(java.util.stream.Collectors.toSet());
    } catch (Exception e) {
      throw new RuntimeException("Could not load expansions.json", e);
    }

    List<String> failures = new ArrayList<>();

    for (CampaignChapterDTO chapter : chapterService.getAllChapters()) {
      if (chapter.getRequiredExpansions() != null) {
        for (String code : chapter.getRequiredExpansions()) {
          if (!known.contains(code)) {
            failures.add(
                "[" + chapter.getId() + "] Unknown requiredExpansion code on chapter: " + code);
          }
        }
      }
      if (!chapter.hasStaticEncounters()) {
        continue;
      }
      for (var encounter : chapter.getStaticEncounters()) {
        if (encounter.getRequiredExpansion() != null
            && !known.contains(encounter.getRequiredExpansion())) {
          failures.add(
              "["
                  + chapter.getId()
                  + "] Encounter '"
                  + encounter.getId()
                  + "': unknown requiredExpansion: "
                  + encounter.getRequiredExpansion());
        }
        if (encounter.getChoices() == null) {
          continue;
        }
        for (var choice : encounter.getChoices()) {
          if (choice.getRequiredExpansion() != null
              && !known.contains(choice.getRequiredExpansion())) {
            failures.add(
                "["
                    + chapter.getId()
                    + "] Encounter '"
                    + encounter.getId()
                    + "' choice '"
                    + choice.getId()
                    + "': unknown requiredExpansion: "
                    + choice.getRequiredExpansion());
          }
        }
      }
    }

    assertThat(failures)
        .as("UNKNOWN EXPANSION CODE FAILURES:\n" + String.join("\n", failures))
        .isEmpty();
  }

  // Check 7: successor availability (WARN only — never fails the build)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Chapters without a static successor emit a warning (not a failure)")
  void chaptersWithoutSuccessorEmitWarning() {
    for (CampaignChapterDTO chapter : chapterService.getAllChapters()) {
      if (chapter.isExpansionBoundary()) {
        continue;
      }
      if (chapter.getExitPoints() == null || chapter.getExitPoints().isEmpty()) {
        continue;
      }

      int vpWin = vpWin(chapter);
      int vpLoss = vpLoss(chapter);

      for (ChapterPointDTO point : chapter.getExitPoints()) {
        // Build a state that satisfies this exit point
        CampaignState exitState = buildSatisfyingState(chapter, point, vpWin, vpLoss);
        if (exitState == null) {
          continue; // Exit point itself is unreachable — caught by check 1

          // Mark this chapter as completed and look for successors
        }
        exitState.getCompletedChapterIds().add(chapter.getId());
        List<CampaignChapterDTO> successors = chapterService.findAvailableChapters(exitState);

        if (successors.isEmpty()) {
          log.warn(
              "[WARN] No static successor for exit '{}' of chapter '{}' — AI handoff or expansion"
                  + " boundary. Add \"expansionBoundary\": true to silence.",
              point.getName(),
              chapter.getId());
        }
      }
    }
    // This test always passes — it only produces log warnings
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private int vpWin(CampaignChapterDTO chapter) {
    int base =
        Optional.ofNullable(chapter.getRules())
            .map(CampaignChapterDTO.ChapterRules::getVictoryPointsWin)
            .orElse(2);
    // titleDefenseVP and titleWinVP are awarded per win on top of the base VP.
    // Use the best possible per-win VP to avoid false "unreachable" failures for champion chapters.
    int titleBonus =
        Optional.ofNullable(chapter.getRules())
            .map(
                r ->
                    Math.max(
                        r.getTitleDefenseVP() == 0 ? 0 : r.getTitleDefenseVP(),
                        r.getTitleWinVP() == 0 ? 0 : r.getTitleWinVP()))
            .orElse(0);
    return base + titleBonus;
  }

  private int vpLoss(CampaignChapterDTO chapter) {
    return Optional.ofNullable(chapter.getRules())
        .map(CampaignChapterDTO.ChapterRules::getVictoryPointsLoss)
        .orElse(-1);
  }

  private int maxMinMatchesPlayed(CampaignChapterDTO chapter) {
    if (chapter.getExitPoints() == null) {
      return 0;
    }
    return chapter.getExitPoints().stream()
        .flatMap(
            p ->
                p.getCriteria() == null
                    ? java.util.stream.Stream.empty()
                    : p.getCriteria().stream())
        .mapToInt(c -> c.getMinMatchesPlayed() == null ? 0 : c.getMinMatchesPlayed())
        .max()
        .orElse(0);
  }

  /**
   * Builds a CampaignState for the given simulation tick. If any criterion in the point requires
   * external-entity checks (isChampion, hasFaction, requiredAlignmentType,
   * requiredCompletedChapterIds), the state is pre-built to satisfy those conditions — they are
   * treated as wildcards because the simulation cannot control match outcomes for them.
   */
  private CampaignState buildState(
      CampaignChapterDTO chapter,
      int matches,
      int wins,
      int vp,
      boolean tournamentWinner,
      boolean failedToQualify,
      boolean wonFinale,
      List<ChapterCriteriaDTO> criteria) {

    CampaignState state = new CampaignState();
    state.setMatchesPlayed(matches);
    state.setWins(wins);
    state.setVictoryPoints(vp);
    state.setCurrentChapterId(chapter.getId());

    // Encode boolean feature flags into featureData JSON
    try {
      java.util.Map<String, Object> flags = new java.util.HashMap<>();
      flags.put("tournamentWinner", tournamentWinner);
      flags.put("failedToQualify", failedToQualify);
      flags.put("wonFinale", wonFinale);
      state.setFeatureData(new ObjectMapper().writeValueAsString(flags));
    } catch (Exception e) {
      // ignored — state has no featureData
    }

    // Build wrestler + campaign scaffolding
    Wrestler wrestler = new Wrestler();
    wrestler.setReigns(new LinkedHashSet<>());

    Campaign campaign = new Campaign();
    campaign.setWrestler(wrestler);
    state.setCampaign(campaign);

    // Satisfy external-entity criteria as wildcards
    if (criteria != null) {
      for (ChapterCriteriaDTO c : criteria) {
        // isChampion: add a current reign to the wrestler
        if (Boolean.TRUE.equals(c.getIsChampion())) {
          TitleReign reign = new TitleReign();
          reign.setStartDate(Instant.now());
          reign.setEndDate(null); // null endDate = current reign
          wrestler.getReigns().add(reign);
        }

        // hasFaction: add a WrestlerState with a non-null faction
        if (Boolean.TRUE.equals(c.getHasFaction())) {
          com.github.javydreamercsw.management.domain.faction.Faction faction =
              new com.github.javydreamercsw.management.domain.faction.Faction();
          faction.setName("Sim Faction");
          WrestlerState ws = new WrestlerState();
          ws.setFaction(faction);
          wrestler.getWrestlerStates().add(ws);
        }

        // requiredAlignmentType: attach a matching alignment
        if (c.getRequiredAlignmentType() != null) {
          WrestlerAlignment alignment = new WrestlerAlignment();
          alignment.setAlignmentType(AlignmentType.valueOf(c.getRequiredAlignmentType()));
          alignment.setLevel(c.getMinAlignmentLevel() == null ? 0 : c.getMinAlignmentLevel());
          wrestler.getAlignments().add(alignment);
        }

        // requiredCompletedChapterIds: mark them as completed
        if (c.getRequiredCompletedChapterIds() != null) {
          state.getCompletedChapterIds().addAll(c.getRequiredCompletedChapterIds());
        }
      }
    }

    return state;
  }

  /** Finds the first state that satisfies the exit point, or null if none exists. */
  private CampaignState buildSatisfyingState(
      CampaignChapterDTO chapter, ChapterPointDTO point, int vpWin, int vpLoss) {
    int maxMatches = maxMinMatchesPlayed(chapter) + 1;
    for (int matches = 0; matches < maxMatches + 1; matches++) {
      for (int wins = 0; wins < matches + 1; wins++) {
        int vp = wins * vpWin + (matches - wins) * vpLoss;
        for (boolean tw : new boolean[] {false, true}) {
          for (boolean fq : new boolean[] {false, true}) {
            for (boolean wf : new boolean[] {false, true}) {
              CampaignState state =
                  buildState(chapter, matches, wins, vp, tw, fq, wf, point.getCriteria());
              if (areAllCriteriaMet(point.getCriteria(), state)) {
                return state;
              }
            }
          }
        }
      }
    }
    return null;
  }

  private boolean areAllCriteriaMet(List<ChapterCriteriaDTO> criteria, CampaignState state) {
    if (criteria == null || criteria.isEmpty()) {
      return true;
    }
    return criteria.stream().allMatch(c -> chapterService.isCriteriaMet(c, state));
  }

  private String describeCriteria(List<ChapterCriteriaDTO> criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return "(none)";
    }
    return criteria.stream()
        .map(
            c -> {
              List<String> parts = new ArrayList<>();
              if (c.getMinVictoryPoints() != null) {
                parts.add("minVP=" + c.getMinVictoryPoints());
              }
              if (c.getMaxVictoryPoints() != null) {
                parts.add("maxVP=" + c.getMaxVictoryPoints());
              }
              if (c.getMinMatchesPlayed() != null) {
                parts.add("minMatches=" + c.getMinMatchesPlayed());
              }
              if (c.getMinWins() != null) {
                parts.add("minWins=" + c.getMinWins());
              }
              if (c.getTournamentWinner() != null) {
                parts.add("tournamentWinner=" + c.getTournamentWinner());
              }
              if (c.getFailedToQualify() != null) {
                parts.add("failedToQualify=" + c.getFailedToQualify());
              }
              if (c.getWonFinale() != null) {
                parts.add("wonFinale=" + c.getWonFinale());
              }
              if (c.getIsChampion() != null) {
                parts.add("isChampion=" + c.getIsChampion());
              }
              if (c.getHasFaction() != null) {
                parts.add("hasFaction=" + c.getHasFaction());
              }
              return "{" + String.join(", ", parts) + "}";
            })
        .collect(Collectors.joining(", "));
  }
}
