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
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.TournamentDTO;
import com.github.javydreamercsw.management.dto.campaign.TournamentDTO.TournamentMatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
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

  @Transactional
  public void initializeTournament(Campaign campaign) {
    CampaignState state = campaign.getState();
    if (state.getTournamentState() != null && !state.getTournamentState().isEmpty()) {
      return; // Already initialized
    }

    log.info("Initializing Tournament for {}", campaign.getWrestler().getName());

    List<Wrestler> roster = wrestlerRepository.findAll();
    roster.removeIf(w -> w.equals(campaign.getWrestler()));
    Collections.shuffle(roster);

    // Need 15 opponents
    if (roster.size() < 15) {
      throw new IllegalStateException("Not enough wrestlers to populate 16-man tournament");
    }
    List<Wrestler> participants = new ArrayList<>(roster.subList(0, 15));
    participants.add(campaign.getWrestler()); // Add player
    Collections.shuffle(participants); // Shuffle placement

    TournamentDTO tournament = new TournamentDTO();
    List<TournamentMatch> matches = new ArrayList<>();

    // Round 1 (16 people -> 8 matches)
    // IDs: R1-M1 to R1-M8
    for (int i = 0; i < 8; i++) {
      TournamentMatch match = new TournamentMatch();
      match.setId("R1-M" + (i + 1));
      match.setRound(1);
      match.setNextMatchId("R2-M" + (i / 2 + 1)); // M1,M2 -> R2-M1; M3,M4 -> R2-M2

      Wrestler w1 = participants.get(i * 2);
      Wrestler w2 = participants.get(i * 2 + 1);

      match.setWrestler1Id(w1.getId());
      match.setWrestler1Name(w1.getName());
      match.setWrestler2Id(w2.getId());
      match.setWrestler2Name(w2.getName());

      if (w1.equals(campaign.getWrestler()) || w2.equals(campaign.getWrestler())) {
        match.setPlayerMatch(true);
      }

      matches.add(match);
    }

    // Initialize placeholders for R2, R3, R4
    // R2 (8 people -> 4 matches)
    for (int i = 0; i < 4; i++) {
      TournamentMatch match = new TournamentMatch();
      match.setId("R2-M" + (i + 1));
      match.setRound(2);
      match.setNextMatchId("R3-M" + (i / 2 + 1));
      matches.add(match);
    }
    // R3 (4 people -> 2 matches)
    for (int i = 0; i < 2; i++) {
      TournamentMatch match = new TournamentMatch();
      match.setId("R3-M" + (i + 1));
      match.setRound(3);
      match.setNextMatchId("R4-M1");
      matches.add(match);
    }
    // R4 (Finals -> 1 match)
    TournamentMatch finals = new TournamentMatch();
    finals.setId("R4-M1");
    finals.setRound(4);
    matches.add(finals);

    tournament.setMatches(matches);
    saveTournamentState(state, tournament);
  }

  public TournamentDTO getTournamentState(Campaign campaign) {
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
  public void advanceTournament(Campaign campaign, boolean playerWon) {
    TournamentDTO tournament = getTournamentState(campaign);
    if (tournament == null) return;

    int currentRound = tournament.getCurrentRound();
    List<TournamentMatch> currentRoundMatches =
        tournament.getMatches().stream()
            .filter(m -> m.getRound() == currentRound)
            .collect(Collectors.toList());

    boolean playerAlive = false;

    // Simulate current round
    for (TournamentMatch match : currentRoundMatches) {
      if (match.getWinnerId() != null) continue; // Already resolved

      if (match.isPlayerMatch()) {
        // Player Match logic
        if (playerWon) {
          match.setWinnerId(campaign.getWrestler().getId());
          playerAlive = true;
        } else {
          // Determine opponent ID
          Long opponentId =
              match.getWrestler1Id().equals(campaign.getWrestler().getId())
                  ? match.getWrestler2Id()
                  : match.getWrestler1Id();
          match.setWinnerId(opponentId);
        }
      } else {
        // NPC vs NPC Simulation (Random for now, could be Tier based)
        // 50/50 chance
        if (random.nextBoolean()) {
          match.setWinnerId(match.getWrestler1Id());
        } else {
          match.setWinnerId(match.getWrestler2Id());
        }
      }

      // Propagate winner to next match
      if (match.getNextMatchId() != null) {
        String nextId = match.getNextMatchId();
        tournament.getMatches().stream()
            .filter(m -> m.getId().equals(nextId))
            .findFirst()
            .ifPresent(
                nextMatch -> {
                  String winnerName =
                      match.getWinnerId().equals(match.getWrestler1Id())
                          ? match.getWrestler1Name()
                          : match.getWrestler2Name();

                  if (nextMatch.getWrestler1Id() == null) {
                    nextMatch.setWrestler1Id(match.getWinnerId());
                    nextMatch.setWrestler1Name(winnerName);
                  } else {
                    nextMatch.setWrestler2Id(match.getWinnerId());
                    nextMatch.setWrestler2Name(winnerName);
                  }

                  if (match.getWinnerId().equals(campaign.getWrestler().getId())) {
                    nextMatch.setPlayerMatch(true);
                  }
                });
      }
    }

    // Check if round complete
    boolean roundComplete = currentRoundMatches.stream().allMatch(m -> m.getWinnerId() != null);
    if (roundComplete) {
      tournament.setCurrentRound(currentRound + 1);
    }

    saveTournamentState(campaign.getState(), tournament);
  }

  public TournamentMatch getCurrentPlayerMatch(Campaign campaign) {
    TournamentDTO tournament = getTournamentState(campaign);
    if (tournament == null) return null;

    return tournament.getMatches().stream()
        .filter(m -> m.getRound() == tournament.getCurrentRound() && m.isPlayerMatch())
        .findFirst()
        .orElse(null);
  }

  private void saveTournamentState(CampaignState state, TournamentDTO dto) {
    try {
      state.setTournamentState(objectMapper.writeValueAsString(dto));
      campaignStateRepository.save(state);
    } catch (JsonProcessingException e) {
      log.error("Failed to save tournament state", e);
    }
  }
}
