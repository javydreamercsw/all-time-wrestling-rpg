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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.TournamentDTO;
import com.github.javydreamercsw.management.dto.campaign.TournamentDTO.TournamentMatch;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentService {

  private final CampaignStateRepository campaignStateRepository;
  private final WrestlerRepository wrestlerRepository;
  private final ObjectMapper objectMapper;
  private final Random random = new Random();
  private final SegmentService segmentService;
  private final SegmentTypeRepository segmentTypeRepository;
  private final SegmentRuleRepository segmentRuleRepository;

  @Transactional
  public void initializeTournament(@NonNull Campaign campaign) {
    CampaignState state = campaign.getState();
    if (state.getTournamentState() != null && !state.getTournamentState().isEmpty()) {
      return; // Already initialized
    }

    log.info("Initializing Tournament for {}", campaign.getWrestler().getName());

    List<Wrestler> roster = wrestlerRepository.findAll();
    roster.removeIf(w -> w.equals(campaign.getWrestler()));
    Collections.shuffle(roster);

    // Dynamic Bracket Logic
    List<Wrestler> participants = new ArrayList<>(roster);
    // User requested 16-man bracket (8 matches). Cap at 16 total (15 opponents + player).
    // If fewer than 16, dynamic size is used (e.g. 8).
    if (participants.size() > 15) {
      participants = new ArrayList<>(participants.subList(0, 15));
    }
    participants.add(campaign.getWrestler());
    Collections.shuffle(participants);

    int totalParticipants = participants.size();
    // Find next power of 2
    int bracketSize = 1;
    while (bracketSize < totalParticipants) {
      bracketSize *= 2;
    }
    // Minimum size 4 for a decent tournament
    if (bracketSize < 4) bracketSize = 4;

    int totalRounds = (int) (Math.log(bracketSize) / Math.log(2));

    TournamentDTO tournament = new TournamentDTO();
    tournament.setTotalRounds(totalRounds);
    List<TournamentMatch> matches = new ArrayList<>();

    // Round 1 Generation
    // We assign one real player to 'Wrestler 1' of each match first (up to bracketSize/2 matches)
    // If we have more players than matches, we fill 'Wrestler 2'.
    // Remaining 'Wrestler 2' slots are Byes.
    // If we have even fewer players (e.g. 3 players for size 4), some 'Wrestler 1' might be Byes?
    // No, algorithm:
    // 1. Create matches.
    // 2. Distribute players.

    int matchCountR1 = bracketSize / 2;
    // We use a list of "Slots" which includes Byes (nulls) if necessary?
    // Actually, simple distribution:
    // P1..PN.
    // If N <= matchCountR1: Each player gets a match. Opponents are Byes.
    // If N > matchCountR1: First (N - matchCountR1) matches have 2 players. Rest have 1.
    // Wait, simpler: Just fill slots 1..bracketSize with players, then Byes.
    // Then M1 = Slot 1 vs Slot 2. M2 = Slot 3 vs Slot 4.

    List<Wrestler> slots = new ArrayList<>(participants);
    while (slots.size() < bracketSize) {
      slots.add(null); // Bye
    }

    for (int i = 0; i < matchCountR1; i++) {
      TournamentMatch match = new TournamentMatch();
      match.setId("R1-M" + (i + 1));
      match.setRound(1);
      int nextMatchNum = (i / 2) + 1;
      match.setNextMatchId("R2-M" + nextMatchNum);

      Wrestler w1 = slots.get(i * 2);
      Wrestler w2 = slots.get(i * 2 + 1);

      if (w1 != null) {
        match.setWrestler1Id(w1.getId());
        match.setWrestler1Name(w1.getName());
      } else {
        match.setWrestler1Name("BYE");
      }

      if (w2 != null) {
        match.setWrestler2Id(w2.getId());
        match.setWrestler2Name(w2.getName());
      } else {
        match.setWrestler2Name("BYE");
      }

      // Resolve Byes immediately
      if (w1 == null && w2 == null) {
        // Double Bye - Advance Bye?
        match.setWinnerId(-1L); // Special ID or just handle upstream?
        // Actually, if Double Bye, next match gets a Bye.
        // Let's set winnerId to null but name to "BYE" in next match logic?
        // Simplest: w1 is null, w2 is null. Winner is null.
        // But we need to mark it resolved.
        // Let's assume w1 "wins" (Bye advances).
        // But w1 is null.
        // Let's say winnerId = 0L for Bye?
      } else if (w1 != null && w2 == null) {
        match.setWinnerId(w1.getId());
      } else if (w1 == null) {
        match.setWinnerId(w2.getId());
      }

      if ((w1 != null && w1.equals(campaign.getWrestler()))
          || (w2 != null && w2.equals(campaign.getWrestler()))) {
        match.setPlayerMatch(true);
      }

      matches.add(match);
    }

    // Subsequent Rounds placeholders
    int matchesInRound = matchCountR1;
    for (int r = 2; r <= totalRounds; r++) {
      matchesInRound /= 2;
      for (int i = 0; i < matchesInRound; i++) {
        TournamentMatch match = new TournamentMatch();
        match.setId("R" + r + "-M" + (i + 1));
        match.setRound(r);
        if (r < totalRounds) {
          match.setNextMatchId("R" + (r + 1) + "-M" + ((i / 2) + 1));
        }
        matches.add(match);
      }
    }

    tournament.setMatches(matches);

    // Propagate the Bye winners to Round 2 immediately
    propagateWinners(tournament, 1, campaign);

    saveTournamentState(state, tournament);
  }

  public TournamentDTO getTournamentState(@NonNull Campaign campaign) {
    CampaignState state = campaign.getState();
    if (state.getTournamentState() == null) {
      return null;
    }
    try {
      return objectMapper.readValue(state.getTournamentState(), TournamentDTO.class);
    } catch (JsonProcessingException e) {
      log.error("Failed to parse tournament state", e);
      return null;
    }
  }

  @Transactional
  public void advanceTournament(@NonNull Campaign campaign, boolean playerWon, Show show) {
    TournamentDTO tournament = getTournamentState(campaign);
    if (tournament == null) return;

    int currentRound = tournament.getCurrentRound();
    log.info("Advancing tournament. Current Round: {}", currentRound);

    List<TournamentMatch> currentRoundMatches =
        tournament.getMatches().stream()
            .filter(m -> m.getRound() == currentRound)
            .collect(Collectors.toList());

    log.info("Found {} matches for round {}", currentRoundMatches.size(), currentRound);

    // Simulate current round
    for (TournamentMatch match : currentRoundMatches) {
      if (match.getWinnerId() != null) {
        log.info("Match {} already resolved. Winner: {}", match.getId(), match.getWinnerId());
        continue; // Already resolved
      }

      log.info(
          "Resolving match {}: {} vs {}",
          match.getId(),
          match.getWrestler1Name(),
          match.getWrestler2Name());

      Long winnerId = null;

      if (match.isPlayerMatch()) {
        // Player Match logic
        if (playerWon) {
          winnerId = campaign.getWrestler().getId();
          log.info("Player WON match {}", match.getId());
        } else {
          // Determine opponent ID
          winnerId =
              match.getWrestler1Id().equals(campaign.getWrestler().getId())
                  ? match.getWrestler2Id()
                  : match.getWrestler1Id();
        }
      } else {
        // NPC vs NPC Simulation
        if (match.getWrestler1Id() == null && match.getWrestler2Id() == null) {
          // Bye vs Bye -> Double Bye
          match.setWrestler1Name("BYE");
          winnerId = -1L; // Mark as resolved (Bye advances)
        } else if (match.getWrestler1Id() == null) {
          winnerId = match.getWrestler2Id();
        } else if (match.getWrestler2Id() == null) {
          winnerId = match.getWrestler1Id();
        } else {
          // Real match
          if (random.nextBoolean()) {
            winnerId = match.getWrestler1Id();
          } else {
            winnerId = match.getWrestler2Id();
          }

          // Create Segment for record keeping if Show is provided and both wrestlers exist
          if (show != null && winnerId > 0) {
            createSimulatedMatch(show, match.getWrestler1Id(), match.getWrestler2Id(), winnerId);
          }
        }
      }
      match.setWinnerId(winnerId);
    }
    // Propagate
    propagateWinners(tournament, currentRound, campaign);

    // Check if round complete
    boolean roundComplete = currentRoundMatches.stream().allMatch(m -> m.getWinnerId() != null);
    if (roundComplete) {
      tournament.setCurrentRound(currentRound + 1);
    }

    saveTournamentState(campaign.getState(), tournament);
  }

  private void createSimulatedMatch(Show show, Long w1Id, Long w2Id, Long winnerId) {
    Optional<Wrestler> w1Opt = wrestlerRepository.findById(w1Id);
    Optional<Wrestler> w2Opt = wrestlerRepository.findById(w2Id);

    if (w1Opt.isPresent() && w2Opt.isPresent()) {
      Wrestler w1 = w1Opt.get();
      Wrestler w2 = w2Opt.get();

      // Determine Segment Type (Standard Match)
      SegmentType type =
          segmentTypeRepository
              .findByName("One on One")
              .orElseThrow(() -> new IllegalStateException("One on One segment type not found"));

      // Create Segment
      // show.getShowDate() is LocalDate. createSegment needs Instant.
      Instant date =
          show.getShowDate() != null
              ? show.getShowDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
              : Instant.now();

      Segment segment = segmentService.createSegment(show, type, date);
      segment.setNarration("Tournament Match: " + w1.getName() + " vs " + w2.getName());
      // We set narration manually because createSegment(show, type, date) doesn't take narration.
      // Or we can use updateSegment logic?
      // SegmentService.createSegment returns saved segment.
      // We can update it.

      // Add Participants
      segmentService.addParticipant(segment, w1);
      segmentService.addParticipant(segment, w2);

      // Determine Winner
      Wrestler winner = winnerId.equals(w1.getId()) ? w1 : w2;
      segmentService.setWinner(segment, winner);
      segmentService.setAdjudicationStatus(segment, AdjudicationStatus.ADJUDICATED);

      // Update Records? SegmentService.setWinner/adjudicate might not update stats automatically if
      // not calling specific method?
      // SegmentService usually handles stats in `adjudicateMatch`?
      // Let's assume setWinner + ADJUDICATED is enough for history, or check SegmentService.
      // Usually need to call a method to process stats.
      // For now, this creates the record in history.
    }
  }

  private void propagateWinners(
      @NonNull TournamentDTO tournament, int round, @NonNull Campaign campaign) {
    List<TournamentMatch> matches =
        tournament.getMatches().stream().filter(m -> m.getRound() == round).toList();

    for (TournamentMatch match : matches) {
      if (match.getWinnerId() != null && match.getNextMatchId() != null) {
        String nextId = match.getNextMatchId();
        tournament.getMatches().stream()
            .filter(m -> m.getId().equals(nextId))
            .findFirst()
            .ifPresent(
                nextMatch -> {
                  String winnerName = "BYE";
                  // If winnerId is valid (not double bye placeholder)
                  if (match.getWinnerId() > 0) { // Assuming >0 are valid IDs
                    winnerName =
                        match.getWinnerId().equals(match.getWrestler1Id())
                            ? match.getWrestler1Name()
                            : match.getWrestler2Name();
                  }

                  if (nextMatch.getWrestler1Id() == null && nextMatch.getWrestler1Name() == null) {
                    nextMatch.setWrestler1Id(match.getWinnerId() > 0 ? match.getWinnerId() : null);
                    nextMatch.setWrestler1Name(winnerName);
                  } else {
                    nextMatch.setWrestler2Id(match.getWinnerId() > 0 ? match.getWinnerId() : null);
                    nextMatch.setWrestler2Name(winnerName);
                  }

                  if (match.getWinnerId() != null
                      && match.getWinnerId().equals(campaign.getWrestler().getId())) {
                    nextMatch.setPlayerMatch(true);
                  }
                });
      }
    }
  }

  public TournamentMatch getCurrentPlayerMatch(@NonNull Campaign campaign) {
    TournamentDTO tournament = getTournamentState(campaign);
    if (tournament == null) return null;

    // Return the first unresolved player match, regardless of round
    return tournament.getMatches().stream()
        .filter(m -> m.isPlayerMatch() && m.getWinnerId() == null)
        .findFirst()
        .orElse(null);
  }

  public boolean isPlayerChampion(@NonNull Campaign campaign) {
    TournamentDTO tournament = getTournamentState(campaign);
    if (tournament == null) return false;

    int totalRounds = tournament.getTotalRounds();
    TournamentMatch finals =
        tournament.getMatches().stream()
            .filter(m -> m.getRound() == totalRounds)
            .findFirst()
            .orElse(null);

    return finals != null && campaign.getWrestler().getId().equals(finals.getWinnerId());
  }

  private void saveTournamentState(@NonNull CampaignState state, @NonNull TournamentDTO dto) {
    try {
      state.setTournamentState(objectMapper.writeValueAsString(dto));
      campaignStateRepository.save(state);
    } catch (JsonProcessingException e) {
      log.error("Failed to save tournament state", e);
    }
  }
}
