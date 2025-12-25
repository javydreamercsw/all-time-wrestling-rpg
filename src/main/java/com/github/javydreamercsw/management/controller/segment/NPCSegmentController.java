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
package com.github.javydreamercsw.management.controller.segment;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService;
import com.github.javydreamercsw.management.service.segment.SegmentTeam;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for NPC segment resolution system. Provides endpoints for automatically resolving
 * segments between wrestlers using ATW RPG mechanics.
 */
@RestController
@RequestMapping("/api/npc-segments")
@RequiredArgsConstructor
@Slf4j
public class NPCSegmentController {
  private final NPCSegmentResolutionService npcSegmentResolutionService;
  private final WrestlerRepository wrestlerRepository;
  private final SegmentTypeRepository segmentTypeRepository;
  private final ShowRepository showRepository;
  private final SegmentRepository segmentRepository;

  /** Resolve a team-based segment (supports singles, tag team, handicap, etc.). */
  @PostMapping("/team")
  public ResponseEntity<?> resolveTeamSegment(@RequestBody TeamSegmentRequest request) {
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

      // Validate segment type
      Optional<SegmentType> segmentTypeOpt =
          segmentTypeRepository.findById(request.segmentTypeId());
      if (segmentTypeOpt.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Segment type not found"));
      }

      // Validate show
      Optional<Show> showOpt = showRepository.findById(request.showId());
      if (showOpt.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Show not found"));
      }

      // Create teams
      SegmentTeam team1 = new SegmentTeam(team1Wrestlers, request.team1Name());
      SegmentTeam team2 = new SegmentTeam(team2Wrestlers, request.team2Name());

      // Resolve segment
      Segment result =
          npcSegmentResolutionService.resolveTeamSegment(
              team1, team2, segmentTypeOpt.get(), showOpt.get(), request.stipulation());

      // Create response map (Map.of() has a 10 key-value pair limit)
      Map<String, Object> response = new HashMap<>();
      response.put("segmentResult", result);
      List<Wrestler> winners = result.getWinners();
      List<String> winnerNames = winners.stream().map(Wrestler::getName).toList();

      // Determine winning team
      String winningTeamName = "";
      if (!winners.isEmpty()) {
        if (team1.getMembers().contains(winners.get(0))) {
          winningTeamName = team1.getTeamName();
        } else {
          winningTeamName = team2.getTeamName();
        }
      }
      response.put("winners", winnerNames);
      response.put("winningTeam", winningTeamName);
      response.put("participants", result.getWrestlers().stream().map(Wrestler::getName).toList());
      response.put("team1", team1.getMemberNames());
      response.put("team2", team2.getMemberNames());
      response.put("team1Size", team1.getSize());
      response.put("team2Size", team2.getSize());
      response.put("segmentType", getSegmentTypeDescription(team1.getSize(), team2.getSize()));

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Error resolving team segment", e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  /** Get segment results for a specific show. */
  @GetMapping("/show/{showId}")
  public ResponseEntity<?> getSegmentsByShow(@PathVariable Long showId) {
    try {
      Optional<Show> showOpt = showRepository.findById(showId);
      if (showOpt.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Show not found"));
      }

      List<Segment> results = segmentRepository.findByShow(showOpt.get());
      return ResponseEntity.ok(
          Map.of(
              "show",
              showOpt.get().getName(),
              "segmentCount",
              results.size(),
              "segments",
              results));

    } catch (Exception e) {
      log.error("Error getting segment results for show", e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  /** Get segment history for a specific wrestler. */
  @GetMapping("/wrestler/{wrestlerId}/history")
  public ResponseEntity<?> getWrestlerSegmentHistory(
      @PathVariable Long wrestlerId, @RequestParam(defaultValue = "10") int limit) {
    try {
      Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);
      if (wrestlerOpt.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Wrestler not found"));
      }

      List<Segment> segments =
          segmentRepository
              .findByWrestlerParticipation(wrestlerOpt.get(), PageRequest.of(0, limit))
              .stream()
              .toList();

      long totalSegments = segmentRepository.countSegmentsByWrestler(wrestlerOpt.get());
      long wins = segmentRepository.countWinsByWrestler(wrestlerOpt.get());

      return ResponseEntity.ok(
          Map.of(
              "wrestler",
              wrestlerOpt.get().getName(),
              "totalSegments",
              totalSegments,
              "wins",
              wins,
              "losses",
              totalSegments - wins,
              "winPercentage",
              totalSegments > 0 ? (double) wins / totalSegments * 100 : 0,
              "recentSegments",
              segments));

    } catch (Exception e) {
      log.error("Error getting wrestler segment history", e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  /** Get all NPC-generated segments. */
  @GetMapping("/npc-generated")
  public ResponseEntity<?> getNpcGeneratedSegments() {
    try {
      List<Segment> segments = segmentRepository.findByIsNpcGeneratedTrue();
      return ResponseEntity.ok(Map.of("npcGeneratedSegments", segments, "count", segments.size()));

    } catch (Exception e) {
      log.error("Error getting NPC-generated segments", e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  /** Helper method to describe segment type based on team sizes. */
  private String getSegmentTypeDescription(int team1Size, int team2Size) {
    if (team1Size == 1 && team2Size == 1) {
      return "Singles Segment";
    } else if (team1Size == 2 && team2Size == 2) {
      return "Tag Team Segment";
    } else if (team1Size == 1 && team2Size > 1) {
      return "Handicap Segment (1v" + team2Size + ")";
    } else if (team1Size > 1 && team2Size == 1) {
      return "Handicap Segment (" + team1Size + "v1)";
    } else if (team1Size > 2 || team2Size > 2) {
      return "Multi-Team Segment (" + team1Size + "v" + team2Size + ")";
    } else {
      return "Team Segment (" + team1Size + "v" + team2Size + ")";
    }
  }

  /** Request DTOs */
  public record TeamSegmentRequest(
      List<Long> team1WrestlerIds,
      String team1Name,
      List<Long> team2WrestlerIds,
      String team2Name,
      Long segmentTypeId,
      Long showId,
      String stipulation) {}
}
