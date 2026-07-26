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
 * <p>Twelve checks:
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
 *   <li>FAIL — every ungated static encounter must have at least one choice with no
 *       requiredExpansion, so the player can always proceed without an optional pack.
 *   <li>WARN — every exit state should have at least one static successor chapter; if not, logs a
 *       warning (expansion boundary or AI handoff, not a bug).
 *   <li>PASS — chapter graph written to target/campaign-graph.dot for authors.
 *   <li>FAIL — allowedWrestlerNames entries must reference real wrestlers in wrestlers.json.
 *   <li>FAIL — opponentPool, forcedOpponentName, and excludedOpponents entries (excluding
 *       placeholders like {{RIVAL}}, {{CHAMP}}) must reference real wrestlers in wrestlers.json.
 *   <li>FAIL — initialChampions wrestler values must reference real wrestlers in wrestlers.json.
 *   <li>FAIL — initialChampions title keys must reference real titles in championships.json.
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
    chapterService =
        new CampaignChapterService(
            objectMapper,
            featureDataService,
            expansionService,
            new org.springframework.core.io.support.PathMatchingResourcePatternResolver());
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

  // ---------------------------------------------------------------------------
  // Check 7: encounters without an encounter-level gate must have ≥1 ungated choice
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Every ungated static encounter has at least one choice with no requiredExpansion")
  void ungatedEncountersHaveAtLeastOneBaseChoice() {
    List<String> failures = new ArrayList<>();

    for (CampaignChapterDTO chapter : chapterService.getAllChapters()) {
      if (!chapter.hasStaticEncounters()) {
        continue;
      }
      for (var encounter : chapter.getStaticEncounters()) {
        // Encounters gated by an expansion are only shown when that pack is present,
        // so all their choices may require the same expansion — that's fine.
        if (encounter.getRequiredExpansion() != null) {
          continue;
        }
        if (encounter.getChoices() == null || encounter.getChoices().isEmpty()) {
          continue; // No choices — handled by other checks or intentional terminal card
        }
        boolean hasBaseChoice =
            encounter.getChoices().stream().anyMatch(c -> c.getRequiredExpansion() == null);
        if (!hasBaseChoice) {
          failures.add(
              String.format(
                  "[%s] Encounter '%s': all choices require an expansion — player will be stuck"
                      + " if no expansion is installed. Add a base-game fallback choice or"
                      + " gate the entire encounter with requiredExpansion.",
                  chapter.getId(), encounter.getId()));
        }
      }
    }

    assertThat(failures)
        .as("EXPANSION-GATED-ALL-CHOICES FAILURES:\n" + String.join("\n", failures))
        .isEmpty();
  }

  // Check 8: successor availability (WARN only — never fails the build)
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
  // Graph: write target/campaign-graph.dot (always passes, purely for authors)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Generate campaign-graph.dot in target/ for visualization")
  void generateChapterGraph() throws Exception {
    // Build edge map: chapter → list of (exitName, successor)
    java.util.Map<String, java.util.List<String[]>> edges = new java.util.LinkedHashMap<>();
    java.util.Set<String> aiHandoffNodes = new java.util.HashSet<>();

    for (CampaignChapterDTO chapter : chapterService.getAllChapters()) {
      edges.put(chapter.getId(), new java.util.ArrayList<>());

      if (chapter.getExitPoints() == null || chapter.getExitPoints().isEmpty()) {
        continue;
      }
      int vpWin = vpWin(chapter);
      int vpLoss = vpLoss(chapter);

      for (ChapterPointDTO point : chapter.getExitPoints()) {
        CampaignState exitState = buildSatisfyingState(chapter, point, vpWin, vpLoss);
        if (exitState == null) {
          continue;
        }
        exitState.getCompletedChapterIds().add(chapter.getId());
        List<CampaignChapterDTO> successors = chapterService.findAvailableChapters(exitState);

        if (successors.isEmpty()) {
          // AI handoff or expansion boundary — add a virtual sink node
          String sinkId = chapter.getId() + "_exit_" + point.getName().replaceAll("\\W+", "_");
          edges.get(chapter.getId()).add(new String[] {point.getName(), sinkId, "sink"});
          aiHandoffNodes.add(sinkId);
        } else {
          for (CampaignChapterDTO successor : successors) {
            edges
                .get(chapter.getId())
                .add(new String[] {point.getName(), successor.getId(), "normal"});
          }
        }
      }
    }

    // Build DOT source
    StringBuilder dot = new StringBuilder();
    dot.append("digraph CampaignChapters {\n");
    dot.append("  rankdir=LR;\n");
    dot.append("  node [fontname=\"Helvetica\", fontsize=10];\n");
    dot.append("  edge [fontname=\"Helvetica\", fontsize=9];\n\n");

    // Legend
    dot.append("  subgraph cluster_legend {\n");
    dot.append("    label=\"Legend\"; style=dotted; fontsize=9;\n");
    dot.append("    l1 [label=\"STATIC_ONLY\",  style=filled, fillcolor=lightblue,  shape=box];\n");
    dot.append(
        "    l2 [label=\"AI_WITH_FALLBACK\", style=filled, fillcolor=lightyellow, shape=box];\n");
    dot.append("    l3 [label=\"AI_ONLY\",      style=filled, fillcolor=white,      shape=box];\n");
    dot.append(
        "    l4 [label=\"AI handoff / expansion boundary\", style=dashed, shape=ellipse];\n");
    dot.append("  }\n\n");

    // Chapter nodes
    for (CampaignChapterDTO chapter : chapterService.getAllChapters()) {
      String fillColor =
          switch (chapter.getMode()) {
            case STATIC_ONLY -> "lightblue";
            case AI_WITH_FALLBACK -> "lightyellow";
            default -> "white";
          };
      String border =
          chapter.isExpansionBoundary() ? ", style=\"filled,dashed\"" : ", style=filled";
      String expansionLabel =
          chapter.getRequiredExpansions() != null && !chapter.getRequiredExpansions().isEmpty()
              ? "\\n[requires: " + String.join(", ", chapter.getRequiredExpansions()) + "]"
              : "";
      String diff =
          chapter.getDifficulty() != null ? "\\n(" + chapter.getDifficulty().name() + ")" : "";
      String modeLabel = chapter.getMode().name();
      dot.append(
          String.format(
              "  \"%s\" [label=\"%s\\n%s\\n%s%s%s\", shape=box, fillcolor=%s%s];\n",
              chapter.getId(),
              chapter.getId(),
              chapter.getTitle(),
              modeLabel,
              diff,
              expansionLabel,
              fillColor,
              border));
    }

    // AI handoff sink nodes
    for (String sink : aiHandoffNodes) {
      dot.append(
          String.format(
              "  \"%s\" [label=\"AI handoff\", shape=ellipse, style=dashed,"
                  + " fillcolor=lightgrey];\n",
              sink));
    }

    dot.append("\n");

    // Edges
    for (CampaignChapterDTO chapter : chapterService.getAllChapters()) {
      for (String[] edge : edges.getOrDefault(chapter.getId(), java.util.Collections.emptyList())) {
        String exitName = edge[0];
        String targetId = edge[1];
        dot.append(
            String.format(
                "  \"%s\" -> \"%s\" [label=\"%s\"];\n", chapter.getId(), targetId, exitName));
      }
    }

    dot.append("}\n");

    // Write to target/
    java.nio.file.Path outDir = java.nio.file.Paths.get("target");
    java.nio.file.Files.createDirectories(outDir);
    java.nio.file.Path outFile = outDir.resolve("campaign-graph.dot");
    java.nio.file.Files.writeString(outFile, dot.toString());

    log.info("Campaign chapter graph written to: {}", outFile.toAbsolutePath());
    log.info("Render with: dot -Tsvg {} -o target/campaign-graph.svg", outFile.getFileName());
    // This test always passes — the file is an author aid, not a correctness check
  }

  // ---------------------------------------------------------------------------
  // Check 9: allowedWrestlerNames references real wrestlers in wrestlers.json
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("allowedWrestlerNames entries reference wrestlers that exist in wrestlers.json")
  void allowedWrestlerNamesExistInWrestlersJson() throws Exception {
    java.util.Set<String> knownWrestlers = loadWrestlerNames();
    List<String> failures = new ArrayList<>();

    for (CampaignChapterDTO chapter : chapterService.getAllChapters()) {
      if (chapter.getAllowedWrestlerNames() == null
          || chapter.getAllowedWrestlerNames().isEmpty()) {
        continue;
      }
      for (String name : chapter.getAllowedWrestlerNames()) {
        if (!knownWrestlers.contains(name)) {
          failures.add(
              String.format(
                  "[%s] allowedWrestlerNames entry '%s' does not match any wrestler in"
                      + " wrestlers.json",
                  chapter.getId(), name));
        }
      }
    }

    assertThat(failures)
        .as("ALLOWED WRESTLER NAME FAILURES:\n" + String.join("\n", failures))
        .isEmpty();
  }

  // ---------------------------------------------------------------------------
  // Check 10: opponentPool and non-placeholder forcedOpponentName reference real wrestlers
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("opponentPool and forcedOpponentName entries reference wrestlers in wrestlers.json")
  void opponentPoolAndForcedOpponentNamesExistInWrestlersJson() throws Exception {
    java.util.Set<String> knownWrestlers = loadWrestlerNames();
    List<String> failures = new ArrayList<>();

    for (CampaignChapterDTO chapter : chapterService.getAllChapters()) {
      if (!chapter.hasStaticEncounters()) {
        continue;
      }
      for (var encounter : chapter.getStaticEncounters()) {
        if (encounter.getChoices() == null) {
          continue;
        }
        for (var choice : encounter.getChoices()) {
          // Validate opponentPool entries (skip placeholder tokens)
          if (choice.getOpponentPool() != null) {
            for (String name : choice.getOpponentPool()) {
              if (!name.contains("{{") && !knownWrestlers.contains(name)) {
                failures.add(
                    String.format(
                        "[%s] Encounter '%s': opponentPool entry '%s' does not match any wrestler"
                            + " in wrestlers.json",
                        chapter.getId(), encounter.getId(), name));
              }
            }
          }
          // Validate forcedOpponentName (skip placeholder tokens like {{RIVAL}}, {{CHAMP}})
          String forced = choice.getForcedOpponentName();
          if (forced != null && !forced.contains("{{") && !knownWrestlers.contains(forced)) {
            failures.add(
                String.format(
                    "[%s] Encounter '%s': forcedOpponentName '%s' does not match any wrestler in"
                        + " wrestlers.json",
                    chapter.getId(), encounter.getId(), forced));
          }
          // Validate excludedOpponents entries (skip placeholder tokens)
          if (choice.getExcludedOpponents() != null) {
            for (String name : choice.getExcludedOpponents()) {
              if (!name.contains("{{") && !knownWrestlers.contains(name)) {
                failures.add(
                    String.format(
                        "[%s] Encounter '%s': excludedOpponents entry '%s' does not match any"
                            + " wrestler in wrestlers.json",
                        chapter.getId(), encounter.getId(), name));
              }
            }
          }
        }
      }
      // Also validate chapter-level defaultExcludedOpponents
      if (chapter.getDefaultExcludedOpponents() != null) {
        for (String name : chapter.getDefaultExcludedOpponents()) {
          if (!name.contains("{{") && !knownWrestlers.contains(name)) {
            failures.add(
                String.format(
                    "[%s] defaultExcludedOpponents entry '%s' does not match any wrestler in"
                        + " wrestlers.json",
                    chapter.getId(), name));
          }
        }
      }
    }

    assertThat(failures).as("OPPONENT NAME FAILURES:\n" + String.join("\n", failures)).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // Check 11: initialChampions wrestler names exist in wrestlers.json
  // Check 12: initialChampions title names exist in championships.json
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("initialChampions wrestler names reference wrestlers that exist in wrestlers.json")
  void initialChampionsWrestlerNamesExistInWrestlersJson() throws Exception {
    java.util.Set<String> knownWrestlers = loadWrestlerNames();
    List<String> failures = new ArrayList<>();

    for (CampaignChapterDTO chapter : chapterService.getAllChapters()) {
      if (chapter.getInitialChampions() == null || chapter.getInitialChampions().isEmpty()) {
        continue;
      }
      for (java.util.Map.Entry<String, String> entry : chapter.getInitialChampions().entrySet()) {
        String wrestlerName = entry.getValue();
        if (!knownWrestlers.contains(wrestlerName)) {
          failures.add(
              String.format(
                  "[%s] initialChampions: wrestler '%s' (for title '%s') does not match any"
                      + " wrestler in wrestlers.json",
                  chapter.getId(), wrestlerName, entry.getKey()));
        }
      }
    }

    assertThat(failures)
        .as("INITIAL CHAMPION WRESTLER NAME FAILURES:\n" + String.join("\n", failures))
        .isEmpty();
  }

  @Test
  @DisplayName("initialChampions title names reference titles that exist in championships.json")
  void initialChampionsTitleNamesExistInChampionshipsJson() throws Exception {
    java.util.Set<String> knownTitles = loadTitleNames();
    List<String> failures = new ArrayList<>();

    for (CampaignChapterDTO chapter : chapterService.getAllChapters()) {
      if (chapter.getInitialChampions() == null || chapter.getInitialChampions().isEmpty()) {
        continue;
      }
      for (String titleName : chapter.getInitialChampions().keySet()) {
        if (!knownTitles.contains(titleName)) {
          failures.add(
              String.format(
                  "[%s] initialChampions: title '%s' does not match any title in"
                      + " championships.json",
                  chapter.getId(), titleName));
        }
      }
    }

    assertThat(failures)
        .as("INITIAL CHAMPION TITLE NAME FAILURES:\n" + String.join("\n", failures))
        .isEmpty();
  }

  /** Loads all wrestler names from wrestlers.json as a Set for O(1) lookup. */
  private java.util.Set<String> loadWrestlerNames() throws Exception {
    com.fasterxml.jackson.databind.ObjectMapper om =
        new com.fasterxml.jackson.databind.ObjectMapper();
    java.io.InputStream is = getClass().getResourceAsStream("/wrestlers.json");
    java.util.List<java.util.Map<String, Object>> raw =
        om.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<>() {});
    return raw.stream()
        .map(m -> (String) m.get("name"))
        .collect(java.util.stream.Collectors.toSet());
  }

  /** Loads all title names from championships.json as a Set for O(1) lookup. */
  private java.util.Set<String> loadTitleNames() throws Exception {
    com.fasterxml.jackson.databind.ObjectMapper om =
        new com.fasterxml.jackson.databind.ObjectMapper();
    java.io.InputStream is = getClass().getResourceAsStream("/championships.json");
    java.util.List<java.util.Map<String, Object>> raw =
        om.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<>() {});
    return raw.stream()
        .map(m -> (String) m.get("name"))
        .collect(java.util.stream.Collectors.toSet());
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
