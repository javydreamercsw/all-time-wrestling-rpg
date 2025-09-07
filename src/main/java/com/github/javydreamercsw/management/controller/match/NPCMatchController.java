package com.github.javydreamercsw.management.controller.match;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.match.Match;
import com.github.javydreamercsw.management.domain.show.match.MatchRepository;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.show.match.type.MatchTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.match.MatchTeam;
import com.github.javydreamercsw.management.service.match.NPCMatchResolutionService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for NPC match resolution system. Provides endpoints for automatically resolving
 * matches between wrestlers using ATW RPG mechanics.
 */
@RestController
@RequestMapping("/api/npc-matches")
@RequiredArgsConstructor
@Slf4j
public class NPCMatchController {
  private final NPCMatchResolutionService npcMatchResolutionService;
  private final WrestlerRepository wrestlerRepository;
  private final MatchTypeRepository matchTypeRepository;
  private final ShowRepository showRepository;
  private final MatchRepository matchRepository;

  /** Resolve a team-based match (supports singles, tag team, handicap, etc.). */
  @PostMapping("/team")
  public ResponseEntity<?> resolveTeamMatch(@RequestBody TeamMatchRequest request) {
    try {
      // Validate team 1 wrestlers
      List<Wrestler> team1Wrestlers =
          wrestlerRepository.findAllById(request.team1WrestlerIds()).stream().toList();
      if (team1Wrestlers.size() != request.team1WrestlerIds().size()) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "One or more Team 1 wrestlers not found"));
      }
      if (team1Wrestlers.isEmpty()) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Team 1 must have at least one wrestler"));
      }

      // Validate team 2 wrestlers
      List<Wrestler> team2Wrestlers =
          wrestlerRepository.findAllById(request.team2WrestlerIds()).stream().toList();
      if (team2Wrestlers.size() != request.team2WrestlerIds().size()) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "One or more Team 2 wrestlers not found"));
      }
      if (team2Wrestlers.isEmpty()) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Team 2 must have at least one wrestler"));
      }

      // Validate match type
      Optional<MatchType> matchTypeOpt = matchTypeRepository.findById(request.matchTypeId());
      if (matchTypeOpt.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Match type not found"));
      }

      // Validate show
      Optional<Show> showOpt = showRepository.findById(request.showId());
      if (showOpt.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Show not found"));
      }

      // Create teams
      MatchTeam team1 = new MatchTeam(team1Wrestlers, request.team1Name());
      MatchTeam team2 = new MatchTeam(team2Wrestlers, request.team2Name());

      // Resolve match
      Match result =
          npcMatchResolutionService.resolveTeamMatch(
              team1, team2, matchTypeOpt.get(), showOpt.get(), request.stipulation());

      // Create response map (Map.of() has a 10 key-value pair limit)
      Map<String, Object> response = new HashMap<>();
      response.put("matchResult", result);
      response.put("winner", result.getWinner().getName());
      response.put(
          "winningTeam", result.getWinner().getName()); // Primary wrestler represents the team
      response.put("participants", result.getWrestlers().stream().map(Wrestler::getName).toList());
      response.put("team1", team1.getMemberNames());
      response.put("team2", team2.getMemberNames());
      response.put("team1Size", team1.getSize());
      response.put("team2Size", team2.getSize());
      response.put("matchType", getMatchTypeDescription(team1.getSize(), team2.getSize()));
      response.put("duration", result.getDurationMinutes());
      response.put("rating", result.getMatchRating());

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Error resolving team match", e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  /** Get match results for a specific show. */
  @GetMapping("/show/{showId}")
  public ResponseEntity<?> getMatchesByShow(@PathVariable Long showId) {
    try {
      Optional<Show> showOpt = showRepository.findById(showId);
      if (showOpt.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Show not found"));
      }

      List<Match> results = matchRepository.findByShow(showOpt.get());
      return ResponseEntity.ok(
          Map.of(
              "show", showOpt.get().getName(), "matchCount", results.size(), "matches", results));

    } catch (Exception e) {
      log.error("Error getting match results for show", e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  /** Get match history for a specific wrestler. */
  @GetMapping("/wrestler/{wrestlerId}/history")
  public ResponseEntity<?> getWrestlerMatchHistory(
      @PathVariable Long wrestlerId, @RequestParam(defaultValue = "10") int limit) {
    try {
      Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);
      if (wrestlerOpt.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Wrestler not found"));
      }

      List<Match> matches =
          matchRepository.findByWrestlerParticipation(wrestlerOpt.get()).stream()
              .limit(limit)
              .toList();

      long totalMatches = matchRepository.countMatchesByWrestler(wrestlerOpt.get());
      long wins = matchRepository.countWinsByWrestler(wrestlerOpt.get());

      return ResponseEntity.ok(
          Map.of(
              "wrestler",
              wrestlerOpt.get().getName(),
              "totalMatches",
              totalMatches,
              "wins",
              wins,
              "losses",
              totalMatches - wins,
              "winPercentage",
              totalMatches > 0 ? (double) wins / totalMatches * 100 : 0,
              "recentMatches",
              matches));

    } catch (Exception e) {
      log.error("Error getting wrestler match history", e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  /** Get all NPC-generated matches. */
  @GetMapping("/npc-generated")
  public ResponseEntity<?> getNpcGeneratedMatches() {
    try {
      List<Match> matches = matchRepository.findByIsNpcGeneratedTrue();
      return ResponseEntity.ok(Map.of("npcGeneratedMatches", matches, "count", matches.size()));

    } catch (Exception e) {
      log.error("Error getting NPC-generated matches", e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  /** Helper method to describe match type based on team sizes. */
  private String getMatchTypeDescription(int team1Size, int team2Size) {
    if (team1Size == 1 && team2Size == 1) {
      return "Singles Match";
    } else if (team1Size == 2 && team2Size == 2) {
      return "Tag Team Match";
    } else if (team1Size == 1 && team2Size > 1) {
      return "Handicap Match (1v" + team2Size + ")";
    } else if (team1Size > 1 && team2Size == 1) {
      return "Handicap Match (" + team1Size + "v1)";
    } else if (team1Size > 2 || team2Size > 2) {
      return "Multi-Team Match (" + team1Size + "v" + team2Size + ")";
    } else {
      return "Team Match (" + team1Size + "v" + team2Size + ")";
    }
  }

  /** Request DTOs */
  public record TeamMatchRequest(
      List<Long> team1WrestlerIds,
      String team1Name,
      List<Long> team2WrestlerIds,
      String team2Name,
      Long matchTypeId,
      Long showId,
      String stipulation) {}
}
